package io.github.alittlecutesheep.eyerest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 階段結束的精確鬧鐘。 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Engine.ACTION_PHASE_END.equals(intent.getAction())) {
            Engine.onAlarm(context);
        }
    }
}
