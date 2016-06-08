package jp.codepanic.batope2;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Account {

	MainActivity _activity = null;

	String		_prefName;

	int			_requestCode;

	long 		_powMSEC;	// 支給までの時間（ミリ秒）
	String		_account;
	int			_bitiku;
	int			_sikyu;
	int			_sikyuMax;
	int			_min;
	Calendar	_calAlarm = Calendar.getInstance();

	Timer		_timer 		= null;
	Handler		_handlerTimer	= new Handler();

	Button		_btnGo;
	TextView	_textAccount;
	TextView	_textBitiku;
	TextView	_textSikyu;
	TextView	_textMin;
	TextView	_textNokori;
    TextView    _textMantan;

	// ユニークなIDを取得するために、R.layout.mainのリソースIDを使います
	private static int NOTIFICATION_ID = R.layout.activity_main;

	public Account(MainActivity ac){
		_activity = ac;
	}

	public void initialize(
			String   prefname,
			int		 requestcode,
			Button	 go,
			TextView account,
			TextView bitiku,
			TextView sikyu,
			TextView min,
			TextView nokori,
            TextView mantan){

		_prefName 		= prefname;

		_requestCode 	= requestcode;

		_btnGo			= go;
		_textAccount	= account;
		_textBitiku		= bitiku;
		_textSikyu		= sikyu;
		_textMin		= min;
		_textNokori		= nokori;
        _textMantan     = mantan;
	}

	public void initData(){
		_account	= "account name";
		_bitiku 	= 0;
		_sikyu		= 3;
		_sikyuMax	= 3;
		_min		= 120;
		_powMSEC	= 1000*60*_min;
	}

	void save(){
		SharedPreferences pref = _activity.getSharedPreferences(_prefName, Context.MODE_PRIVATE);
		Editor e = pref.edit();

		e.putString("account", _account);
		e.putInt("bitiku", 	_bitiku);
		e.putInt("sikyu", 	_sikyu);
		e.putInt("sikyuMax",_sikyuMax);
		e.putInt("min", 	_min);

		_powMSEC= 1000*60*_min;

//		// System.currentTimeMillis()を呼び出すと、1970年1月1日からのミリ秒を取得できます。
//		long currentTimeMillis = System.currentTimeMillis();
//		e.putLong("time", currentTimeMillis);

		e.putLong("alarm", _calAlarm.getTimeInMillis());

		e.commit();
	}

	void load(){
		SharedPreferences pref = _activity.getSharedPreferences(_prefName, Context.MODE_PRIVATE);

		_account= pref.getString("account", "account name");
		_bitiku = pref.getInt("bitiku", 0);
		_sikyu	= pref.getInt("sikyu",  3);
		_sikyuMax=pref.getInt("sikyuMax",3);
		_min	= pref.getInt("min",    120);
		_powMSEC= 1000*60*_min;

		long msec = pref.getLong("alarm", 0);
		_calAlarm.setTimeInMillis(msec);

		// 経過時間から出撃回数計算
		if(_sikyu < _sikyuMax && _calAlarm.getTimeInMillis() > 0){

			int num = 0;

			// 記憶していた時刻を超えている？
			if(System.currentTimeMillis() > _calAlarm.getTimeInMillis())
				num ++;

			// 差分の時間分加算
			long span 	= System.currentTimeMillis() - _calAlarm.getTimeInMillis();
			if(span > _powMSEC){
				num	+= (int)(span / _powMSEC);
			}
			_sikyu += num;
		}

		if(_sikyu > _sikyuMax){
			_sikyu = _sikyuMax;

			stopTimer();
			cancelAlarm();
		}

		//
		// 次の支給時刻を計算
		//
		if(_sikyu < _sikyuMax && _calAlarm.getTimeInMillis() > 0){
			// 支給時刻が現在時刻以降になるまで加算し続ける
			while(_calAlarm.getTimeInMillis() < System.currentTimeMillis())
				_calAlarm.add(Calendar.MILLISECOND, (int)_powMSEC);

			// 次のアラームセット
			int min = (int)((System.currentTimeMillis() -_calAlarm.getTimeInMillis()) / 1000 / 60);
			updateAlarm(min);
		}
	}

	public void onClick(View v){
		if(v == _textAccount){
			showAccountDialog();
		}
		else if(v == _textBitiku){
			showBitikuDialog();
			clearNotiry();
		}
		else if(v == _textSikyu){
			showSikyuDialog();
			clearNotiry();
		}
		else if(v == _textMin){
			showMinDialog();
			clearNotiry();
		}
		else if(v == _textNokori){
			showNokoriDialog();
			clearNotiry();
		}
		else if(v == _btnGo){
			go();
			clearNotiry();
		}
	}

	void clearNotiry(){
		NotificationManager mManager = (NotificationManager)_activity.getSystemService(Context.NOTIFICATION_SERVICE);
		mManager.cancel(NOTIFICATION_ID);
	}

	void go(){
		if(_sikyu > 0){
			if(_sikyu == _sikyuMax)
				_calAlarm.setTimeInMillis(0);

			_sikyu --;
			startAlarm();
		}else if(_bitiku > 0){
			_bitiku --;
			startAlarm();
		}else{
		    AlertDialog.Builder alertDialog = new AlertDialog.Builder(_activity);
		    alertDialog.setIcon(R.drawable.ic_launcher);
		    alertDialog.setTitle("出撃不可");
		    alertDialog.setMessage("エネルギーが足りません！");
		    alertDialog.setPositiveButton("OK",new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog,int whichButton) {
		            _activity.setResult(Activity.RESULT_OK);
		        }
		    });
		    alertDialog.create();
		    alertDialog.show();
		}

		refreshText();
	}

	void updateAlarm(int min){
		if(0 < min && min <= _min){

			// 次のアラーム時間計算
			_calAlarm.setTimeInMillis( System.currentTimeMillis() + min * 60 * 1000 );

			// ReceivedActivityを呼び出すインテントを作成
			Intent i = new Intent(_activity.getApplicationContext(), AlarmReceiver.class);

			// ブロードキャストを投げるPendingIntentの作成
			PendingIntent sender = PendingIntent.getBroadcast(_activity, _requestCode, i, 0);

			// 古いアラーム削除 -> アラーム設定
			AlarmManager am = (AlarmManager)_activity.getSystemService(Context.ALARM_SERVICE);
			am.cancel(sender);
			am.set(AlarmManager.RTC_WAKEUP, _calAlarm.getTimeInMillis(), sender);

			// 保存
			save();

			// タイマー開始
			startTiemr();
		}
	}

	void startAlarm(){
		if(_calAlarm.getTimeInMillis() == 0){
			// 次のアラーム時間計算
			_calAlarm.setTimeInMillis( System.currentTimeMillis() + _powMSEC );
		}

		// ReceivedActivityを呼び出すインテントを作成
		Intent i = new Intent(_activity.getApplicationContext(), AlarmReceiver.class);

		// ブロードキャストを投げるPendingIntentの作成
		PendingIntent sender = PendingIntent.getBroadcast(_activity, _requestCode, i, 0);

		// 古いアラーム削除 -> アラーム設定
		AlarmManager am = (AlarmManager)_activity.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
		am.set(AlarmManager.RTC_WAKEUP, _calAlarm.getTimeInMillis(), sender);

		// 保存
		save();

		// タイマー開始
//		Intent intent = new Intent(getApplicationContext(), TimerService.class);
//		startService(intent);
		startTiemr();
	}

	void cancelAlarm(){
		// ReceivedActivityを呼び出すインテントを作成
		Intent i = new Intent(_activity.getApplicationContext(), AlarmReceiver.class);

		// ブロードキャストを投げるPendingIntentの作成
		PendingIntent sender = PendingIntent.getBroadcast(_activity, _requestCode, i, 0);

		// 古いアラーム削除 -> アラーム設定
		AlarmManager am = (AlarmManager)_activity.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);

		// 保存
		_calAlarm.setTimeInMillis(0);
		save();
	}

	void startTiemr(){
		stopTimer();

		if(_sikyu < _sikyuMax){
			if(_timer == null) _timer = new Timer();

			_timer.schedule( new TimerTask(){
				@Override
				public void run(){
					_handlerTimer.post(new Runnable(){
						@Override
						public void run(){
							onTick();
						}
					});
				}
			}, 0, 1000);
		}
	}

	public void stopTimer(){
		if(_timer != null){
			_timer.cancel();
			_timer.purge();
			_timer = null;
		}
	}

	void showAccountDialog(){
		final EditText editView = new EditText(_activity);

		final AlertDialog dlg = new AlertDialog.Builder(_activity)
		    .setIcon(R.drawable.ic_launcher)
		    .setTitle("アカウント名")
		    .setView(editView)
		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        	try{
			        	_account = editView.getText().toString().trim();
			        	refreshText();
			        	save();
		        	}catch(Exception e){
		        	}
		        }
		    })
		    .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        }
		    })
		    .create();

		editView.setOnFocusChangeListener(new OnFocusChangeListener() {
	        @Override
	        public void onFocusChange(View v, boolean hasFocus) {
	            if (hasFocus) {
	        		editView.setInputType(InputType.TYPE_CLASS_TEXT);
	            	dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	        		editView.setText(_account);
	        		editView.setSelection(0, _account.length());
	            }
	        }
	    });

		dlg.show();
	}

	void showBitikuDialog(){
		final EditText editView = new EditText(_activity);

		final AlertDialog dlg = new AlertDialog.Builder(_activity)
		    .setIcon(R.drawable.ic_launcher)
		    .setTitle("備蓄エネルギー")
		    .setView(editView)
		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        	try{
			        	String num = editView.getText().toString().trim();
			        	if(num != null && num.length() > 0)
			        		_bitiku = Integer.parseInt(num);
			        	refreshText();
			        	save();
		        	}catch(Exception e){
		        	}
		        }
		    })
		    .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        }
		    })
		    .create();

		editView.setOnFocusChangeListener(new OnFocusChangeListener() {
	        @Override
	        public void onFocusChange(View v, boolean hasFocus) {
	            if (hasFocus) {
	        		editView.setInputType(InputType.TYPE_CLASS_NUMBER);
	            	dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	        		String str = String.format("%d", _bitiku);
	        		editView.setText(str);
	        		editView.setSelection(0, str.length());
	            }
	        }
	    });

		dlg.show();
	}

	void showSikyuDialog(){

		LayoutInflater inflater = (LayoutInflater)_activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		final View layout = inflater.inflate(R.layout.dlg_sikyu, (ViewGroup)_activity.findViewById(R.id.layout_root_sikyu));

		final EditText editSikyu= (EditText) layout.findViewById(R.id.editSikyu);
		final EditText editMax 	= (EditText) layout.findViewById(R.id.editSikyuMax);

		editSikyu.setText( String.format("%d", _sikyu) );
		editMax.setText(   String.format("%d", _sikyuMax) );

		final AlertDialog dlg = new AlertDialog.Builder(_activity)
		    .setIcon(R.drawable.ic_launcher)
		    .setTitle("支給エネルギー")
		    .setView(layout)
		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        	try{
		        		String num = editSikyu.getText().toString().trim();
		        		String max = editMax.getText().toString().trim();

			        	if(num != null && num.length() > 0)
			        		_sikyu = Integer.parseInt(num);

			        	if(max !=null && max.length() > 0)
			        		_sikyuMax = Integer.parseInt(max);

			        	refreshText();
			        	save();
		        	}catch(Exception e){
		        	}
		        }
		    })
		    .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        }
		    }).create();

		editSikyu.setOnFocusChangeListener(new OnFocusChangeListener() {
	        @Override
	        public void onFocusChange(View v, boolean hasFocus) {
	            if (hasFocus) {
	            	editSikyu.setInputType(InputType.TYPE_CLASS_NUMBER);
	            	dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	            	editSikyu.setSelectAllOnFocus(true);
	            }
	        }
	    });

		editMax.setOnFocusChangeListener(new OnFocusChangeListener() {
	        @Override
	        public void onFocusChange(View v, boolean hasFocus) {
	            if (hasFocus) {
	            	editMax.setInputType(InputType.TYPE_CLASS_NUMBER);
	            	dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	            	editMax.setSelectAllOnFocus(true);
	            }
	        }
	    });

