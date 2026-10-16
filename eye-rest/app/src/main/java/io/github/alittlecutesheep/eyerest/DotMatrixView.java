package io.github.alittlecutesheep.eyerest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 點陣字：用 5×7 圓點畫數字與英文大寫，仿 LED 點陣顯示。
 * panel 模式會把沒亮的點也淡淡畫出來，看起來像一整塊點陣面板。
 */
public class DotMatrixView extends View {
    private static final int ROWS = 7;
    private static final Map<Character, String[]> FONT = new HashMap<>();

    static {
        glyph('0', "01110", "10001", "10001", "10001", "10001", "10001", "01110");
        glyph('1', "00100", "01100", "00100", "00100", "00100", "00100", "01110");
        glyph('2', "01110", "10001", "00001", "00010", "00100", "01000", "11111");
        glyph('3', "11111", "00010", "00100", "00010", "00001", "10001", "01110");
        glyph('4', "00010", "00110", "01010", "10010", "11111", "00010", "00010");
        glyph('5', "11111", "10000", "11110", "00001", "00001", "10001", "01110");
        glyph('6', "00110", "01000", "10000", "11110", "10001", "10001", "01110");
        glyph('7', "11111", "00001", "00010", "00100", "01000", "01000", "01000");
        glyph('8', "01110", "10001", "10001", "01110", "10001", "10001", "01110");
        glyph('9', "01110", "10001", "10001", "01111", "00001", "00010", "01100");
        glyph('A', "01110", "10001", "10001", "11111", "10001", "10001", "10001");
        glyph('B', "11110", "10001", "10001", "11110", "10001", "10001", "11110");
        glyph('C', "01110", "10001", "10000", "10000", "10000", "10001", "01110");
        glyph('D', "11100", "10010", "10001", "10001", "10001", "10010", "11100");
        glyph('E', "11111", "10000", "10000", "11110", "10000", "10000", "11111");
        glyph('F', "11111", "10000", "10000", "11110", "10000", "10000", "10000");
        glyph('G', "01110", "10001", "10000", "10111", "10001", "10001", "01111");
        glyph('H', "10001", "10001", "10001", "11111", "10001", "10001", "10001");
        glyph('I', "111", "010", "010", "010", "010", "010", "111");
        glyph('J', "00111", "00010", "00010", "00010", "00010", "10010", "01100");
        glyph('K', "10001", "10010", "10100", "11000", "10100", "10010", "10001");
        glyph('L', "10000", "10000", "10000", "10000", "10000", "10000", "11111");
        glyph('M', "10001", "11011", "10101", "10101", "10001", "10001", "10001");
        glyph('N', "10001", "10001", "11001", "10101", "10011", "10001", "10001");
        glyph('O', "01110", "10001", "10001", "10001", "10001", "10001", "01110");
        glyph('P', "11110", "10001", "10001", "11110", "10000", "10000", "10000");
        glyph('Q', "01110", "10001", "10001", "10001", "10101", "10010", "01101");
        glyph('R', "11110", "10001", "10001", "11110", "10100", "10010", "10001");
        glyph('S', "01111", "10000", "10000", "01110", "00001", "00001", "11110");
        glyph('T', "11111", "00100", "00100", "00100", "00100", "00100", "00100");
        glyph('U', "10001", "10001", "10001", "10001", "10001", "10001", "01110");
        glyph('V', "10001", "10001", "10001", "10001", "10001", "01010", "00100");
        glyph('W', "10001", "10001", "10001", "10101", "10101", "10101", "01010");
        glyph('X', "10001", "10001", "01010", "00100", "01010", "10001", "10001");
        glyph('Y', "10001", "10001", "10001", "01010", "00100", "00100", "00100");
        glyph('Z', "11111", "00001", "00010", "00100", "01000", "10000", "11111");
        glyph(':', "0", "0", "1", "0", "1", "0", "0");
        glyph('.', "0", "0", "0", "0", "0", "0", "1");
        glyph('-', "000", "000", "000", "111", "000", "000", "000");
        glyph('/', "00001", "00010", "00010", "00100", "01000", "01000", "10000");
        glyph(' ', "00", "00", "00", "00", "00", "00", "00");
    }

