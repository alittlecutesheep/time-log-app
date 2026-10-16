package io.github.alittlecutesheep.eyerest;

import java.util.Calendar;
import java.util.Locale;

/** 時間格式化。 */
final class Fmt {
    private Fmt() {}

    /** 倒數用：MM:SS，超過一小時為 H:MM:SS。秒數無條件進位，歸零前不會先顯示 00:00。 */
    static String countdown(long ms) {
        long s = Math.max(0L, (ms + 999L) / 1000L);
        long h = s / 3600L;
        long m = (s % 3600L) / 60L;
        long sec = s % 60L;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, sec);
        return String.format(Locale.US, "%02d:%02d", m, sec);
    }

    /** 時鐘時間 HH:MM */
    static String clock(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    static String minuteOfDay(int m) {
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60);
    }

    /** 休息長度的中文描述：20 秒、1 分 30 秒、5 分鐘 */
    static String restLabel(int sec) {
        if (sec < 60) return sec + " 秒";
        if (sec % 60 == 0) return (sec / 60) + " 分鐘";
        return (sec / 60) + " 分 " + (sec % 60) + " 秒";
    }

    /** 用眼時間 H:MM */
    static String hoursMinutes(long ms) {
        long m = ms / 60000L;
        return String.format(Locale.US, "%d:%02d", m / 60L, m % 60L);
    }

    static String twoDigits(int v) {
        return String.format(Locale.US, "%02d", v);
    }
}
