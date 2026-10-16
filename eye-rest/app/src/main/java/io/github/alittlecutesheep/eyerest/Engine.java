package io.github.alittlecutesheep.eyerest;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;

/**
 * 計時狀態機：WORK（用眼中）→ REST（休息中）→ WORK …，勿擾時段內停在 QUIET。
 * 每個階段結束時間都用精確鬧鐘喚醒，就算 app 被系統收掉也能準時跳出。
 * 所有方法都在主執行緒呼叫。
 */
final class Engine {
    static final String ACTION_PHASE_END = "io.github.alittlecutesheep.eyerest.PHASE_END";

    /** 鬧鐘晚到超過這個時間，就不再補震動（例如關機後重新開機）。 */
    private static final long LATE_LIMIT_MS = 60_000L;

    /** 目前可見的 app 畫面數量；有畫面在前景時可以直接開啟休息畫面。 */
    static int visibleScreens = 0;

    private Engine() {}

    static long now() {
        return System.currentTimeMillis();
    }

    // ---- 使用者操作 ----

    static void enable(Context c) {
        Prefs.setEnabled(c, true);
        Notifier.cancelOff(c);
        startWork(c, now());
        changed(c);
        ensureService(c);
    }

    static void disable(Context c, boolean showOffNotice) {
        long now = now();
        if (Prefs.enabled(c) && Prefs.phase(c) == Prefs.PHASE_WORK) {
            Stats.addWork(c, Prefs.phaseStart(c), now);
        }
        Prefs.setEnabled(c, false);
        cancelAlarm(c);
        Notifier.cancelBreak(c);
        c.stopService(new Intent(c, EyeRestService.class));
        Notifier.cancelStatus(c);
        if (showOffNotice) Notifier.postOff(c);
        changed(c);
    }

    /** 重新開始這一輪用眼計時。 */
    static void restartCycle(Context c) {
        if (!Prefs.enabled(c)) return;
        long now = now();
        if (Prefs.phase(c) == Prefs.PHASE_WORK) Stats.addWork(c, Prefs.phaseStart(c), now);
        Notifier.cancelBreak(c);
        startWork(c, now);
        changed(c);
    }

    /** 不等倒數，現在就休息。 */
    static void restNow(Context c) {
        if (!Prefs.enabled(c) || Prefs.phase(c) != Prefs.PHASE_WORK) return;
        long now = now();
        Stats.addWork(c, Prefs.phaseStart(c), now);
        startRest(c, now);
        changed(c);
    }

    /** 間隔時間改了：用新的間隔重新計時。 */
    static void onIntervalChanged(Context c) {
        if (Prefs.enabled(c) && Prefs.phase(c) == Prefs.PHASE_WORK) restartCycle(c);
        else changed(c);
    }

    /** 勿擾時段改了：保留目前這一輪的起點，重新算結束時間。 */
    static void onQuietChanged(Context c) {
        if (!Prefs.enabled(c)) {
            changed(c);
            return;
        }
        long now = now();
        int phase = Prefs.phase(c);
        if (phase == Prefs.PHASE_WORK) {
            long start = Prefs.phaseStart(c);
            if (Quiet.active(c, now)) {
                Stats.addWork(c, start, now);
                enterQuiet(c, now);
            } else {
                long end = Math.max(now + 3_000L, start + Prefs.intervalMin(c) * 60_000L);
                long quietAt = Quiet.nextStart(c, now);
                if (quietAt > 0 && quietAt < end) end = quietAt;
                Prefs.setPhase(c, Prefs.PHASE_WORK, start, end);
                scheduleAlarm(c, end);
            }
        } else if (phase == Prefs.PHASE_QUIET) {
            if (Quiet.active(c, now)) enterQuiet(c, now);
            else startWork(c, now);
        }
        changed(c);
    }

    // ---- 鬧鐘與同步 ----

    static void onAlarm(Context c) {
        if (!Prefs.enabled(c)) {
            cancelAlarm(c);
            return;
        }
        long now = now();
        long end = Prefs.phaseEnd(c);
        if (now < end - 1_000L) {
            // 已經被其他路徑處理過（例如休息畫面自己倒數完），重新排程即可
            scheduleAlarm(c, end);
            return;
        }
        advance(c, now);
        ensureService(c);
    }

    /** App 開啟或服務重啟時呼叫：補上錯過的階段切換，並確保鬧鐘還在。 */
    static void sync(Context c) {
        if (!Prefs.enabled(c)) return;
        long now = now();
        long end = Prefs.phaseEnd(c);
        if (end <= 0) {
            startWork(c, now);
            changed(c);
        } else if (now >= end + 2_000L) {
            advance(c, now);
        } else {
            scheduleAlarm(c, end);
        }
    }

