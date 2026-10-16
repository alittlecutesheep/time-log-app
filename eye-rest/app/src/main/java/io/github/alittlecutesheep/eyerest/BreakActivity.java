package io.github.alittlecutesheep.eyerest;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 全螢幕休息畫面：圓點倒數、自訂文字，時間到震動並自動關閉。 */
public class BreakActivity extends Activity {
    static final String EXTRA_PREVIEW = "preview";
    private static final long PREVIEW_MS = 6_000L;
    private static final long CLOSE_DELAY_MS = 1_800L;

    /** 給 Engine 判斷畫面有沒有真的出現。 */
    static boolean showing;

    static Intent intent(Context c) {
        return new Intent(c, BreakActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    static Intent preview(Context c) {
        return intent(c).putExtra(EXTRA_PREVIEW, true);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
            if (!done) handler.postDelayed(this, 100);
        }
    };

    private boolean preview;
    private boolean done;
    private long restStart;
    private long restEnd;

    private View liveDot;
    private TextView topLabel;
    private TextView topRight;
    private DotRingView ring;
    private TextView ringLabel;
    private DotMatrixView countdown;
    private TextView title;
    private TextView message;
    private TextView footer;
    private TextView hint;
    private ObjectAnimator blink;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            w.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        Ui.edgeToEdge(this);
        setContentView(buildLayout());
        hideSystemBars();
        load(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        load(intent);
        if (showing) restartTick();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!preview && (!Prefs.enabled(this) || Prefs.phase(this) != Prefs.PHASE_REST)) {
            // 休息已經結束（例如從舊通知點進來）
            finish();
            return;
        }
        showing = true;
        if (!preview) Notifier.cancelBreak(this);
        hideSystemBars();
        restartTick();
    }

    @Override
    protected void onPause() {
        super.onPause();
        showing = false;
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (blink != null) blink.cancel();
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (preview || done) {
            super.onBackPressed();
            return;
        }
        // 休息中不提供跳過；提示一下時間到會自動關閉
        hint.animate().cancel();
        hint.setAlpha(1f);
        hint.animate().alpha(0f).setStartDelay(1600).setDuration(400).start();
    }

    private void load(Intent intent) {
        preview = intent != null && intent.getBooleanExtra(EXTRA_PREVIEW, false);
        done = false;
        if (preview) {
            restStart = System.currentTimeMillis();
            restEnd = restStart + PREVIEW_MS;
        } else {
            restStart = Prefs.phaseStart(this);
            restEnd = Prefs.phaseEnd(this);
        }
        String t = Prefs.displayTitle(this);
        String m = Prefs.message(this).trim();
        title.setText(t);
        title.setVisibility(t.isEmpty() ? View.GONE : View.VISIBLE);
        message.setText(m);
        message.setVisibility(m.isEmpty() ? View.GONE : View.VISIBLE);
        topLabel.setText("EYE BREAK");
        topRight.setText(preview ? "PREVIEW" : "BREAK " + Fmt.twoDigits(Stats.breaks(this, restStart) + 1));
        footer.setText(preview ? "預覽 · 按返回鍵關閉" : "時間到會震動，並自動開始下一輪計時");
        ringLabel.setText("REMAINING");
        ring.setLitColor(Ui.TEXT);
        ring.setShowHead(true);
        liveDot.setBackground(Ui.oval(Ui.RED, 0, 0));
        if (blink != null && !blink.isStarted()) blink.start();
    }

