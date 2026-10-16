package io.github.alittlecutesheep.eyerest;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/**
 * 開啟提醒期間常駐的前景服務：保持通知欄的倒數與開關，讓系統不要收掉 app，
 * 並監聽螢幕開關與解鎖（螢幕使用時間模式靠它暫停／繼續計時）。
 */
public class EyeRestService extends Service {

    // 螢幕開關與解鎖只能在執行中註冊接收，無法寫在 manifest
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Engine.onScreenChanged(context);
        }
    };
    private boolean receiverRegistered;

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }
        receiverRegistered = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = Notifier.buildStatus(this);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(Notifier.ID_STATUS, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(Notifier.ID_STATUS, n);
            }
        } catch (RuntimeException e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Prefs.enabled(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        Engine.sync(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver);
            receiverRegistered = false;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
