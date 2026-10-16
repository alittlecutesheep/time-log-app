package io.github.alittlecutesheep.eyerest;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/** 開啟提醒期間常駐的前景服務：保持通知欄的倒數與開關，並讓系統不要收掉 app。 */
public class EyeRestService extends Service {

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
    public IBinder onBind(Intent intent) {
        return null;
    }
}
