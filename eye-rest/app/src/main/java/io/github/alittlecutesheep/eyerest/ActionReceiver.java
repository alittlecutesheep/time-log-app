package io.github.alittlecutesheep.eyerest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 通知上的按鈕：關閉、開啟、重新計時。 */
public class ActionReceiver extends BroadcastReceiver {
    static final String ACTION_DISABLE = "io.github.alittlecutesheep.eyerest.DISABLE";
    static final String ACTION_ENABLE = "io.github.alittlecutesheep.eyerest.ENABLE";
    static final String ACTION_RESET = "io.github.alittlecutesheep.eyerest.RESET";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_DISABLE.equals(action)) {
            Engine.disable(context, true);
        } else if (ACTION_ENABLE.equals(action)) {
            Engine.enable(context);
        } else if (ACTION_RESET.equals(action)) {
            Engine.restartCycle(context);
        }
    }
}