    private void restartTick() {
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void update() {
        if (done) return;
        long now = System.currentTimeMillis();
        if (!preview) {
            if (!Prefs.enabled(this)) {
                finish();
                return;
            }
            if (Prefs.phase(this) != Prefs.PHASE_REST || Prefs.phaseStart(this) != restStart) {
                showDone(); // 鬧鐘那邊已經處理好結束與震動
                return;
            }
        }
        long remaining = restEnd - now;
        if (remaining <= 0) {
            if (preview) {
                if (Prefs.vibrateEnd(this)) Vibes.end(this, Prefs.vibeStrength(this));
            } else {
                Engine.completeRestIfDue(this);
            }
            showDone();
            return;
        }
        float span = Math.max(1L, restEnd - restStart);
        ring.setProgress(remaining / span);
        String text = Fmt.countdown(remaining);
        countdown.setPitchDp(ring.fitPitchDp(text, text.length() > 5 ? 6f : 8.4f));
        countdown.setText(text);
    }

    private void showDone() {
        done = true;
        handler.removeCallbacks(tick);
        ring.setShowHead(false);
        ring.setProgress(1f);
        countdown.setPitchDp(ring.fitPitchDp("DONE", 8.4f));
        countdown.setText("DONE");
        ringLabel.setText("COMPLETE");
        footer.setText(preview ? "預覽結束" : "休息完成，繼續加油");
        if (blink != null) blink.cancel();
        liveDot.setAlpha(1f);
        liveDot.setBackground(Ui.oval(Ui.TEXT, 0, 0));
        handler.postDelayed(this::finish, CLOSE_DELAY_MS);
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    private View buildLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        Ui.applyInsets(root, false, true);

        LinearLayout col = Ui.column(this);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(this, 24);
        col.setPadding(pad, Ui.dp(this, 18), pad, Ui.dp(this, 24));
        root.addView(col, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // 頂部：閃爍紅點 + EYE BREAK
        LinearLayout top = Ui.row(this);
        liveDot = Ui.dot(this, Ui.RED, 8);
        top.addView(liveDot);
        topLabel = Ui.label(this, "EYE BREAK");
        topLabel.setTextColor(Ui.TEXT);
        topLabel.setPadding(Ui.dp(this, 10), 0, 0, 0);
        top.addView(topLabel, Ui.weight(1f));
        topRight = Ui.label(this, "");
        top.addView(topRight, Ui.wrap());
        col.addView(top, Ui.matchWrap());
        blink = ObjectAnimator.ofFloat(liveDot, "alpha", 1f, 0.15f);
        blink.setDuration(700);
        blink.setRepeatCount(ValueAnimator.INFINITE);
        blink.setRepeatMode(ValueAnimator.REVERSE);

        col.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        // 圓環倒數
        FrameLayout hero = new FrameLayout(this);
        ring = new DotRingView(this, 300);
        ring.setMaxHeightFraction(0.46f);
        hero.addView(ring, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        LinearLayout center = Ui.column(this);
        center.setGravity(Gravity.CENTER_HORIZONTAL);
        ringLabel = Ui.label(this, "REMAINING");
        center.addView(ringLabel, Ui.wrap());
        countdown = new DotMatrixView(this, 8.4f);
        countdown.setPanel(true);
        LinearLayout.LayoutParams cdLp = Ui.wrap();
        cdLp.topMargin = Ui.dp(this, 16);
        center.addView(countdown, cdLp);
        hero.addView(center, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        col.addView(hero, Ui.matchWrap());

        // 自訂文字
        title = Ui.heading(this, "", 27);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = Ui.matchWrap();
        titleLp.topMargin = Ui.dp(this, 36);
        col.addView(title, titleLp);
        message = Ui.text(this, "", 16, Ui.TEXT_2);
        message.setGravity(Gravity.CENTER);
        message.setLineSpacing(0f, 1.35f);
        LinearLayout.LayoutParams msgLp = Ui.matchWrap();
        msgLp.topMargin = Ui.dp(this, 12);
        col.addView(message, msgLp);

        col.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        hint = Ui.text(this, "休息中 · 時間到會自動關閉", 13, Ui.TEXT);
        hint.setGravity(Gravity.CENTER);
        hint.setAlpha(0f);
        col.addView(hint, Ui.matchWrap());

        LinearLayout bottom = Ui.column(this);
        bottom.setGravity(Gravity.CENTER_HORIZONTAL);
        bottom.setPadding(0, Ui.dp(this, 14), 0, 0);
        TextView mark = Ui.label(this, "LOOK FAR · BLINK SLOW");
        mark.setTextColor(Ui.TEXT_3);
        bottom.addView(mark, Ui.wrap());
        footer = Ui.text(this, "", 12, Ui.TEXT_3);
        footer.setPadding(0, Ui.dp(this, 6), 0, 0);
        bottom.addView(footer, Ui.wrap());
        col.addView(bottom, Ui.matchWrap());
        return root;
    }
}