//		dlg.setOnShowListener(new OnShowListener() {
//
//			@Override
//			public void onShow(DialogInterface dialog) {
//            	editSikyu.setInputType(InputType.TYPE_CLASS_NUMBER);
//            	dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
//            	editSikyu.setSelectAllOnFocus(true);
//			}
//		});

		dlg.show();

		editSikyu.clearFocus();
		editSikyu.setSelectAllOnFocus(true);

//		final EditText editView = new EditText(_activity);
//
//		final AlertDialog dlg = new AlertDialog.Builder(_activity)
//		    .setIcon(R.drawable.ic_launcher)
//		    .setTitle("支給エネルギー")
//		    .setView(editView)
//		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//		        @Override
//				public void onClick(DialogInterface dialog, int whichButton) {
//		        	try{
//			        	String num = editView.getText().toString().trim();
//			        	if(num != null && num.length() > 0)
//			        		_sikyu = Integer.parseInt(num);
//			        	refreshText();
//			        	save();
//		        	}catch(Exception e){
//		        	}
//		        }
//		    })
//		    .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
//		        @Override
//				public void onClick(DialogInterface dialog, int whichButton) {
//		        }
//		    })
//		    .create();
//
//		editView.setOnFocusChangeListener(new OnFocusChangeListener() {
//	        @Override
//	        public void onFocusChange(View v, boolean hasFocus) {
//	            if (hasFocus) {
//	        		editView.setInputType(InputType.TYPE_CLASS_NUMBER);
//	            	dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
//	        		String str = String.format("%d", _sikyu);
//	        		editView.setText(str);
//	        		editView.setSelection(0, str.length());
//	            }
//	        }
//	    });
//
//		dlg.show();
	}

	void showMinDialog(){
		final EditText editView = new EditText(_activity);

		final AlertDialog dlg = new AlertDialog.Builder(_activity)
		    .setIcon(R.drawable.ic_launcher)
		    .setTitle("支給時間")
		    .setView(editView)
		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        	try{
			        	String num = editView.getText().toString().trim();
			        	if(num != null && num.length() > 0)
			        		_min = Integer.parseInt(num);
			        	refreshText();
			        	save();
		        	}catch(Exception e){
		        	}
		        }
		    })
		    .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        }
		    })
		    .create();

		editView.setOnFocusChangeListener(new OnFocusChangeListener() {
	        @Override
	        public void onFocusChange(View v, boolean hasFocus) {
	            if (hasFocus) {
	        		editView.setInputType(InputType.TYPE_CLASS_NUMBER);
	            	dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	        		String str = String.format("%d", _min);
	        		editView.setText(str);
	        		editView.setSelection(0, str.length());
	            }
	        }
	    });

		dlg.show();
	}

	void showNokoriDialog(){
		final EditText editView = new EditText(_activity);

		final AlertDialog dlg = new AlertDialog.Builder(_activity)
		    .setIcon(R.drawable.ic_launcher)
		    .setTitle("次の支給まで残り時間(分)")
		    .setView(editView)
		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        	try{
			        	String num = editView.getText().toString().trim();
			        	if(num != null && num.length() > 0){
			        		int min = Integer.parseInt(num);
			        		updateAlarm(min);
			        	}
			        	refreshText();
			        	save();
		        	}catch(Exception e){
		        	}
		        }
		    })
		    .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
		        @Override
				public void onClick(DialogInterface dialog, int whichButton) {
		        }
		    })
		    .create();

		editView.setOnFocusChangeListener(new OnFocusChangeListener() {
	        @Override
	        public void onFocusChange(View v, boolean hasFocus) {
	            if (hasFocus) {
	        		editView.setInputType(InputType.TYPE_CLASS_NUMBER);
	            	dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

	    			long span = _calAlarm.getTimeInMillis() - System.currentTimeMillis();
	    			int min = (int)(span / 1000L / 60L);

	    			String str = String.format("%d", min);
	    			editView.setText(str);
	        		editView.setSelection(0, str.length());
	            }
	        }
	    });

		dlg.show();
	}

	public void onTick(){
		// チャージ？
		if(_sikyu < _sikyuMax){
			long span = _calAlarm.getTimeInMillis() - System.currentTimeMillis();

			if(span < 1000){
				_sikyu ++;

//				fire();
//              _activity.vibe(_sikyu); ※onPauseでタイマー停止されてるから意味ない。AlarmReceiver側でVibeすること

				_calAlarm.setTimeInMillis(0);
				startAlarm();

				if(_sikyu >= _sikyuMax){
					cancelAlarm();
					stopTimer();
				}
			}
		}

		refreshText();
	}

