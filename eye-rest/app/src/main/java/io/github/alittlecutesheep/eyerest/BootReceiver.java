package io.github.alittlecutesheep.eyerest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 開機或 app 更新後恢復提醒。 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Engine.onBoot(context);
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Engine.sync(context);
            Engine.changed(context);
            Engine.ensureService(context);
        }
    }
}
