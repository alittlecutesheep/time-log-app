package io.github.alittlecutesheep.eyerest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;

/** 通知：常駐狀態（可直接開關）、休息提醒（全螢幕）、已關閉提示。 */
final class Notifier {
    static final String CH_STATUS = "status";
    static final String CH_BREAK = "break";

    static final int ID_STATUS = 1;
    static final int ID_BREAK = 2;
    static final int ID_OFF = 3;

    private Notifier() {}

    private static NotificationManager nm(Context c) {
        return c.getSystemService(NotificationManager.class);
    }

    static void ensureChannels(Context c) {
        NotificationManager nm = nm(c);
        if (nm == null) return;

        NotificationChannel status = new NotificationChannel(
                CH_STATUS, "運作狀態", NotificationManager.IMPORTANCE_LOW);
        status.setDescription("顯示下次休息時間，可以直接在通知上開關提醒");
        status.setShowBadge(false);

        NotificationChannel rest = new NotificationChannel(
                CH_BREAK, "休息提醒", NotificationManager.IMPORTANCE_HIGH);
        rest.setDescription("時間到時跳出休息畫面");
        rest.setSound(null, null);
        rest.enableVibration(false); // 震動由 app 自己控制
        rest.setShowBadge(false);
        rest.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        nm.createNotificationChannel(status);
        nm.createNotificationChannel(rest);
    }

    static Notification buildStatus(Context c) {
        ensureChannels(c);
        int phase = Prefs.phase(c);
        long end = Prefs.phaseEnd(c);
        String time = Fmt.clock(end);

        Notification.Builder b = new Notification.Builder(c, CH_STATUS)
                .setSmallIcon(R.drawable.ic_stat_eye)
                .setColor(Ui.RED)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(openMain(c));
        if (Build.VERSION.SDK_INT >= 31) {
            b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        if (!Prefs.enabled(c)) {
            b.setContentTitle("護眼提醒已關閉").setShowWhen(false);
            return b.build();
        }

        String cycle = Fmt.cycle(c);
        if (Engine.isPaused(c)) {
            long left = Math.max(0L, Prefs.workTarget(c) - Prefs.workUsed(c));
            b.setContentTitle("計時暫停 · 還剩 " + Fmt.countdown(left))
                    .setContentText("螢幕使用時間模式：解鎖後繼續計時")
                    .setShowWhen(false)
                    .addAction(action(c, "重新計時", ActionReceiver.ACTION_RESET, 11))
                    .addAction(action(c, "關閉提醒", ActionReceiver.ACTION_DISABLE, 12));
            return b.build();
        }
        switch (phase) {
            case Prefs.PHASE_REST:
                b.setContentTitle("休息中 · " + time + " 結束")
                        .setContentText(Prefs.displayTitle(c))
                        .setContentIntent(openBreak(c));
                countdown(b, end);
                break;
            case Prefs.PHASE_QUIET:
                b.setContentTitle("勿擾時段中")
                        .setContentText(time + " 恢復提醒")
                        .setShowWhen(false);
                break;
            default:
                boolean toQuiet = Quiet.active(c, end);
                String next = Engine.screenMode(c) ? "持續使用的話 " + time + " 休息" : "下次休息 " + time;
                b.setContentTitle(toQuiet ? time + " 進入勿擾時段" : next)
                        .setContentText(cycle);
                countdown(b, end);
                b.addAction(action(c, "重新計時", ActionReceiver.ACTION_RESET, 11));
                break;
        }
        b.addAction(action(c, "關閉提醒", ActionReceiver.ACTION_DISABLE, 12));
        return b.build();
    }

    private static void countdown(Notification.Builder b, long end) {
        b.setWhen(end)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true);
    }

    static void updateStatus(Context c) {
        NotificationManager nm = nm(c);
        if (nm == null || !Prefs.enabled(c)) return;
        nm.notify(ID_STATUS, buildStatus(c));
    }

    static void cancelStatus(Context c) {
        NotificationManager nm = nm(c);
        if (nm != null) nm.cancel(ID_STATUS);
    }

    /** 休息提醒：鎖定時以全螢幕顯示；使用中則以浮動通知出現，點了就開休息畫面。 */
    static void postBreak(Context c) {
        ensureChannels(c);
        NotificationManager nm = nm(c);
        if (nm == null) return;
        PendingIntent full = openBreak(c);
        String message = Prefs.message(c).trim();
        Notification.Builder b = new Notification.Builder(c, CH_BREAK)
                .setSmallIcon(R.drawable.ic_stat_eye)
                .setColor(Ui.RED)
                .setContentTitle(Prefs.displayTitle(c))
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setFullScreenIntent(full, true)
                .setContentIntent(full)
                .setAutoCancel(true)
                .setTimeoutAfter(Prefs.restSec(c) * 1000L + 5_000L);
        if (!message.isEmpty()) b.setContentText(message);
        nm.notify(ID_BREAK, b.build());
    }

    static void cancelBreak(Context c) {
        NotificationManager nm = nm(c);
        if (nm != null) nm.cancel(ID_BREAK);
    }

    /** 從通知關掉後留一則提示，讓使用者也能從通知欄再打開。 */
    static void postOff(Context c) {
        ensureChannels(c);
        NotificationManager nm = nm(c);
        if (nm == null) return;
        Notification n = new Notification.Builder(c, CH_STATUS)
                .setSmallIcon(R.drawable.ic_stat_eye)
                .setColor(Ui.RED)
                .setContentTitle("護眼提醒已關閉")
                .setContentText("需要時按「開啟」恢復提醒")
                .setShowWhen(false)
                .setAutoCancel(true)
                .setContentIntent(openMain(c))
                .addAction(action(c, "開啟", ActionReceiver.ACTION_ENABLE, 13))
                .build();
        nm.notify(ID_OFF, n);
    }

    static void cancelOff(Context c) {
        NotificationManager nm = nm(c);
        if (nm != null) nm.cancel(ID_OFF);
    }

    private static Notification.Action action(Context c, String title, String action, int requestCode) {
        Intent i = new Intent(c, ActionReceiver.class).setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(c, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(
                Icon.createWithResource(c, R.drawable.ic_stat_eye), title, pi).build();
    }

    private static PendingIntent openMain(Context c) {
        Intent i = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, 21, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent openBreak(Context c) {
        return PendingIntent.getActivity(c, 22, BreakActivity.intent(c),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
