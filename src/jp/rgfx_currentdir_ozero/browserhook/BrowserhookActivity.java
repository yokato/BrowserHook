package jp.rgfx_currentdir_ozero.browserhook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
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

import jp.rgfx_currentdir_ozero.browserhook.Converter;

public class BrowserhookActivity extends Activity implements OnClickListener  {
	static Uri URI = null;
	static Boolean IS_STANDALONE = true;
	static String TAG = "bh";
	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_EDIT = 1;
	private static final int INSERT_ID = R.id.menu_insert;
	private static final int EXPORT_ID = R.id.menu_export;
	private static final int IMPORT_ID = R.id.menu_import;
	private static final int INITIALIZE_ID = R.id.menu_initialize;
	private static ArrayList<HashMap<String, String>> SUITABLEAPPS = new ArrayList<HashMap<String, String>>();
	private static ArrayList<HashMap<String, String>> CONVERTERS = new ArrayList<HashMap<String, String>>();

	//gui
	private static Spinner wdgSpinnerBrowsers = null;
	private static Spinner wdgSpinnerConverters = null;
	private Button wdgDirectBtn;
	private Button wdgConvertBtn;
	private Button wdgSettingBtn;
	
	// private Spinner wdgSpinnerConverters = (Spinner)
	// findViewById(R.id.SpinnerConverters);
	private Converter mDbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// start
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mDbHelper = new Converter(this);
		mDbHelper.open();
		//bind
		wdgDirectBtn = (Button) findViewById(R.id.ButtonDirect);
		wdgDirectBtn.setOnClickListener(this);
		wdgConvertBtn = (Button) findViewById(R.id.ButtonConvert);
		wdgConvertBtn.setOnClickListener(this);
		wdgSettingBtn = (Button) findViewById(R.id.ButtonSetting);
		wdgSettingBtn.setOnClickListener(this);

