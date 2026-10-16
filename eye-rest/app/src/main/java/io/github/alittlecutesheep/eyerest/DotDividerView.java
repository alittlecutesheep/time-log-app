package io.github.alittlecutesheep.eyerest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** 點線分隔線。 */
public class DotDividerView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float gap;
    private final float radius;
    private final int height;

    public DotDividerView(Context c) {
        super(c);
        gap = Ui.dpf(c, 6);
        radius = Ui.dpf(c, 1);
        height = Ui.dp(c, 2);
        paint.setColor(0xFF2E2E2E);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), resolveSize(height, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float y = getHeight() / 2f;
        for (float x = radius; x < getWidth(); x += gap) {
            canvas.drawCircle(x, y, radius, paint);
        }
    }
}
