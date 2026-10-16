package io.github.alittlecutesheep.eyerest;

import android.content.Context;

import java.util.Calendar;

/** 勿擾時段的時間計算，支援跨午夜（例如 23:00 → 07:00）。 */
final class Quiet {
    private Quiet() {}

    private static boolean usable(Context c) {
        return Prefs.quietEnabled(c) && Prefs.quietStart(c) != Prefs.quietEnd(c);
    }

    static boolean active(Context c, long t) {
        if (!usable(c)) return false;
        int s = Prefs.quietStart(c);
        int e = Prefs.quietEnd(c);
        int m = minuteOfDay(t);
        return s < e ? (m >= s && m < e) : (m >= s || m < e);
    }

    /** 下一次勿擾開始的時間；未啟用時回傳 -1。 */
    static long nextStart(Context c, long t) {
        if (!usable(c)) return -1;
        return nextOccurrence(t, Prefs.quietStart(c));
    }

    static long endAfter(Context c, long t) {
        return nextOccurrence(t, Prefs.quietEnd(c));
    }

    static int minuteOfDay(long t) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    private static long nextOccurrence(long t, int minuteOfDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t);
        cal.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        cal.set(Calendar.MINUTE, minuteOfDay % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= t) cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }
}
