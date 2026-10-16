package io.github.alittlecutesheep.eyerest;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/** 震動：以「鬧鐘」用途送出，靜音模式下也會震。 */
final class Vibes {
    private Vibes() {}

    /** 休息畫面跳出時的輕震。 */
    static void tick(Context c) {
        play(c, new long[]{0, 70}, new int[]{0, 150});
    }

    /** 休息結束。strength：0 輕、1 標準、2 強。 */
    static void end(Context c, int strength) {
        switch (strength) {
            case 0:
                play(c, new long[]{0, 160, 140, 160}, new int[]{0, 110, 0, 110});
                break;
            case 2:
                play(c, new long[]{0, 550, 180, 550, 180, 900}, new int[]{0, 255, 0, 255, 0, 255});
                break;
            default:
                play(c, new long[]{0, 320, 160, 320, 160, 320}, new int[]{0, 200, 0, 200, 0, 200});
                break;
        }
    }

    private static Vibrator vibrator(Context c) {
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = c.getSystemService(VibratorManager.class);
            return vm == null ? null : vm.getDefaultVibrator();
        }
        return c.getSystemService(Vibrator.class);
    }

    @SuppressWarnings("deprecation")
    private static void play(Context c, long[] timings, int[] amplitudes) {
        Vibrator v = vibrator(c);
        if (v == null || !v.hasVibrator()) return;
        VibrationEffect effect = v.hasAmplitudeControl()
                ? VibrationEffect.createWaveform(timings, amplitudes, -1)
                : VibrationEffect.createWaveform(timings, -1);
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM));
        } else {
            v.vibrate(effect, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
        }
    }
}
