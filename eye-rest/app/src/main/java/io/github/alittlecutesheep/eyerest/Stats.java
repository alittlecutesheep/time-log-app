package io.github.alittlecutesheep.eyerest;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 每日統計：完成幾次休息（b_yyyyMMdd）、累計用眼毫秒數（w_yyyyMMdd）。 */
final class Stats {
    private static final int KEEP_DAYS = 40;
    private static final String[] WEEKDAYS = {"SU", "MO", "TU", "WE", "TH", "FR", "SA"};

    private Stats() {}

    static String dayKey(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return String.format(Locale.US, "%04d%02d%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static long startOfDay(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static long nextDayStart(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(t));
        c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    static void addBreak(Context ctx, long at) {
        SharedPreferences sp = Prefs.sp(ctx);
        String key = "b_" + dayKey(at);
        sp.edit().putInt(key, sp.getInt(key, 0) + 1).apply();
        prune(ctx, at);
    }

    /** 記錄一段用眼時間，跨過午夜時拆到各自的日期。 */
    static void addWork(Context ctx, long from, long to) {
        if (from <= 0 || to <= from) return;
        SharedPreferences sp = Prefs.sp(ctx);
        SharedPreferences.Editor e = sp.edit();
        long t = from;
        while (t < to) {
            long segEnd = Math.min(to, nextDayStart(t));
            String key = "w_" + dayKey(t);
            e.putLong(key, sp.getLong(key, 0L) + (segEnd - t));
            t = segEnd;
        }
        e.apply();
    }

    static int breaks(Context ctx, long day) {
        return Prefs.sp(ctx).getInt("b_" + dayKey(day), 0);
    }

    /** 今天的用眼時間，包含正在進行中的這一輪。 */
    static long workToday(Context ctx, long now) {
        long total = Prefs.sp(ctx).getLong("w_" + dayKey(now), 0L);
        if (Prefs.enabled(ctx) && Prefs.phase(ctx) == Prefs.PHASE_WORK) {
            long from = Math.max(Prefs.phaseStart(ctx), startOfDay(now));
            if (now > from) total += now - from;
        }
        return total;
    }

    /** 最近 n 天（舊 → 今天）的休息次數。 */
    static int[] recentBreaks(Context ctx, long now, int n) {
        int[] out = new int[n];
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(now));
        c.add(Calendar.DAY_OF_MONTH, -(n - 1));
        for (int i = 0; i < n; i++) {
            out[i] = breaks(ctx, c.getTimeInMillis());
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return out;
    }

    static String[] recentLabels(long now, int n) {
        String[] out = new String[n];
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(now));
        c.add(Calendar.DAY_OF_MONTH, -(n - 1));
        for (int i = 0; i < n; i++) {
            out[i] = WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1];
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return out;
    }

    private static void prune(Context ctx, long now) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(now));
        c.add(Calendar.DAY_OF_MONTH, -KEEP_DAYS);
        String oldest = dayKey(c.getTimeInMillis());
        SharedPreferences sp = Prefs.sp(ctx);
        List<String> stale = new ArrayList<>();
        for (Map.Entry<String, ?> entry : sp.getAll().entrySet()) {
            String k = entry.getKey();
            if ((k.startsWith("b_") || k.startsWith("w_")) && k.length() == 10
                    && k.substring(2).compareTo(oldest) < 0) {
                stale.add(k);
            }
        }
        if (stale.isEmpty()) return;
        SharedPreferences.Editor e = sp.edit();
        for (String k : stale) e.remove(k);
        e.apply();
    }
}
