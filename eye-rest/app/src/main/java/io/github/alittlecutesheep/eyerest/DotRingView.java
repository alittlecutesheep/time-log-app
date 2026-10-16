package io.github.alittlecutesheep.eyerest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * 圓點進度環：一圈小圓點從 12 點鐘方向順時針點亮，最前端是一顆紅點。
 * 內圈再畫一圈很淡的細點當紋理。
 */
public class DotRingView extends View {
    private static final int COUNT = 60;
    private static final int TEXTURE_COUNT = 120;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float maxSize;
    private final float dotRadius;
    private float progress;
    private boolean showHead = true;
    private int litColor = Ui.TEXT;
    private float maxHeightFraction = 1f;

    public DotRingView(Context c, float maxSizeDp) {
        super(c);
        maxSize = Ui.dpf(c, maxSizeDp);
        dotRadius = Ui.dpf(c, 3.4f);
    }

    void setProgress(float p) {
        float clamped = Math.max(0f, Math.min(1f, p));
        if (clamped == progress) return;
        progress = clamped;
        invalidate();
    }

    void setShowHead(boolean show) {
        if (show == showHead) return;
        showHead = show;
        invalidate();
    }

    /** 限制圓環最多佔可用高度的比例（橫向螢幕時避免把文字擠出畫面）。 */
    void setMaxHeightFraction(float f) {
        maxHeightFraction = f;
        requestLayout();
    }

    /** 讓中間的點陣倒數剛好放得進圓環內圈。 */
    float fitPitchDp(String text, float maxDp) {
        float density = getResources().getDisplayMetrics().density;
        float ringDp = getWidth() / density;
        if (ringDp <= 0) return maxDp;
        return Math.min(maxDp, ringDp * 0.6f / Math.max(1, DotMatrixView.columns(text)));
    }

    void setLitColor(int color) {
        if (color == litColor) return;
        litColor = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int size = (int) maxSize;
        if (MeasureSpec.getMode(widthSpec) != MeasureSpec.UNSPECIFIED) size = Math.min(size, w);
        if (MeasureSpec.getMode(heightSpec) == MeasureSpec.AT_MOST
                || MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY) {
            int h = MeasureSpec.getSize(heightSpec);
            if (h > 0) size = Math.min(size, (int) (h * maxHeightFraction));
        }
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - dotRadius * 1.6f;

        // 內圈紋理
        float inner = radius - dotRadius * 5.2f;
        float tiny = dotRadius * 0.38f;
        paint.setColor(Ui.DOT_OFF);
        for (int i = 0; i < TEXTURE_COUNT; i++) {
            double a = Math.toRadians(-90.0 + 360.0 * i / TEXTURE_COUNT);
            canvas.drawCircle(cx + (float) (inner * Math.cos(a)), cy + (float) (inner * Math.sin(a)), tiny, paint);
        }

        float filled = progress * COUNT;
        int full = (int) Math.floor(filled);
        for (int i = 0; i < COUNT; i++) {
            double a = Math.toRadians(-90.0 + 360.0 * i / COUNT);
            float x = cx + (float) (radius * Math.cos(a));
            float y = cy + (float) (radius * Math.sin(a));
            float r = dotRadius;
            if (i < full) {
                paint.setColor(litColor);
            } else if (i == full && showHead && progress < 1f) {
                paint.setColor(Ui.RED);
                r = dotRadius * 1.3f;
            } else {
                paint.setColor(Ui.DOT_DIM);
                r = dotRadius * 0.82f;
            }
            canvas.drawCircle(x, y, r, paint);
        }
    }
}
