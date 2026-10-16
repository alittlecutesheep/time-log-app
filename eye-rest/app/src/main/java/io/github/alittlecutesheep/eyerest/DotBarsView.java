package io.github.alittlecutesheep.eyerest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.Arrays;

/** 最近 7 天休息次數的點陣長條圖，今天那一欄頂端是紅點。 */
public class DotBarsView extends View {
    private static final int ROWS = 8;

    private final Paint dots = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labels = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint numbers = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float pitch;
    private final float labelGap;
    private int[] values = new int[0];
    private String[] names = new String[0];

    public DotBarsView(Context c) {
        super(c);
        pitch = Ui.dpf(c, 11);
        labelGap = Ui.dpf(c, 22);
        labels.setTypeface(Ui.mono(c));
        labels.setTextSize(Ui.dpf(c, 10));
        labels.setTextAlign(Paint.Align.CENTER);
        labels.setLetterSpacing(0.1f);
        numbers.setTypeface(Ui.mono(c));
        numbers.setTextSize(Ui.dpf(c, 10));
        numbers.setTextAlign(Paint.Align.CENTER);
        numbers.setColor(Ui.TEXT_2);
    }

    void setData(int[] values, String[] names) {
        if (Arrays.equals(values, this.values) && Arrays.equals(names, this.names)) return;
        this.values = values.clone();
        this.names = names.clone();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = (int) (labelGap + ROWS * pitch + labelGap);
        setMeasuredDimension(w, resolveSize(h, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int n = values.length;
        if (n == 0) return;
        int max = 1;
        for (int v : values) max = Math.max(max, v);
        float colW = getWidth() / (float) n;
        float r = pitch * 0.34f;
        float top = labelGap;
        for (int i = 0; i < n; i++) {
            boolean today = i == n - 1;
            float cx = colW * (i + 0.5f);
            int lit = values[i] == 0 ? 0 : Math.max(1, Math.round(values[i] * ROWS / (float) max));
            for (int row = 0; row < ROWS; row++) {
                int fromBottom = ROWS - 1 - row;
                boolean on = fromBottom < lit;
                boolean head = today && on && fromBottom == lit - 1;
                if (head) dots.setColor(Ui.RED);
                else if (on) dots.setColor(today ? Ui.TEXT : 0xFFBDBDBD);
                else dots.setColor(Ui.DOT_DIM);
                canvas.drawCircle(cx, top + (row + 0.5f) * pitch, on ? r : r * 0.8f, dots);
            }
            if (values[i] > 0) {
                float y = top + (ROWS - lit) * pitch - Ui.dpf(getContext(), 6);
                canvas.drawText(String.valueOf(values[i]), cx, y, numbers);
            }
            labels.setColor(today ? Ui.TEXT : Ui.TEXT_3);
            canvas.drawText(i < names.length ? names[i] : "", cx, top + ROWS * pitch + labelGap * 0.8f, labels);
        }
    }
}
