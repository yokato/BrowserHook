package jp.rgfx_currentdir_ozero.browserhook;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.content.SharedPreferences;

import jp.rgfx_currentdir_ozero.browserhook.Converter;

public class BrowserhookActivity extends Activity implements OnClickListener  {
	
	//constants
	public static Uri URI = null;
	public static Boolean IS_STANDALONE = true;
	public static String TAG = "bh";
	public static String PREFS_NAME = "MyPrefsFile";
	public static String sPrefKeyDisableHistory = "DisableHistory";
	public static Integer ACTIVITY_EDIT = 1;
	
	//member
	private ArrayList<HashMap<String, String>> mSuitableApps = new ArrayList<HashMap<String, String>>();
	private ArrayList<HashMap<String, String>> mConverters = new ArrayList<HashMap<String, String>>();
	private SharedPreferences mSharedPrefs;
	private String mLastBrowser;
	
	//gui
	private Spinner mWdgSpinnerBrowsers = null;
	private Spinner mWdgSpinnerConverters = null;
	private Button mWdgDirectBtn;
	private Button mWdgConvertBtn;
	private Button mWdgHistoryBtn;
	private Button mWdgSettingBtn;
	private final int mHistoryId = R.id.menu_history;
	private final int mSettingId = R.id.menu_setting;
	
	//instances
	// private Spinner wdgSpinnerConverters = (Spinner)
	// findViewById(R.id.SpinnerConverters);
	private Converter mDbHelperConverter;
	private History mDBHelperHistory;
	