    private static void glyph(char ch, String... rows) {
        FONT.put(ch, rows);
    }

    private static String[] glyphOf(char ch) {
        String[] g = FONT.get(ch);
        return g != null ? g : FONT.get(' ');
    }

    /** 字串總共佔幾欄（字與字之間空一欄）。 */
    static int columns(String s) {
        int cols = 0;
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) cols++;
            cols += glyphOf(s.charAt(i))[0].length();
        }
        return cols;
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String text = "";
    private float maxPitch;
    private float pitch;
    private float dotRatio = 0.74f;
    private int onColor = Ui.TEXT;
    private int offColor = Ui.DOT_OFF;
    private boolean panel;
    private boolean alignLeft;

    public DotMatrixView(Context c, float pitchDp) {
        super(c);
        maxPitch = pitch = Ui.dpf(c, pitchDp);
    }

    void setText(String s) {
        String next = s == null ? "" : s.toUpperCase(Locale.US);
        if (next.equals(text)) return;
        boolean resize = columns(next) != columns(text);
        text = next;
        setContentDescription(next);
        if (resize) requestLayout();
        invalidate();
    }

    String getText() {
        return text;
    }

    void setPitchDp(float dp) {
        float p = Ui.dpf(getContext(), dp);
        if (p == maxPitch) return;
        maxPitch = p;
        requestLayout();
        invalidate();
    }

    void setColors(int on, int off) {
        if (on == onColor && off == offColor) return;
        onColor = on;
        offColor = off;
        invalidate();
    }

    void setPanel(boolean panel) {
        this.panel = panel;
        invalidate();
    }

    void setAlignLeft(boolean left) {
        alignLeft = left;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int cols = Math.max(1, columns(text));
        int padH = getPaddingLeft() + getPaddingRight();
        int padV = getPaddingTop() + getPaddingBottom();
        float p = maxPitch;
        int avail = MeasureSpec.getSize(widthSpec) - padH;
        if (MeasureSpec.getMode(widthSpec) != MeasureSpec.UNSPECIFIED && avail > 0 && cols * p > avail) {
            p = avail / (float) cols; // 放不下時自動縮小
        }
        pitch = p;
        int w = (int) Math.ceil(cols * p) + padH;
        int h = (int) Math.ceil(ROWS * p) + padV;
        setMeasuredDimension(resolveSize(w, widthSpec), resolveSize(h, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int cols = columns(text);
        float contentW = getWidth() - getPaddingLeft() - getPaddingRight();
        float contentH = getHeight() - getPaddingTop() - getPaddingBottom();
        float x0 = getPaddingLeft() + (alignLeft ? 0f : (contentW - cols * pitch) / 2f);
        float y0 = getPaddingTop() + (contentH - ROWS * pitch) / 2f;
        float r = pitch * dotRatio / 2f;

        int col = 0;
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) {
                if (panel) drawColumn(canvas, x0, y0, col, r);
                col++;
            }
            String[] g = glyphOf(text.charAt(i));
            int gw = g[0].length();
            for (int row = 0; row < ROWS; row++) {
                for (int cc = 0; cc < gw; cc++) {
                    boolean on = g[row].charAt(cc) == '1';
                    if (!on && !panel) continue;
                    paint.setColor(on ? onColor : offColor);
                    canvas.drawCircle(x0 + (col + cc + 0.5f) * pitch, y0 + (row + 0.5f) * pitch, r, paint);
                }
            }
            col += gw;
        }
    }

    private void drawColumn(Canvas canvas, float x0, float y0, int col, float r) {
        paint.setColor(offColor);
        for (int row = 0; row < ROWS; row++) {
            canvas.drawCircle(x0 + (col + 0.5f) * pitch, y0 + (row + 0.5f) * pitch, r, paint);
        }
    }
}