		// インテントが渡されたか単体起動かを判別
		if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
			URI = getIntent().getData();
			setTitle(getText(R.string.apptitle_main).toString() + ": "
					+ URI.toString());// タイトルを設定
			IS_STANDALONE = false;
			Log.d(TAG, "oc:i:got");
			//
			buildBrowserSpinner();
			buildConvertSpinner();
		} else {
			URI = Uri.parse("http://cnn.com");
			setTitle(R.string.apptitle_main_standalone);// タイトルを設定
			IS_STANDALONE = true;
			Log.d(TAG, "oc:i:none");
			buildBrowserSpinner();
			buildConvertSpinner();
		}
		return;
	}

	// 編集画面から戻ってきた際に実行される。
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		// buildSelectDialog();
	}


	// //////////////////////////////////////////////////////////////////////
	// GUI event dispatcher
		
	// ボタンクリックのディスパッチ
	public void onClick(View v) {
		if (v == wdgDirectBtn) {
			Log.d(TAG, "click:direct");
			//
			wdgSpinnerBrowsers = (Spinner) findViewById(R.id.SpinnerBrowsers);
			long itemid = wdgSpinnerBrowsers.getSelectedItemId();
			String[] app = {
					SUITABLEAPPS.get((int) itemid).get("packageName"),
					SUITABLEAPPS.get((int)itemid).get("activityInfo")
			};
			startBrowserApp("",app[0], app[1]);
			
		} else if (v == wdgConvertBtn) {
			Log.d(TAG, "click:convert");
			//
			wdgSpinnerBrowsers = (Spinner) findViewById(R.id.SpinnerBrowsers);
			long itemid = wdgSpinnerBrowsers.getSelectedItemId();
			String[] app = {
					SUITABLEAPPS.get((int) itemid).get("packageName"),
					SUITABLEAPPS.get((int)itemid).get("activityInfo")
			};
			//
			wdgSpinnerConverters = (Spinner) findViewById(R.id.SpinnerConverters);
			long cnvkey = wdgSpinnerConverters.getSelectedItemId();
			String cnv = 
				CONVERTERS.get((int)cnvkey).get("url")
			;
			startBrowserApp(cnv,app[0], app[1]);
		} else if (v == wdgSettingBtn) {
			Log.d(TAG, "click:setting");
		}
		return;
	}

	// メニューを作成
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// メニューインフレーターを取得
		MenuInflater inflater = getMenuInflater();
		// xmlのリソースファイルを使用してメニューにアイテムを追加
		inflater.inflate(R.menu.main, menu);
		// できたらtrueを返す
		return true;
	}

	// メニューがクリックされた際
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case INSERT_ID:
			createItem();
			return true;
		case IMPORT_ID:
			alertdialog("sorry", "not impremented yet.");
			return true;
		case EXPORT_ID:
			alertdialog("sorry", "not impremented yet.");
			return true;
		case INITIALIZE_ID:
			initializeItem();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	// //////////////////////////////////////////////////////////////////////

	// intent投げ先activitiesをspinnerに積む処理
	private void buildBrowserSpinner() {

		// アダプターを設定します
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		wdgSpinnerBrowsers = (Spinner) findViewById(R.id.SpinnerBrowsers);
		wdgSpinnerBrowsers.setAdapter(adapter);
		// アイテムを追加します
		SUITABLEAPPS = getSuitableActivities();
		for (int i = 0; i < SUITABLEAPPS.size(); i++) {
			adapter.add(SUITABLEAPPS.get(i).get("label"));
		}
		// スピナーのアイテムが選択された時に呼び出されるコールバックを登録します
		wdgSpinnerBrowsers
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						Spinner spinner = (Spinner) parent;
						// 選択されたアイテムを取得します
						String item = (String) spinner.getSelectedItem();
						Log.d(TAG, "spb:sel:" + item);
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});
		return;
	}

	// intentに適したactivityを一覧で引いてくる処理。
	private ArrayList<HashMap<String, String>> getSuitableActivities() {
		ArrayList<HashMap<String, String>> apps = new ArrayList<HashMap<String, String>>();

		//
		final PackageManager pm = this.getPackageManager();
		final Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.addCategory(Intent.CATEGORY_BROWSABLE);
		intent.setData(Uri.parse("http://cnn.com/"));
		List<ResolveInfo> list = pm.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);

		if (list == null) {
			return apps;
		}

		Collections.sort(list, new ResolveInfo.DisplayNameComparator(pm));

		// アクティビティ情報の取得
		for (int i = 0; i < list.size(); i++) {
			HashMap<String, String> appinfo = new HashMap<String, String>();
			ResolveInfo info = list.get(i);
			//
			appinfo.put("label", (String) info.loadLabel(pm));
			Log.d(TAG, "gsa:label:" + (String) info.loadLabel(pm));
			//
			appinfo.put("packageName",
					(String) info.activityInfo.applicationInfo.packageName);
			Log.d(TAG, "gsa:pName:"
					+ (String) info.activityInfo.applicationInfo.packageName);
			//
			appinfo.put("activityInfo", (String) info.activityInfo.name);
			Log.d(TAG, "gsa:aName:" + (String) info.activityInfo.name);
			//
			apps.add(appinfo);
		}
		return apps;

	}

	// 変換方法一覧spinnerに変換候補を流し込む
	private void buildConvertSpinner() {
		Log.d(TAG, "init:dialog");

		// アダプターを設定します
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		wdgSpinnerConverters = (Spinner) findViewById(R.id.SpinnerConverters);
		wdgSpinnerConverters.setAdapter(adapter);
		// アイテムを追加します
		Cursor c = mDbHelper.fetchAllItems();
		startManagingCursor(c);
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++) {
			//選択項目に追加
			adapter.add(c.getString(1));
			//変換項目キャッシュに追加
			HashMap<String,String> hm = new HashMap<String,String>();
			hm.put("title",c.getString(1));
			hm.put("url",c.getString(2));
			CONVERTERS.add(hm);
			c.moveToNext();
		}

		// スピナーのアイテムが選択された時に呼び出されるコールバックを登録します
		wdgSpinnerConverters
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						Spinner spinner = (Spinner) parent;
						// 選択されたアイテムを取得します
						Cursor c = mDbHelper.fetchItemByTitle((String) spinner
								.getSelectedItem());
						startManagingCursor(c);
						String item = c.getString(2);
						Log.d(TAG, "bcs:ois:sel:" + item);
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});

		Log.d(TAG, "init:dialog:show:done");
		return;
	}

	// URLを加工してブラウザを起動する。
	private void startBrowserApp(String cv,String pkg, String act) {
		Log.d(TAG, "sba:cv:" + cv);
		URI = Uri.parse(cv + URI.toString());
		Log.d(TAG, "sba:urlmod:" + URI);
		Log.d(TAG, "sba:pkg:" + pkg);
		Log.d(TAG, "sba:act:" + act);

		ComponentName comp = new ComponentName(pkg,act);
		Intent intent = new Intent(Intent.ACTION_VIEW, URI);
		intent.setComponent(comp);		
		startActivity(intent);
		finish();
		Log.d(TAG, "successfully launch browser.");
		return;
	}

	// 設定画面を開く
	private void startSettingActivity(long id) {
		Log.d(TAG, "ssa:openSetting:" + id);
		Intent i = new Intent(this, SettingActivity.class);
		// クリックされた行のIDをintentに埋める。これで項目ID取れるのなー
		i.putExtra(Converter.KEY_ROWID, id);
		startActivityForResult(i, ACTIVITY_EDIT);
		Log.d(TAG, "ssa:launch setting activity.");
		return;
	}

	// 項目を新規作成
	private void createItem() {
		Intent i = new Intent(this, SettingActivity.class);
		startActivityForResult(i, ACTIVITY_CREATE);
	}

	// 項目を初期化
	private void initializeItem() {
		// confirmダイアログを出す
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.alert_init_title);
		builder.setMessage(R.string.alert_init_msg);
		builder.setPositiveButton("OK",
				new android.content.DialogInterface.OnClickListener() {
					public void onClick(android.content.DialogInterface dialog,
							int whichButton) {
						// OKなら
						setResult(RESULT_OK);
						mDbHelper.initdb();
						// dispSelectDialog();
					}
				});
		builder.setNegativeButton("Cancel",
				new android.content.DialogInterface.OnClickListener() {
					public void onClick(android.content.DialogInterface dialog,
							int whichButton) {
						// キャンセルなら
						setResult(RESULT_CANCELED);
						return;
					}
				});
		builder.create();
		builder.show();
	}

	// TODO:エクスポート処理
	// TODO:インポート処理

	// メッセージを出す
	private void alertdialog(String title, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setMessage(msg);
		builder.setPositiveButton("OK",
				new android.content.DialogInterface.OnClickListener() {
					public void onClick(android.content.DialogInterface dialog,
							int whichButton) {
						setResult(RESULT_OK);
					}
				});
		builder.create();
		builder.show();
	}

}