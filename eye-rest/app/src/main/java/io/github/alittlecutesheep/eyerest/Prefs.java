package io.github.alittlecutesheep.eyerest;

import android.content.Context;
import android.content.SharedPreferences;

/** 使用者設定與計時狀態，全部存在同一個 SharedPreferences。 */
final class Prefs {
    static final int PHASE_WORK = 0;
    static final int PHASE_REST = 1;
    static final int PHASE_QUIET = 2;

    /** 計時方式：實際經過時間，或只算螢幕開著且已解鎖的時間。 */
    static final int COUNT_CLOCK = 0;
    static final int COUNT_SCREEN = 1;

    static final String DEFAULT_TITLE = "讓眼睛休息一下";
    static final String DEFAULT_MESSAGE = "離開螢幕，看看遠方，慢慢眨眼。";

    private Prefs() {}

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("eye_rest", Context.MODE_PRIVATE);
    }

    // ---- 計時設定 ----

    static int intervalMin(Context c) {
        return sp(c).getInt("interval_min", 20);
    }

    static void setIntervalMin(Context c, int v) {
        sp(c).edit().putInt("interval_min", v).apply();
    }

    static int restSec(Context c) {
        return sp(c).getInt("rest_sec", 20);
    }

    static void setRestSec(Context c, int v) {
        sp(c).edit().putInt("rest_sec", v).apply();
    }

    static int countMode(Context c) {
        return sp(c).getInt("count_mode", COUNT_CLOCK);
    }

    static void setCountMode(Context c, int v) {
        sp(c).edit().putInt("count_mode", v).apply();
    }

    // ---- 提醒文字 ----

    static String title(Context c) {
        return sp(c).getString("title", DEFAULT_TITLE);
    }

    static void setTitle(Context c, String v) {
        sp(c).edit().putString("title", v).apply();
    }

    static String message(Context c) {
        return sp(c).getString("message", DEFAULT_MESSAGE);
    }

    static void setMessage(Context c, String v) {
        sp(c).edit().putString("message", v).apply();
    }

    /** 標題與內文都清空時，退回預設標題，避免跳出一個空白畫面。 */
    static String displayTitle(Context c) {
        String t = title(c).trim();
        if (t.isEmpty() && message(c).trim().isEmpty()) return DEFAULT_TITLE;
        return t;
    }

    // ---- 震動 ----

    static boolean vibrateEnd(Context c) {
        return sp(c).getBoolean("vibrate_end", true);
    }

    static void setVibrateEnd(Context c, boolean v) {
        sp(c).edit().putBoolean("vibrate_end", v).apply();
    }

    /** 0 = 輕、1 = 標準、2 = 強 */
    static int vibeStrength(Context c) {
        return sp(c).getInt("vibe_strength", 1);
    }

    static void setVibeStrength(Context c, int v) {
        sp(c).edit().putInt("vibe_strength", v).apply();
    }

    static boolean vibrateStart(Context c) {
        return sp(c).getBoolean("vibrate_start", true);
    }

    static void setVibrateStart(Context c, boolean v) {
        sp(c).edit().putBoolean("vibrate_start", v).apply();
    }

    // ---- 勿擾時段（以一天中的第幾分鐘表示） ----

    static boolean quietEnabled(Context c) {
        return sp(c).getBoolean("quiet_enabled", false);
    }

    static void setQuietEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean("quiet_enabled", v).apply();
    }

    static int quietStart(Context c) {
        return sp(c).getInt("quiet_start", 23 * 60);
    }

    static void setQuietStart(Context c, int v) {
        sp(c).edit().putInt("quiet_start", v).apply();
    }

    static int quietEnd(Context c) {
        return sp(c).getInt("quiet_end", 7 * 60);
    }

    static void setQuietEnd(Context c, int v) {
        sp(c).edit().putInt("quiet_end", v).apply();
    }

    // ---- 執行狀態 ----

    static boolean enabled(Context c) {
        return sp(c).getBoolean("enabled", false);
    }

    static void setEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean("enabled", v).commit();
    }

    static int phase(Context c) {
        return sp(c).getInt("phase", PHASE_WORK);
    }

    static long phaseStart(Context c) {
        return sp(c).getLong("phase_start", 0L);
    }

    static long phaseEnd(Context c) {
        return sp(c).getLong("phase_end", 0L);
    }

    static void setPhase(Context c, int phase, long start, long end) {
        sp(c).edit()
                .putInt("phase", phase)
                .putLong("phase_start", start)
                .putLong("phase_end", end)
                .commit();
    }

    // ---- 這一輪用眼計時（WORK 階段） ----

    /** 這一輪要累積多久才休息（毫秒）。 */
    static long workTarget(Context c) {
        return sp(c).getLong("work_target", 0L);
    }

    /** 這一輪在目前這段計時之前已經累積的時間（毫秒）。 */
    static long workUsed(Context c) {
        return sp(c).getLong("work_used", 0L);
    }

    /** 目前這段計時從什麼時候開始（暫停時沒有意義）。 */
    static long segmentStart(Context c) {
        return sp(c).getLong("seg_start", 0L);
    }

    /** 螢幕使用時間模式下，螢幕關閉或鎖定時暫停。 */
    static boolean workPaused(Context c) {
        return sp(c).getBoolean("work_paused", false);
    }

    static void setWork(Context c, long target, long used, long segmentStart, boolean paused) {
        sp(c).edit()
                .putLong("work_target", target)
                .putLong("work_used", used)
                .putLong("seg_start", segmentStart)
                .putBoolean("work_paused", paused)
                .commit();
    }

    static boolean askedNotificationPermission(Context c) {
        return sp(c).getBoolean("asked_notif", false);
    }

    static void setAskedNotificationPermission(Context c) {
        sp(c).edit().putBoolean("asked_notif", true).apply();
    }
}
