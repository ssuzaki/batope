package jp.codepanic.batope2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

public class MainActivity
	extends Activity
	implements OnCheckedChangeListener {

	final static int MAX_ACCOUNT = 10;

	Account[]	_account = new Account[MAX_ACCOUNT];

	int _accountNum = 1;

	ToggleButton	_toggleVibe;
	boolean			_vibe = true;

    LinearLayout    _root;
    LayoutInflater  _inflater;
    View[]          _viewAccount = new View[MAX_ACCOUNT];

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 画面向き固定
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// タイトルバーを隠す
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_main);

		//
		// 各アカウント初期処理
		//
        _inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _root = (LinearLayout) findViewById(R.id.llRoot);

		for(int i = 0; i < MAX_ACCOUNT; i++){
			_account[i] = new Account(this);

            View view = _inflater.inflate(R.layout.account, _root, false);

            _root.addView(view);

            String prefName = String.format("pref%d", i);

            _account[i].initialize(
                    prefName,
                    i,
                    (Button)   view.findViewById(R.id.buttonGo),
                    (TextView) view.findViewById(R.id.textViewAccount),
                    (TextView) view.findViewById(R.id.textViewBitiku),
                    (TextView) view.findViewById(R.id.textViewSikyu),
                    (TextView) view.findViewById(R.id.textViewMin),
                    (TextView) view.findViewById(R.id.textViewNokori),
                    (TextView) view.findViewById(R.id.textViewMantan)
            );

            _viewAccount[i] = view;
		}

		_toggleVibe		= (ToggleButton) findViewById(R.id.toggleButtonVibe);

		_toggleVibe.setOnCheckedChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		load();

		_toggleVibe.setChecked(_vibe);

		for(int i = 0; i < _accountNum; i++){
			_account[i].refreshText();
			_account[i].startTiemr();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		for(int i = 0; i < _accountNum; i++){
			_account[i].stopTimer();
		}

		save();
	}

	@Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		_vibe = isChecked;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.menu_tweet:
			tweet();
			break;
		case R.id.menu_review:
			review();
			break;
		case R.id.menu_version:
			showVersion();
			break;
		case R.id.menu_exit:
			showExit();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	void tweet(){
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
//            intent.setType("application/twitter"); だめだった
            intent.putExtra(Intent.EXTRA_TEXT, "Android版 バトオペ出撃Notifier公開中！ https://play.google.com/store/apps/details?id=jp.codepanic.batope2 #バトオペ");
            startActivity(intent);
        } catch (Exception e) {
        }
    }

	void review(){
		try{
			Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=jp.codepanic.batope2");
			Intent i = new Intent(Intent.ACTION_VIEW,uri);
			startActivity(i);
		}catch(Exception e){
		}
	}

	void showExit(){
		new AlertDialog.Builder(this)
	    .setIcon(R.drawable.ic_launcher)
		.setTitle("終了しますか？")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            	finish();
            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        })
		.show();
	}

	void showVersion(){
	    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
	    alertDialog.setIcon(R.drawable.ic_launcher);
	    alertDialog.setTitle( getString(R.string.app_name) );
	    alertDialog.setMessage( getVersionName() );
	    alertDialog.setPositiveButton("OK",new DialogInterface.OnClickListener() {
	        @Override
			public void onClick(DialogInterface dialog,int whichButton) {
	            setResult(RESULT_OK);
	        }
	    });
	    alertDialog.create();
	    alertDialog.show();
	}

	String getVersionName(){
        int versionCode = 0;
        String versionName = "";
        PackageManager packageManager = this.getPackageManager();

        try {
               PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
               versionCode = packageInfo.versionCode;
               versionName = packageInfo.versionName;
        } catch (NameNotFoundException e) {
               e.printStackTrace();
        }

        return "version : " + versionName;
	}

	void save(){
		SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
		Editor e = pref.edit();
		e.putBoolean("vibe", _vibe);
		e.putInt("num", _accountNum);
		e.commit();

		for(int i = 0; i < _accountNum; i++){
			_account[i].save();
		}
	}

	void load(){
		SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
		_vibe		= pref.getBoolean("vibe", true);
		_accountNum	= pref.getInt("num", 1);

		for(int i = 0; i < _accountNum; i++){
			_account[i].load();
            _viewAccount[i].setVisibility(View.VISIBLE);
		}
        for(int i = _accountNum; i < MAX_ACCOUNT; i++){
            _viewAccount[i].setVisibility(View.GONE);
        }
	}

//	public void vibe(int num){
//		//
//		// バイブレーション
//		//
//		if(_vibe){
////			long pattern[] = {100, 100, 100, 100, 100, 100}; // 1000ミリ秒OFF→500ミリ秒ON→3000ミリ秒OFF→500ミリ秒ON→1000ミリ秒OFF→500ミリ秒ON
//            long pattern[] = new long[num * 2];
//            for(int i = 0; i < num * 2; i++){
//                pattern[i] = 100;
//            }
//
//            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(pattern, -1); // 定義したパターン・リピートなしでバイブレーション開始
//		}
//	}

	void addAccount(){
		if(_accountNum >= 10){
		    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
		    alertDialog.setIcon(R.drawable.ic_launcher);
		    alertDialog.setTitle( getString(R.string.app_name) );
		    alertDialog.setMessage( "アカウントは最大１０個までです。" );
		    alertDialog.setPositiveButton("OK",new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog,int whichButton) {
		            setResult(RESULT_OK);
		        }
		    });
		    alertDialog.create();
		    alertDialog.show();

			return;
		}

        _viewAccount[_accountNum].setVisibility(View.VISIBLE);

		_account[_accountNum].initData();
		_account[_accountNum].refreshText();
		_account[_accountNum].startTiemr();

		_accountNum ++;
		
		save();
	}

	void deleteAccount(){
		if(_accountNum <= 1){
		    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
		    alertDialog.setIcon(R.drawable.ic_launcher);
		    alertDialog.setTitle( getString(R.string.app_name) );
		    alertDialog.setMessage( "アカウントは最低１つ必要です。" );
		    alertDialog.setPositiveButton("OK",new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog,int whichButton) {
		            setResult(RESULT_OK);
		        }
		    });
		    alertDialog.create();
		    alertDialog.show();

			return;
		}

		new AlertDialog.Builder(this)
	    .setIcon(R.drawable.ic_launcher)
		.setTitle("最後に登録したアカウントを削除しますか？")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            	_accountNum --;

                _viewAccount[_accountNum].setVisibility(View.GONE);

            	_account[_accountNum].cancelAlarm();
            	_account[_accountNum].stopTimer();
            	_account[_accountNum].initData();
            	_account[_accountNum].save();
            	
            	save();
            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        })
		.show();
	}

	public void onClick(View v){

		switch(v.getId()){
			case R.id.buttonAdd:
				addAccount();
				break;
			case R.id.buttonDelete:
				deleteAccount();
				break;
			default:
				for(int i = 0; i < _accountNum; i++){
					_account[i].onClick(v);
				}
				break;
		}

	}
}
