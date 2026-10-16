package io.github.alittlecutesheep.eyerest;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Switch;

/** 開關：關閉時是灰色描邊，開啟時是白色實心、黑色圓鈕。 */
public class ToggleView extends View {
    interface Listener {
        void onToggle(boolean checked);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final int w;
    private final int h;
    private boolean checked;
    private float pos;
    private ValueAnimator anim;
    private Listener listener;

    public ToggleView(Context c) {
        super(c);
        w = Ui.dp(c, 54);
        h = Ui.dp(c, 32);
        setClickable(true);
        setFocusable(true);
        setOnClickListener(v -> {
            setChecked(!checked, true);
            if (listener != null) listener.onToggle(checked);
        });
    }

    void setListener(Listener l) {
        listener = l;
    }

    boolean isChecked() {
        return checked;
    }

    void setChecked(boolean value, boolean animate) {
        if (value == checked) {
            // 動畫進行中就讓它跑完
            if ((anim == null || !anim.isRunning()) && pos != (value ? 1f : 0f)) {
                pos = value ? 1f : 0f;
                invalidate();
            }
            return;
        }
        checked = value;
        float target = value ? 1f : 0f;
        if (anim != null) anim.cancel();
        if (!animate) {
            pos = target;
            invalidate();
            return;
        }
        anim = ValueAnimator.ofFloat(pos, target);
        anim.setDuration(180);
        anim.addUpdateListener(a -> {
            pos = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(resolveSize(w, widthSpec), resolveSize(h, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float stroke = Ui.dpf(getContext(), 1.4f);
        float top = (getHeight() - h) / 2f;
        float left = (getWidth() - w) / 2f;
        track.set(left + stroke / 2f, top + stroke / 2f, left + w - stroke / 2f, top + h - stroke / 2f);
        float radius = h / 2f;

        // 外框
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setColor(blend(0xFF4A4A4A, Ui.TEXT, pos));
        canvas.drawRoundRect(track, radius, radius, paint);

        // 開啟時填滿白色
        if (pos > 0f) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Ui.TEXT);
            paint.setAlpha(Math.round(255 * pos));
            canvas.drawRoundRect(track, radius, radius, paint);
            paint.setAlpha(255);
        }

        // 圓鈕
        paint.setStyle(Paint.Style.FILL);
        float knobR = h / 2f - Ui.dpf(getContext(), 6);
        float cx = left + radius + (w - 2 * radius) * pos;
        paint.setColor(blend(0xFF6A6A6A, Ui.BG, pos));
        canvas.drawCircle(cx, top + h / 2f, knobR, paint);
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Switch.class.getName());
        info.setCheckable(true);
        info.setChecked(checked);
    }
}
