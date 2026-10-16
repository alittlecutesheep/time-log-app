package io.github.alittlecutesheep.eyerest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 「−  數值  +」調整器，數值用點陣字顯示。按住 −/+ 會連續調整。
 * 可選的值由 values 陣列決定；wrap = true 時（例如時間）超過頭尾會繞回去。
 */
public class StepperView extends LinearLayout {
    interface Format {
        String value(int v);

        String unit(int v);
    }

    interface Listener {
        void onChange(int value);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[] values;
    private final boolean wrap;
    private final Format format;
    private final DotMatrixView display;
    private final TextView unit;
    private final TextView minus;
    private final TextView plus;
    private Listener listener;
    private int value;
    private int repeatDir;
    private boolean repeated;

    private final Runnable repeat = new Runnable() {
        @Override
        public void run() {
            repeated = true;
            step(repeatDir);
            handler.postDelayed(this, 70);
        }
    };

    public StepperView(Context c, int[] values, boolean wrap, int initial, Format format) {
        super(c);
        this.values = values;
        this.wrap = wrap;
        this.format = format;
        this.value = initial;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        minus = button(c, "−", -1);
        plus = button(c, "+", 1);

        LinearLayout middle = new LinearLayout(c);
        middle.setOrientation(HORIZONTAL);
        middle.setGravity(Gravity.CENTER);
        display = new DotMatrixView(c, 5.2f);
        display.setPanel(false);
        unit = Ui.label(c, "");
        unit.setPadding(Ui.dp(c, 8), 0, 0, 0);
        middle.addView(display, Ui.wrap());
        middle.addView(unit, Ui.wrap());

        addView(minus, new LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48)));
        addView(middle, new LayoutParams(0, Ui.dp(c, 56), 1f));
        addView(plus, new LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48)));
        render();
    }

    void setListener(Listener l) {
        listener = l;
    }

    int getValue() {
        return value;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        minus.setEnabled(enabled);
        plus.setEnabled(enabled);
        setAlpha(enabled ? 1f : 0.32f);
    }

    @SuppressLint("ClickableViewAccessibility")
    private TextView button(Context c, String s, int dir) {
        TextView t = Ui.text(c, s, 22, Ui.TEXT);
        t.setTypeface(Ui.mono(c));
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.ripple(Ui.oval(Ui.BG, 0xFF3A3A3A, Ui.dp(c, 1)), Ui.oval(Ui.TEXT, 0, 0)));
        t.setContentDescription(dir > 0 ? "增加" : "減少");
        t.setClickable(true);
        t.setOnClickListener(v -> {
            if (repeated) {
                repeated = false; // 長按連續調整後放開，不再多跳一格
                return;
            }
            step(dir);
        });
        t.setOnTouchListener((v, e) -> {
            if (!v.isEnabled()) return false;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    repeatDir = dir;
                    repeated = false;
                    handler.postDelayed(repeat, 420);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(repeat);
                    break;
                default:
                    break;
            }
            return false; // 讓點一下仍由 OnClickListener 處理
        });
        return t;
    }

    private void step(int dir) {
        int next = next(value, dir);
        if (next == value) return;
        value = next;
        render();
        if (listener != null) listener.onChange(value);
    }

    private int next(int v, int dir) {
        int n = values.length;
        if (dir > 0) {
            for (int candidate : values) if (candidate > v) return candidate;
            return wrap ? values[0] : values[n - 1];
        }
        for (int i = n - 1; i >= 0; i--) if (values[i] < v) return values[i];
        return wrap ? values[n - 1] : values[0];
    }

    private void render() {
        display.setText(format.value(value));
        String u = format.unit(value);
        unit.setText(u);
        unit.setVisibility(u.isEmpty() ? GONE : VISIBLE);
        setContentDescription(format.value(value) + " " + u);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(repeat);
        super.onDetachedFromWindow();
    }

    /** 由 from 到 to（含）每次加 step 的整數陣列。 */
    static int[] range(int from, int to, int step) {
        int n = (to - from) / step + 1;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = from + i * step;
        return out;
    }

    static int[] concat(int[]... parts) {
        int total = 0;
        for (int[] p : parts) total += p.length;
        int[] out = new int[total];
        int k = 0;
        for (int[] p : parts) for (int v : p) out[k++] = v;
        return out;
    }
}
