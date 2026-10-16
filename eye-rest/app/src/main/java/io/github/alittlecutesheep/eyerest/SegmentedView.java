package io.github.alittlecutesheep.eyerest;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 分段選擇（例如震動強度：輕 / 標準 / 強）。選中的是白底黑字。 */
public class SegmentedView extends LinearLayout {
    interface Listener {
        void onSelect(int index);
    }

    private final TextView[] items;
    private int selected;
    private Listener listener;

    public SegmentedView(Context c, String[] labels, int initial) {
        super(c);
        setOrientation(HORIZONTAL);
        items = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView t = Ui.text(c, labels[i], 14, Ui.TEXT);
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            t.setGravity(Gravity.CENTER);
            t.setClickable(true);
            t.setFocusable(true);
            t.setOnClickListener(v -> select(index, true));
            LayoutParams lp = new LayoutParams(0, Ui.dp(c, 44), 1f);
            if (i > 0) lp.leftMargin = Ui.dp(c, 8);
            addView(t, lp);
            items[i] = t;
        }
        select(initial, false);
    }

    void setListener(Listener l) {
        listener = l;
    }

    private void select(int index, boolean notify) {
        selected = index;
        float r = Ui.dpf(getContext(), 22);
        for (int i = 0; i < items.length; i++) {
            boolean on = i == selected;
            items[i].setTextColor(on ? Ui.BG : Ui.TEXT_2);
            items[i].setBackground(on
                    ? Ui.rounded(Ui.TEXT, 0, r, 0)
                    : Ui.ripple(Ui.rounded(Ui.BG, 0xFF333333, r, Ui.dp(getContext(), 1)), Ui.rounded(Ui.TEXT, 0, r, 0)));
            items[i].setSelected(on);
        }
        if (notify && listener != null) listener.onSelect(index);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (TextView t : items) t.setEnabled(enabled);
        setAlpha(enabled ? 1f : 0.32f);
    }
}
