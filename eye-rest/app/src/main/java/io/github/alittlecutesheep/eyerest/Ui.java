package io.github.alittlecutesheep.eyerest;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/** 黑白 + 一點紅的視覺元件與共用工具。 */
final class Ui {
    static final int BG = 0xFF000000;
    static final int SURFACE = 0xFF0C0C0C;
    static final int LINE = 0xFF242424;
    static final int TEXT = 0xFFFFFFFF;
    static final int TEXT_2 = 0xFF9A9A9A;
    static final int TEXT_3 = 0xFF555555;
    static final int DOT_OFF = 0xFF161616;
    static final int DOT_DIM = 0xFF2C2C2C;
    static final int RED = 0xFFD71921;

    private static Typeface mono;
    private static Typeface monoBold;

    private Ui() {}

    static Typeface mono(Context c) {
        if (mono == null) mono = font(c, "fonts/SpaceMono-Regular.ttf", Typeface.MONOSPACE);
        return mono;
    }

    static Typeface monoBold(Context c) {
        if (monoBold == null) monoBold = font(c, "fonts/SpaceMono-Bold.ttf", Typeface.MONOSPACE);
        return monoBold;
    }

    private static Typeface font(Context c, String path, Typeface fallback) {
        try {
            return Typeface.createFromAsset(c.getAssets(), path);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static float dpf(Context c, float v) {
        return v * c.getResources().getDisplayMetrics().density;
    }

    // ---- 文字 ----

    static TextView text(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLineSpacing(0f, 1.2f);
        return t;
    }

    static TextView heading(Context c, CharSequence s, float sp) {
        TextView t = text(c, s, sp, TEXT);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setLineSpacing(0f, 1.1f);
        return t;
    }

    /** 等寬大寫小標，例如 NEXT BREAK。 */
    static TextView label(Context c, String s) {
        TextView t = text(c, s.toUpperCase(Locale.US), 11, TEXT_2);
        t.setTypeface(mono(c));
        t.setLetterSpacing(0.14f);
        t.setLineSpacing(0f, 1f);
        return t;
    }

    // ---- 背景 ----

    static GradientDrawable rounded(int fill, int stroke, float radius, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        if (strokeWidth > 0) d.setStroke(strokeWidth, stroke);
        return d;
    }

    static GradientDrawable oval(int fill, int stroke, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        if (strokeWidth > 0) d.setStroke(strokeWidth, stroke);
        return d;
    }

    static Drawable ripple(Drawable content, Drawable mask) {
        return new RippleDrawable(ColorStateList.valueOf(0x2EFFFFFF), content, mask);
    }

    /** 深色卡片：細邊框、大圓角。 */
    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(rounded(SURFACE, LINE, dpf(c, 24), dp(c, 1)));
        int p = dp(c, 18);
        l.setPadding(p, p, p, p);
        return l;
    }

    /** 膠囊按鈕。filled = 白底黑字（主要動作），否則為描邊。 */
    static TextView pill(Context c, String s, boolean filled) {
        TextView t = text(c, s, 15, filled ? BG : TEXT);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(c, 52));
        int h = dp(c, 18);
        t.setPadding(h, 0, h, 0);
        float r = dpf(c, 26);
        GradientDrawable bg = filled ? rounded(TEXT, 0, r, 0) : rounded(BG, 0xFF3A3A3A, r, dp(c, 1));
        t.setBackground(ripple(bg, rounded(TEXT, 0, r, 0)));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    /** 圓形圖示按鈕（設定、返回）。 */
    static DotIconView iconButton(Context c, int icon) {
        DotIconView v = new DotIconView(c, icon);
        GradientDrawable bg = oval(BG, 0xFF3A3A3A, dp(c, 1));
        v.setBackground(ripple(bg, oval(TEXT, 0, 0)));
        v.setClickable(true);
        v.setFocusable(true);
        return v;
    }

    static View dot(Context c, int color, int sizeDp) {
        View v = new View(c);
        v.setBackground(oval(color, 0, 0));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return v;
    }

    static View space(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
    }

    static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    static void setEnabledLook(View v, boolean enabled) {
        v.setEnabled(enabled);
        v.setAlpha(enabled ? 1f : 0.32f);
    }

    // ---- 視窗 ----

    /** 內容延伸到狀態列與導覽列底下（Android 15 起強制），由 applyInsets 補回間距。 */
    @SuppressWarnings("deprecation")
    static void edgeToEdge(Activity a) {
        Window w = a.getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) w.setNavigationBarContrastEnforced(false);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            w.setAttributes(lp);
        }
    }

    /**
     * 把系統列（與鍵盤）的高度加到 view 的 padding。
     * stable = true 時即使系統列被隱藏也保留間距（給全螢幕休息畫面用）。
     */
    @SuppressWarnings("deprecation")
    static void applyInsets(View v, boolean includeIme, boolean stable) {
        v.setOnApplyWindowInsetsListener((view, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                int types = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout();
                Insets bars = stable ? insets.getInsetsIgnoringVisibility(types) : insets.getInsets(types);
                l = bars.left;
                t = bars.top;
                r = bars.right;
                b = bars.bottom;
                if (includeIme) b = Math.max(b, insets.getInsets(WindowInsets.Type.ime()).bottom);
            } else {
                l = insets.getSystemWindowInsetLeft();
                t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight();
                b = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(l, t, r, b);
            return insets;
        });
    }
}