    /** 開機後重新開始計時（關機期間經過多久無從得知）。 */
    static void onBoot(Context c) {
        if (!Prefs.enabled(c)) return;
        Notifier.cancelBreak(c);
        startWork(c, now());
        changed(c);
        ensureService(c);
    }

    /** 休息畫面倒數歸零時呼叫，讓震動跟畫面同步。 */
    static void completeRestIfDue(Context c) {
        if (!Prefs.enabled(c) || Prefs.phase(c) != Prefs.PHASE_REST) return;
        long now = now();
        if (now < Prefs.phaseEnd(c) - 500L) return;
        finishRest(c, now);
        changed(c);
    }

    private static void advance(Context c, long now) {
        switch (Prefs.phase(c)) {
            case Prefs.PHASE_WORK:
                Stats.addWork(c, Prefs.phaseStart(c), now);
                if (Quiet.active(c, now)) enterQuiet(c, now);
                else startRest(c, now);
                break;
            case Prefs.PHASE_REST:
                finishRest(c, now);
                break;
            default:
                startWork(c, now);
                break;
        }
        changed(c);
    }

    // ---- 階段切換 ----

    private static void startWork(Context c, long now) {
        if (Quiet.active(c, now)) {
            enterQuiet(c, now);
            return;
        }
        long end = now + Prefs.intervalMin(c) * 60_000L;
        long quietAt = Quiet.nextStart(c, now);
        if (quietAt > 0 && quietAt < end) end = quietAt;
        Prefs.setPhase(c, Prefs.PHASE_WORK, now, end);
        scheduleAlarm(c, end);
    }

    private static void enterQuiet(Context c, long now) {
        long end = Quiet.endAfter(c, now);
        Prefs.setPhase(c, Prefs.PHASE_QUIET, now, end);
        scheduleAlarm(c, end);
    }

    private static void startRest(Context c, long now) {
        long end = now + Prefs.restSec(c) * 1000L;
        Prefs.setPhase(c, Prefs.PHASE_REST, now, end);
        scheduleAlarm(c, end);
        if (Prefs.vibrateStart(c)) Vibes.tick(c);
        showBreak(c);
    }

    private static void finishRest(Context c, long now) {
        boolean late = now - Prefs.phaseEnd(c) > LATE_LIMIT_MS;
        Stats.addBreak(c, now);
        Notifier.cancelBreak(c);
        if (!late && Prefs.vibrateEnd(c)) Vibes.end(c, Prefs.vibeStrength(c));
        startWork(c, now);
    }

    /**
     * 跳出休息畫面。
     * 鎖定或螢幕關閉時用全螢幕通知（會亮螢幕並蓋在鎖定畫面上）；
     * 使用中則直接開啟畫面（需要「顯示在其他應用程式上層」權限），不行就退回通知。
     */
    private static void showBreak(Context c) {
        KeyguardManager km = c.getSystemService(KeyguardManager.class);
        PowerManager pm = c.getSystemService(PowerManager.class);
        boolean locked = (km != null && km.isKeyguardLocked()) || (pm != null && !pm.isInteractive());
        if (locked) {
            Notifier.postBreak(c);
            return;
        }
        boolean launched = false;
        if (Settings.canDrawOverlays(c) || visibleScreens > 0) {
            try {
                c.startActivity(BreakActivity.intent(c));
                launched = true;
            } catch (RuntimeException ignored) {
                launched = false;
            }
        }
        if (!launched) {
            Notifier.postBreak(c);
            return;
        }
        // 部分廠牌會默默擋下背景開啟畫面：稍後確認畫面真的出現了，沒有就改用通知
        final Context app = c.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!BreakActivity.showing && Prefs.enabled(app) && Prefs.phase(app) == Prefs.PHASE_REST) {
                Notifier.postBreak(app);
            }
        }, 1_500L);
    }

    // ---- 系統整合 ----

    /** 狀態有變：更新常駐通知與快捷設定方塊。 */
    static void changed(Context c) {
        if (Prefs.enabled(c)) Notifier.updateStatus(c);
        RestTileService.requestUpdate(c);
    }

    static void ensureService(Context c) {
        if (!Prefs.enabled(c)) return;
        try {
            c.startForegroundService(new Intent(c, EyeRestService.class));
        } catch (RuntimeException e) {
            // 系統不允許在背景啟動前景服務時，鬧鐘仍會照常喚醒，只是通知改為一般通知
            Notifier.updateStatus(c);
        }
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_PHASE_END);
        return PendingIntent.getBroadcast(c, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static boolean canExact(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    private static void scheduleAlarm(Context c, long at) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = alarmIntent(c);
        try {
            if (canExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    private static void cancelAlarm(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(alarmIntent(c));
    }
}