//	public void fire(){
//		//
//		//Notificationインスタンスの生成と設定
//		//
//		Notification n = new Notification();
//		n.icon = R.drawable.ic_launcher;
//		n.tickerText = "支給エネルギーチャージ！";
//		n.number = _sikyu;
////		try{
////			SimpleDateFormat date = new SimpleDateFormat("yy/mm/dd HH:mm");
////			n.when = date.parse("2010/5/20").getTime();
////		}catch(Exception e){
//			n.when = System.currentTimeMillis();
////		}
//
//		Intent i = new Intent(_activity.getApplicationContext(),MainActivity.class);
//		PendingIntent pend = PendingIntent.getActivity(_activity, 0, i, 0);
//		n.setLatestEventInfo(
//			_activity.getApplicationContext(),
//			"バトオペ出撃Notifier",
//			String.format("%d回出撃可能です！", _sikyu),
//			pend);
//
//		//NotificationManagerへNotificationインスタンスを設定して発行！
//		NotificationManager mManager = (NotificationManager)_activity.getSystemService(Context.NOTIFICATION_SERVICE);
//		mManager.cancel(NOTIFICATION_ID);
//		mManager.notify(NOTIFICATION_ID, n);
//
//		_activity.vibe();
//	}

	public void refreshText(){
		//
		// 上限チェック
		//
		if(_bitiku > 99)	_bitiku = 99;
		if(_bitiku < 0)		_bitiku = 0;

		if(_sikyu > _sikyuMax)	_sikyu  = _sikyuMax;
		if(_sikyu < 0)			_sikyu	= 0;

		//
		// ラベル
		//
		if(_sikyu >= _sikyuMax) {
            _textNokori.setText("次の支給まで　　--分--秒");
            _textMantan.setText("支給満タンまで　--分--秒");
        }else{
			long span = _calAlarm.getTimeInMillis() - System.currentTimeMillis();
			int min = (int)(span / 1000L / 60L);
			int sec = (int)((span - (min * 60L * 1000L)) / 1000L);
            _textNokori.setText(String.format("次の支給まで　　%d分%02d秒", min, sec));

            if(_sikyuMax - _sikyu >= 2) {
                min += (_sikyuMax - _sikyu - 1) * _min;
            }
            _textMantan.setText(String.format("支給満タンまで　%d分%02d秒", min, sec));
		}

		_textAccount.setText(_account);

		_textBitiku.setText(String.format("備蓄 %02d / 99", _bitiku));
		_textSikyu.setText(String.format("支給 %d / %d", _sikyu, _sikyuMax));
		_textMin.setText(String.format("支給時間 %d分", _min));

		_textBitiku.setTextColor( _bitiku > 0 ? Color.WHITE : Color.RED);
		_textSikyu.setTextColor(  _sikyu  > 0 ? Color.WHITE : Color.RED);
	}

}
