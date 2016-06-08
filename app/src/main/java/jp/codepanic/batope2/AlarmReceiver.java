package jp.codepanic.batope2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.Vibrator;

public class AlarmReceiver extends BroadcastReceiver {

	private static int NOTIFICATION_ID = R.layout.activity_main;

    @Override
	public void onReceive(Context context, Intent intent){
//    	Alarm Manager を使うときの注意点として、
//    	"onReceive() を返したあとは、すぐにスリープ状態になることがある" ということ
//    	これを回避するために、onReceive() 内で、
//    	別の wake lock policy を使ってスリープ状態をめるようにしてみる

        // スクリーンオン
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(
        	PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
        	PowerManager.ACQUIRE_CAUSES_WAKEUP	 |
        	PowerManager.ON_AFTER_RELEASE,
        	"hoge app Tag");
        wl.acquire();
        wl.release();

// ※他のアプリ利用中に前面にくるので廃止
        //
        // Mainを起こす。
        // android:launchMode="singleTask"なので二重機動されない
        // onResumeで目覚める
        //
//    	Intent i = new Intent(context, MainActivity.class);
//    	i.setAction("recv");
//    	PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
//
//    	try{
//    		pi.send();
//    	}catch(Exception e){
//    	}


        Notification n = new Notification();
		n.icon = R.drawable.ic_launcher;
		n.tickerText = "支給エネルギーチャージ！";

		Intent i = new Intent(context, MainActivity.class);
		PendingIntent pend = PendingIntent.getActivity(context, 0, i, 0);
		n.setLatestEventInfo(
			context,
			"バトオペ出撃Notifier",
			"出撃可能です！",
			pend);

		//NotificationManagerへNotificationインスタンスを設定して発行！
		NotificationManager mManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		mManager.cancel(NOTIFICATION_ID);
		mManager.notify(NOTIFICATION_ID, n);

		if(isVibe(context)){
			long pattern[] = {100, 100, 100, 100, 100, 100}; // 1000ミリ秒OFF→500ミリ秒ON→3000ミリ秒OFF→500ミリ秒ON→1000ミリ秒OFF→500ミリ秒ON
			((Vibrator) context.getSystemService(android.content.Context.VIBRATOR_SERVICE)).vibrate(pattern, -1); // 定義したパターン・リピートなしでバイブレーション開始
		}
    }

    boolean isVibe(Context c){
		SharedPreferences pref = c.getSharedPreferences("pref", android.content.Context.MODE_PRIVATE);
		return pref.getBoolean("vibe", true);
    }
}
