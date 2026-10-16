package io.github.alittlecutesheep.eyerest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** 圓點組成的小圖示：設定（一圈點加中心點）、返回（點陣箭頭）。 */
public class DotIconView extends View {
    static final int SETTINGS = 0;
    static final int BACK = 1;

    private static final String[] ARROW = {
            "0001000",
            "0010000",
            "0100000",
            "1111111",
            "0100000",
            "0010000",
            "0001000",
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int icon;
    private final int size;

    public DotIconView(Context c, int icon) {
        super(c);
        this.icon = icon;
        this.size = Ui.dp(c, 46);
        paint.setColor(Ui.TEXT);
        setContentDescription(icon == SETTINGS ? "設定" : "返回");
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(resolveSize(size, widthSpec), resolveSize(size, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        if (icon == SETTINGS) {
            float ring = Ui.dpf(getContext(), 7.5f);
            float r = Ui.dpf(getContext(), 1.7f);
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(45.0 * i);
                canvas.drawCircle(cx + (float) (ring * Math.cos(a)), cy + (float) (ring * Math.sin(a)), r, paint);
            }
            paint.setColor(Ui.RED);
            canvas.drawCircle(cx, cy, Ui.dpf(getContext(), 2.4f), paint);
            paint.setColor(Ui.TEXT);
        } else {
            float pitch = Ui.dpf(getContext(), 2.6f);
            float r = pitch * 0.36f;
            float x0 = cx - pitch * 3.5f;
            float y0 = cy - pitch * 3.5f;
            for (int row = 0; row < ARROW.length; row++) {
                for (int col = 0; col < ARROW[row].length(); col++) {
                    if (ARROW[row].charAt(col) == '1') {
                        canvas.drawCircle(x0 + (col + 0.5f) * pitch, y0 + (row + 0.5f) * pitch, r, paint);
                    }
                }
            }
        }
    }
}
