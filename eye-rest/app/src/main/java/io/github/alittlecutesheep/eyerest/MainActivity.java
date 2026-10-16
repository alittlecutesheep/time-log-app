package io.github.alittlecutesheep.eyerest;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 首頁：倒數圓環、開關、今日統計與最近 7 天。 */
public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            render();
            handler.postDelayed(this, 500);
        }
    };

    private View statusDot;
    private TextView statusText;
    private TextView statusRight;
    private LinearLayout warnCard;
    private TextView warnText;
    private DotRingView ring;
    private TextView ringLabel;
    private DotMatrixView countdown;
    private TextView ringSub;
    private ToggleView toggle;
    private TextView toggleSub;
    private TextView resetButton;
    private TextView restButton;
    private DotMatrixView breaksValue;
    private DotMatrixView eyeValue;
    private DotBarsView bars;
    private ObjectAnimator pulse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notifier.ensureChannels(this);
        Ui.edgeToEdge(this);
        setContentView(buildLayout());
        askNotificationPermissionOnce();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Engine.visibleScreens++;
        Engine.sync(this);
        Engine.ensureService(this);
        Engine.changed(this);
        updateWarning();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Engine.visibleScreens--;
        handler.removeCallbacks(tick);
    }

    // ---- 版面 ----

    private View buildLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = Ui.column(this);
        int pad = Ui.dp(this, 20);
        col.setPadding(pad, Ui.dp(this, 14), pad, Ui.dp(this, 32));
        scroll.addView(col, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        Ui.applyInsets(root, false, false);

        // 標頭：點陣字 logo + 設定按鈕
        LinearLayout header = Ui.row(this);
        DotMatrixView logo = new DotMatrixView(this, 3.4f);
        logo.setAlignLeft(true);
        logo.setText("EYE REST");
        header.addView(logo, Ui.weight(1f));
        DotIconView settings = Ui.iconButton(this, DotIconView.SETTINGS);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        header.addView(settings, Ui.wrap());
        col.addView(header, Ui.matchWrap());

        // 狀態列
        LinearLayout status = Ui.row(this);
        status.setPadding(0, Ui.dp(this, 10), 0, 0);
        statusDot = Ui.dot(this, Ui.TEXT_3, 8);
        status.addView(statusDot);
        statusText = Ui.label(this, "OFF");
        statusText.setTextColor(Ui.TEXT);
        statusText.setPadding(Ui.dp(this, 10), 0, 0, 0);
        status.addView(statusText, Ui.weight(1f));
        statusRight = Ui.label(this, "");
        status.addView(statusRight, Ui.wrap());
        col.addView(status, Ui.matchWrap());
        pulse = ObjectAnimator.ofFloat(statusDot, "alpha", 1f, 0.2f);
        pulse.setDuration(900);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);

        // 權限提醒
        warnCard = Ui.card(this);
        warnCard.setBackground(Ui.rounded(Ui.BG, Ui.RED, Ui.dpf(this, 20), Ui.dp(this, 1)));
        warnCard.setOrientation(LinearLayout.HORIZONTAL);
        warnCard.setGravity(Gravity.CENTER_VERTICAL);
        warnCard.addView(Ui.dot(this, Ui.RED, 8));
        warnText = Ui.text(this, "", 13, Ui.TEXT);
        warnText.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 8), 0);
        warnCard.addView(warnText, Ui.weight(1f));
        warnCard.addView(Ui.label(this, "FIX →"), Ui.wrap());
        warnCard.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        LinearLayout.LayoutParams warnLp = Ui.matchWrap();
        warnLp.topMargin = Ui.dp(this, 16);
        col.addView(warnCard, warnLp);

        // 倒數圓環
        FrameLayout hero = new FrameLayout(this);
        ring = new DotRingView(this, 320);
        hero.addView(ring, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        LinearLayout center = Ui.column(this);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        ringLabel = Ui.label(this, "NEXT BREAK");
        center.addView(ringLabel, Ui.wrap());
        countdown = new DotMatrixView(this, 7.2f);
        countdown.setPanel(true);
        LinearLayout.LayoutParams cdLp = Ui.wrap();
        cdLp.topMargin = Ui.dp(this, 14);
        cdLp.bottomMargin = Ui.dp(this, 14);
        center.addView(countdown, cdLp);
        ringSub = Ui.text(this, "", 14, Ui.TEXT_2);
        ringSub.setGravity(Gravity.CENTER);
        center.addView(ringSub, Ui.wrap());
        hero.addView(center, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        hero.setOnClickListener(v -> {
            if (Prefs.enabled(this) && Prefs.phase(this) == Prefs.PHASE_REST) {
                startActivity(BreakActivity.intent(this));
            }
        });
        LinearLayout.LayoutParams heroLp = Ui.matchWrap();
        heroLp.topMargin = Ui.dp(this, 22);
        col.addView(hero, heroLp);

        // 開關卡片
        LinearLayout toggleCard = Ui.card(this);
        toggleCard.setOrientation(LinearLayout.HORIZONTAL);
        toggleCard.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout toggleText = Ui.column(this);
        toggleText.addView(Ui.heading(this, "護眼提醒", 18), Ui.wrap());
        toggleSub = Ui.text(this, "", 13, Ui.TEXT_2);
        toggleSub.setPadding(0, Ui.dp(this, 4), 0, 0);
        toggleText.addView(toggleSub, Ui.wrap());
        toggleCard.addView(toggleText, Ui.weight(1f));
        toggle = new ToggleView(this);
        toggle.setChecked(Prefs.enabled(this), false);
        toggle.setListener(this::setEnabledFromUi);
        toggleCard.addView(toggle, Ui.wrap());
        toggleCard.setOnClickListener(v -> toggle.performClick());
        LinearLayout.LayoutParams toggleLp = Ui.matchWrap();
        toggleLp.topMargin = Ui.dp(this, 24);
        col.addView(toggleCard, toggleLp);

        // 重新計時 / 立即休息
        LinearLayout actions = Ui.row(this);
        resetButton = Ui.pill(this, "重新計時", false);
        resetButton.setOnClickListener(v -> {
            Engine.restartCycle(this);
            render();
        });
        restButton = Ui.pill(this, "立即休息", false);
        restButton.setOnClickListener(v -> {
            Engine.restNow(this);
            render();
        });
        actions.addView(resetButton, Ui.weight(1f));
        LinearLayout.LayoutParams restLp = Ui.weight(1f);
        restLp.leftMargin = Ui.dp(this, 10);
        actions.addView(restButton, restLp);
        LinearLayout.LayoutParams actionsLp = Ui.matchWrap();
        actionsLp.topMargin = Ui.dp(this, 12);
        col.addView(actions, actionsLp);

        // 今日統計
        col.addView(sectionTitle("TODAY", "今日"), sectionLp());
        LinearLayout statsRow = Ui.row(this);
        breaksValue = new DotMatrixView(this, 5f);
        eyeValue = new DotMatrixView(this, 5f);
        statsRow.addView(statCard("BREAKS", breaksValue, "完成休息次數"), Ui.weight(1f));
        LinearLayout.LayoutParams eyeLp = Ui.weight(1f);
        eyeLp.leftMargin = Ui.dp(this, 10);
        statsRow.addView(statCard("SCREEN TIME", eyeValue, "用眼時間（時:分）"), eyeLp);
        col.addView(statsRow, Ui.matchWrap());

        // 最近 7 天
        LinearLayout weekCard = Ui.card(this);
        LinearLayout weekHead = Ui.row(this);
        weekHead.addView(Ui.label(this, "LAST 7 DAYS"), Ui.weight(1f));
        weekHead.addView(Ui.label(this, "BREAKS"), Ui.wrap());
        weekCard.addView(weekHead, Ui.matchWrap());
        bars = new DotBarsView(this);
        LinearLayout.LayoutParams barsLp = Ui.matchWrap();
        barsLp.topMargin = Ui.dp(this, 6);
        weekCard.addView(bars, barsLp);
        LinearLayout.LayoutParams weekLp = Ui.matchWrap();
        weekLp.topMargin = Ui.dp(this, 10);
        col.addView(weekCard, weekLp);

        TextView footer = Ui.label(this, "EYE REST · LOOK FAR · BLINK SLOW");
        footer.setTextColor(Ui.TEXT_3);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = Ui.matchWrap();
        footerLp.topMargin = Ui.dp(this, 28);
        col.addView(footer, footerLp);
        return root;
    }

    private LinearLayout sectionTitle(String en, String zh) {
        LinearLayout row = Ui.row(this);
        TextView label = Ui.label(this, en);
        label.setTextColor(Ui.TEXT);
        row.addView(label, Ui.wrap());
        TextView sub = Ui.text(this, zh, 12, Ui.TEXT_3);
        sub.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 12), 0);
        row.addView(sub, Ui.wrap());
        row.addView(new DotDividerView(this), Ui.weight(1f));
        return row;
    }

    private LinearLayout.LayoutParams sectionLp() {
        LinearLayout.LayoutParams lp = Ui.matchWrap();
        lp.topMargin = Ui.dp(this, 32);
        lp.bottomMargin = Ui.dp(this, 12);
        return lp;
    }

    private LinearLayout statCard(String label, DotMatrixView value, String caption) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.label(this, label), Ui.wrap());
        value.setAlignLeft(true);
        LinearLayout.LayoutParams vLp = Ui.matchWrap();
        vLp.topMargin = Ui.dp(this, 14);
        vLp.bottomMargin = Ui.dp(this, 12);
        card.addView(value, vLp);
        card.addView(Ui.text(this, caption, 12, Ui.TEXT_3), Ui.wrap());
        return card;
    }

    // ---- 狀態更新 ----

    private void setEnabledFromUi(boolean on) {
        if (on) Engine.enable(this);
        else Engine.disable(this, false);
        updateWarning();
        render();
    }

    private void render() {
        long now = System.currentTimeMillis();
        boolean on = Prefs.enabled(this);
        int phase = Prefs.phase(this);
        long start = Prefs.phaseStart(this);
        long end = Prefs.phaseEnd(this);
        int interval = Prefs.intervalMin(this);

        toggle.setChecked(on, false);
        toggleSub.setText("每 " + interval + " 分鐘 · 休息 " + Fmt.restLabel(Prefs.restSec(this)));
        Ui.setEnabledLook(resetButton, on);
        Ui.setEnabledLook(restButton, on && phase == Prefs.PHASE_WORK);

        float span = Math.max(1L, end - start);
        if (!on) {
            setStatus("OFF", "已關閉", Ui.TEXT_3, false);
            ring.setProgress(0f);
            ring.setShowHead(false);
            ringLabel.setText("STANDBY");
            String idle = Fmt.countdown(interval * 60_000L);
            countdown.setPitchDp(ring.fitPitchDp(idle, idle.length() > 5 ? 5.4f : 7.2f));
            countdown.setColors(0xFF3C3C3C, Ui.DOT_OFF);
            countdown.setText(idle);
            ringSub.setText("打開下方開關開始計時");
        } else if (phase == Prefs.PHASE_REST) {
            setStatus("RESTING", "休息中", Ui.RED, true);
            ring.setShowHead(true);
            ring.setProgress((end - now) / span);
            ringLabel.setText("REST");
            setCountdown(Fmt.countdown(end - now));
            ringSub.setText("點這裡回到休息畫面");
        } else if (phase == Prefs.PHASE_QUIET) {
            setStatus("QUIET", "勿擾時段", Ui.TEXT_2, false);
            ring.setShowHead(false);
            ring.setProgress((now - start) / span);
            ringLabel.setText("RESUME AT");
            setCountdown(Fmt.clock(end));
            ringSub.setText("勿擾時段中，不會提醒");
        } else {
            setStatus("RUNNING", "計時中", Ui.RED, true);
            ring.setShowHead(true);
            ring.setProgress((now - start) / span);
            boolean toQuiet = Quiet.active(this, end);
            ringLabel.setText(toQuiet ? "QUIET IN" : "NEXT BREAK");
            setCountdown(Fmt.countdown(end - now));
            ringSub.setText(Fmt.clock(end) + (toQuiet ? " 進入勿擾時段" : " 休息"));
        }

        breaksValue.setText(Fmt.twoDigits(Stats.breaks(this, now)));
        eyeValue.setText(Fmt.hoursMinutes(Stats.workToday(this, now)));
        bars.setData(Stats.recentBreaks(this, now, 7), Stats.recentLabels(now, 7));
    }

    private void setCountdown(String text) {
        countdown.setPitchDp(ring.fitPitchDp(text, text.length() > 5 ? 5.4f : 7.2f));
        countdown.setColors(Ui.TEXT, Ui.DOT_OFF);
        countdown.setText(text);
    }

    private void setStatus(String en, String zh, int dotColor, boolean pulsing) {
        statusText.setText(en);
        statusRight.setText(zh);
        statusDot.setBackground(Ui.oval(dotColor, 0, 0));
        if (pulsing && !pulse.isStarted()) {
            pulse.start();
        } else if (!pulsing && pulse.isStarted()) {
            pulse.cancel();
            statusDot.setAlpha(1f);
        }
    }

    private void updateWarning() {
        boolean overlay = Settings.canDrawOverlays(this);
        NotificationManager nm = getSystemService(NotificationManager.class);
        boolean notif = nm == null || nm.areNotificationsEnabled();
        if (overlay && notif) {
            warnCard.setVisibility(View.GONE);
            return;
        }
        warnCard.setVisibility(View.VISIBLE);
        if (!overlay) warnText.setText("尚未允許「顯示在其他應用程式上層」，使用其他 app 時休息畫面無法自動跳出");
        else warnText.setText("通知已關閉，鎖定畫面時無法跳出休息畫面，也看不到通知欄開關");
    }

    private void askNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < 33 || Prefs.askedNotificationPermission(this)) return;
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        Prefs.setAskedNotificationPermission(this);
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateWarning();
        Engine.changed(this);
    }
}
