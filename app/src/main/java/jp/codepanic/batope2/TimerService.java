//package jp.codepanic.batope2;
//
//import java.util.Timer;
//import java.util.TimerTask;
//
//import android.app.Service;
//import android.content.Intent;
//import android.os.Handler;
//import android.os.IBinder;
//import android.util.Log;
//
//public class TimerService extends Service {
//	
//	public static MainActivity	_main;
//	
//	Timer	_timer;
//	
//	@Override
//	public IBinder onBind(Intent arg0) {
//		// TODO 自動生成されたメソッド・スタブ
//		return null;
//	}
//
//	@Override
//	public void onDestroy() {
//		super.onDestroy();
//		
//		_timer.cancel();
//		
//	}
//
//	@Override
//	@Deprecated
//	public void onStart(Intent intent, int startId) {
//		// TODO 自動生成されたメソッド・スタブ
//		super.onStart(intent, startId);
//		
//		startTimer();
//	}
//
//	void startTimer(){
//		Log.d("unko", "startTimer");
//		
//		if(_timer != null)
//			_timer.cancel();
//		
//		_timer = new Timer();
//		
//		final Handler handler = new Handler();
//		
//		_timer.schedule(
//			new TimerTask() {
//				
//				@Override
//				public void run() {
//					handler.post(new Runnable() {
//						
//						@Override
//						public void run() {
//							Log.d("unko", "timer run");
//							
////							long pattern[] = {10, 10}; // 1000ミリ秒OFF→500ミリ秒ON→3000ミリ秒OFF→500ミリ秒ON→1000ミリ秒OFF→500ミリ秒ON
////							((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(pattern, -1); // 定義したパターン・リピートなしでバイブレーション開始
//							
//							if(_main != null)
//								_main.onTick();
//						}
//					});
//				}
//			},
//			0,
//			1000);
//	}
//}