	// //////////////////////////////////////////////////////////////////////
	// GUI event dispatcher: activity
		
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		// start
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browserhook);
		mDbHelperConverter = new Converter(this);
		mDbHelperConverter.open();
		mDBHelperHistory = new History(this);
		mDBHelperHistory.open();
		mSharedPrefs = getSharedPreferences(PREFS_NAME, 0);
		
		//bind
		mWdgDirectBtn = (Button) findViewById(R.id.ButtonDirect);
		mWdgDirectBtn.setOnClickListener(this);
		mWdgConvertBtn = (Button) findViewById(R.id.ButtonConvert);
		mWdgConvertBtn.setOnClickListener(this);
		mWdgHistoryBtn = (Button) findViewById(R.id.ButtonHistory);
		mWdgHistoryBtn.setOnClickListener(this);
		mWdgSettingBtn = (Button) findViewById(R.id.ButtonSetting);
		mWdgSettingBtn.setOnClickListener(this);

		// インテントが渡されたか単体起動かを判別
		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			URI = getIntent().getData();
			setTitle(getText(R.string.apptitle_main).toString() + ": "
					+ URI.toString());// タイトルを設定
			IS_STANDALONE = false;
			//Log.d(TAG, "oc:i:got");

			//履歴が許可されているなら記録
			final Boolean historyAvailable = mSharedPrefs.getBoolean(
					sPrefKeyDisableHistory, false);  
			if(!historyAvailable){
				Date currentTime_1 = new Date();
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				String dateString = formatter.format(currentTime_1);
				//Log.d(TAG, "sba:cv:" + dateString);
				mDBHelperHistory.createItem(URI.toString(),dateString);
			}
			
			//load shared prefs
      mLastBrowser = mSharedPrefs.getString("lastBrowser", "");
	        
			//build spinner
			buildBrowserSpinner();
			buildConvertSpinner();
			
		} else {
			IS_STANDALONE = true;
			startConverterlistActivity();
			finish();
		}
		return;
	}

	// 編集画面から戻ってきた際に実行される。
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");
		//build spinner
		buildBrowserSpinner();
		buildConvertSpinner();
	}
	
	//プロセスが破棄される際に。
	protected void onDestroy(int requestCode, int resultCode,
			Intent intent) {
		super.onDestroy();
		mDbHelperConverter.close();
		mDBHelperHistory.close();
	}
	
	
	
	// //////////////////////////////////////////////////////////////////////
	// GUI defs
	
	// メニューを作成
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// メニューインフレーターを取得
		MenuInflater inflater = getMenuInflater();
		// xmlのリソースファイルを使用してメニューにアイテムを追加
		inflater.inflate(R.menu.browserhook, menu);
		// できたらtrueを返す
		return true;
	}

	


	// //////////////////////////////////////////////////////////////////////
	// GUI event dispatcher: widget
		
	// メニューがクリックされた際
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case mHistoryId:
			//Log.d(TAG, "menu:history");
			startHistoryActivity();
			return true;
		case mSettingId:
			//Log.d(TAG, "menu:setting");
			startConverterlistActivity();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}
	
	// ボタンクリックのディスパッチ
	public void onClick(View v) {

		if (v == mWdgDirectBtn) {
			//Log.d(TAG, "click:direct");
			//
			mWdgSpinnerBrowsers = (Spinner) findViewById(R.id.SpinnerBrowsers);
			long itemid = mWdgSpinnerBrowsers.getSelectedItemId();
			String[] app = {
				mSuitableApps.get((int)itemid).get("packageName"),
				mSuitableApps.get((int)itemid).get("activityInfo")
			};
			startBrowserApp("",app[0], app[1]);
			
		} else if (v == mWdgConvertBtn) {
			//Log.d(TAG, "click:convert");
			//
			mWdgSpinnerBrowsers = (Spinner) findViewById(R.id.SpinnerBrowsers);
			long itemid = mWdgSpinnerBrowsers.getSelectedItemId();
			String[] app = {
				mSuitableApps.get((int)itemid).get("packageName"),
				mSuitableApps.get((int)itemid).get("activityInfo")
			};
			//
			mWdgSpinnerConverters = (Spinner) findViewById(R.id.SpinnerConverters);
			long cnvkey = mWdgSpinnerConverters.getSelectedItemId();
			String cnv = 
				mConverters.get((int)cnvkey).get("url")
			;
			Log.d(TAG,"onClick:wdgSpnCnv.getSelectedItemId-key/url:"+cnvkey+" / "+cnv);
			startBrowserApp(cnv,app[0], app[1]);
			
		} else if (v == mWdgSettingBtn) {
			startConverterlistActivity();
			
		} else if (v == mWdgHistoryBtn) {
			startHistoryActivity();
			
		}
		
		
		return;
	}
	
	// //////////////////////////////////////////////////////////////////////
	// GUI transition

	// 設定画面を開く
	private void startConverterlistActivity() {
		//Log.d(TAG, "scla:openSetting:");
		Intent i = new Intent(this,ConverterlistActivity.class);
		// クリックされた行のIDをintentに埋める。これで項目ID取れるのなー
		startActivityForResult(i, ACTIVITY_EDIT);
		//Log.d(TAG, "scla:launch setting activity.");
		return;
	}

	// 履歴画面を開く
	private void startHistoryActivity() {
		Intent i = new Intent(this,HistoryActivity.class);
		startActivity(i);
		return;
	}

	// //////////////////////////////////////////////////////////////////////
	// misc logic

	// intent投げ先activitiesをspinnerに積む処理
	private void buildBrowserSpinner() {

		// アダプターを設定します
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mWdgSpinnerBrowsers = (Spinner) findViewById(R.id.SpinnerBrowsers);
		mWdgSpinnerBrowsers.setAdapter(adapter);
		// アイテムを追加します
		mSuitableApps = getSuitableActivities();
		for (int i = 0; i < mSuitableApps.size();i++) {
			adapter.add(mSuitableApps.get(i).get("label"));
		}
		// スピナーのアイテムが選択された時に呼び出されるコールバックを登録します
		mWdgSpinnerBrowsers
			.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent,
						View view, int position, long id) {
					//Spinner spinner = (Spinner) parent;
					// 選択されたアイテムを取得します
					//String item = (String) spinner.getSelectedItem();
					//Log.d(TAG, "spb:sel:" + item);
				}

				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		
		//スピナー表示時タイトル
		mWdgSpinnerBrowsers.setPrompt(
				(CharSequence)getString(R.string.spinnerPrompt_browser)
				);
		
		return;
	}

	// intentに適したactivityを一覧で引いてくる処理。
	private ArrayList<HashMap<String, String>> getSuitableActivities() {
		ArrayList<HashMap<String, String>> apps = new ArrayList<HashMap<String, String>>();
		
		//
		List<ResolveInfo> list = new ArrayList<ResolveInfo>();
		ResolveInfo info;
		HashMap<String, String> appinfo;
		//
		final PackageManager pm = this.getPackageManager();
		final Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		intent.setData(Uri.parse("http://cnn.com/"));
		List<ResolveInfo> list_src = pm.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);

		if (list_src == null) {
			return apps;
		}
		
		//いったんソートする
		Collections.sort(list_src, new ResolveInfo.DisplayNameComparator(pm));
		
		//listに対し、lastBrowserに等しいpackageNameを持っている要素を先頭に持ってくる
		for (int i = 0; i < list_src.size(); i++) {
			info = list_src.get(i);
			if(mLastBrowser.equals((String) info.activityInfo.applicationInfo.packageName)){
				list.add(list_src.get(i));
			}
		}
		//それ以外を後から追加する
		for (int i = 0; i < list_src.size(); i++) {
			info = list_src.get(i);
			if(!mLastBrowser.equals((String) info.activityInfo.applicationInfo.packageName)){
				list.add(list_src.get(i));
			}
		}

		// アクティビティ情報の取得
		for (int i = 0; i < list.size(); i++) {
			appinfo = new HashMap<String, String>();
			info = list.get(i);
			//
			appinfo.put("label", (String) info.loadLabel(pm));
			//Log.d(TAG, "gsa:label:" + (String) info.loadLabel(pm));
			//
			appinfo.put("packageName",
					(String) info.activityInfo.applicationInfo.packageName);
			//Log.d(TAG, "gsa:pName:"
			//		+ (String) info.activityInfo.applicationInfo.packageName);
			//
			appinfo.put("activityInfo", (String) info.activityInfo.name);
			//Log.d(TAG, "gsa:aName:" + (String) info.activityInfo.name);
			//
			apps.add(appinfo);
		}
		
		return apps;

	}

	// 変換方法一覧spinnerに変換候補を流し込む
	private void buildConvertSpinner() {
		//Log.d(TAG, "init:dialog");

		// アダプターを設定します
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mWdgSpinnerConverters = (Spinner) findViewById(R.id.SpinnerConverters);
		mWdgSpinnerConverters.setAdapter(adapter);
		// アイテムを追加します
		mConverters = new ArrayList<HashMap<String, String>>();
		Cursor c = mDbHelperConverter.fetchAllItems();
		startManagingCursor(c);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++) {
			//選択項目に追加
			adapter.add(c.getString(1));
			//変換項目キャッシュに追加
			HashMap<String,String> hm = new HashMap<String,String>();
			hm.put("title",c.getString(1));
			hm.put("url",c.getString(2));
			mConverters.add(hm);
			Log.d(TAG, "bcs:"+i+":" + c.getString(1) + "/" +c.getString(2));
			c.moveToNext();
		}

		// スピナーのアイテムが選択された時に呼び出されるコールバックを登録します
		mWdgSpinnerConverters
			.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				public void onItemSelected(AdapterView<?> parent,
				View view, int position, long id) {
					Spinner spinner = (Spinner) parent;
					// 選択されたアイテムを取得します
					Cursor c = mDbHelperConverter.fetchItemByTitle((String) spinner
							.getSelectedItem());
					startManagingCursor(c);
					Log.d(TAG, "bcs:ois:sel:" + c.getString(2));
				}

				public void onNothingSelected(AdapterView<?> parent) {
				}
			});
		
		//スピナー表示時タイトル
		mWdgSpinnerConverters.setPrompt(
			(CharSequence)getString(R.string.spinnerPrompt_converter)
		);
		
		//Log.d(TAG, "init:dialog:show:done");
		return;
	}

	// URLを加工してブラウザを起動する。
	private void startBrowserApp(String cv,String pkg, String act) {
		Log.d(TAG, "sba:cv:" + cv+"/pkg:"+pkg+"/act:"+act);

		//save browser selection
	    SharedPreferences.Editor editor = mSharedPrefs.edit();
	    editor.putString("lastBrowser", pkg);
		//Log.d(TAG, "sba:shpref:lb:" + pkg);
	    // Don't forget to commit your edits!!!
	    editor.commit();		
		
		//mod url
		URI = Uri.parse(cv + URI.toString());
		
		//open app
		ComponentName comp = new ComponentName(pkg,act);
		Intent intent = new Intent(Intent.ACTION_VIEW, URI);
		intent.setComponent(comp);		
		startActivity(intent);
		finish();
		//Log.d(TAG, "successfully launch browser.");
		return;
	}
	
	

}