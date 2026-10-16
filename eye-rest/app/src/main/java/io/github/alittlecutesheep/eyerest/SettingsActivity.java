package io.github.alittlecutesheep.eyerest;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 設定頁：計時、提醒文字、震動、勿擾時段、系統權限。 */
public class SettingsActivity extends Activity {
    private static final int[] INTERVALS = StepperView.concat(
            StepperView.range(1, 9, 1), StepperView.range(10, 180, 5));
    private static final int[] RESTS = StepperView.concat(
            StepperView.range(10, 55, 5), StepperView.range(60, 285, 15), StepperView.range(300, 1800, 60));
    private static final int[] TIMES = StepperView.range(0, 23 * 60 + 45, 15);

    private final List<Runnable> permissionRefreshers = new ArrayList<>();
    private StepperView quietStart;
    private StepperView quietEnd;
    private SegmentedView strength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.edgeToEdge(this);
        setContentView(buildLayout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Engine.visibleScreens++;
        for (Runnable r : permissionRefreshers) r.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Engine.visibleScreens--;
        Engine.changed(this); // 文字、休息長度等改動同步到通知
    }

    private View buildLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = Ui.column(this);
        int pad = Ui.dp(this, 20);
        col.setPadding(pad, Ui.dp(this, 14), pad, Ui.dp(this, 40));
        // 先讓外層拿到焦點，避免一進來就跳到輸入框、捲動畫面
        col.setFocusableInTouchMode(true);
        col.requestFocus();
        scroll.addView(col, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        Ui.applyInsets(root, true, false);

        // 標頭
        LinearLayout header = Ui.row(this);
        DotIconView back = Ui.iconButton(this, DotIconView.BACK);
        back.setOnClickListener(v -> finish());
        header.addView(back, Ui.wrap());
        LinearLayout titles = Ui.column(this);
        titles.setPadding(Ui.dp(this, 14), 0, 0, 0);
        titles.addView(Ui.label(this, "SETTINGS"), Ui.wrap());
        titles.addView(Ui.heading(this, "設定", 26), Ui.wrap());
        header.addView(titles, Ui.weight(1f));
        col.addView(header, Ui.matchWrap());

        buildTimer(col);
        buildMessage(col);
        buildVibration(col);
        buildQuiet(col);
        buildSystem(col);

        TextView footer = Ui.label(this, "SPACE MONO · SIL OFL 1.1");
        footer.setTextColor(Ui.TEXT_3);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerLp = Ui.matchWrap();
        footerLp.topMargin = Ui.dp(this, 32);
        col.addView(footer, footerLp);
        return root;
    }

    // ---- 01 計時 ----

    private void buildTimer(LinearLayout col) {
        col.addView(section("01", "TIMER", "計時"), sectionLp());
        LinearLayout card = Ui.card(this);

        card.addView(rowTitle("計時方式",
                "實際時間：不管螢幕開關都持續計時。\n螢幕使用時間：只在螢幕開著且已解鎖時計時，關螢幕或鎖定就暫停，解鎖後接著算。"),
                Ui.matchWrap());
        SegmentedView mode = new SegmentedView(this, new String[]{"實際時間", "螢幕使用時間"}, Prefs.countMode(this));
        mode.setListener(i -> {
            Prefs.setCountMode(this, i);
            Engine.onCountModeChanged(this);
        });
        LinearLayout.LayoutParams modeLp = Ui.matchWrap();
        modeLp.topMargin = Ui.dp(this, 14);
        card.addView(mode, modeLp);

        card.addView(divider(), dividerLp());

        card.addView(rowTitle("用眼間隔", "累積多久跳出休息畫面。修改後會重新開始計時。"), Ui.matchWrap());
        StepperView interval = new StepperView(this, INTERVALS, false, Prefs.intervalMin(this),
                new StepperView.Format() {
                    @Override
                    public String value(int v) {
                        return String.valueOf(v);
                    }

                    @Override
                    public String unit(int v) {
                        return "MIN";
                    }
                });
        interval.setListener(v -> {
            Prefs.setIntervalMin(this, v);
            Engine.onIntervalChanged(this);
        });
        card.addView(interval, stepperLp());

        card.addView(divider(), dividerLp());

        card.addView(rowTitle("休息時間", "休息畫面要停留多久，時間到會震動並自動關閉。"), Ui.matchWrap());
        StepperView rest = new StepperView(this, RESTS, false, Prefs.restSec(this),
                new StepperView.Format() {
                    @Override
                    public String value(int v) {
                        if (v < 60) return String.valueOf(v);
                        return String.format(Locale.US, "%d:%02d", v / 60, v % 60);
                    }

                    @Override
                    public String unit(int v) {
                        return v < 60 ? "SEC" : "MIN";
                    }
                });
        rest.setListener(v -> Prefs.setRestSec(this, v));
        card.addView(rest, stepperLp());
        col.addView(card, Ui.matchWrap());
    }

    // ---- 02 提醒文字 ----

    private void buildMessage(LinearLayout col) {
        col.addView(section("02", "MESSAGE", "提醒文字"), sectionLp());
        LinearLayout card = Ui.card(this);

        card.addView(Ui.label(this, "TITLE · 標題"), Ui.wrap());
        EditText title = input(Prefs.title(this), Prefs.DEFAULT_TITLE, false);
        title.addTextChangedListener(watcher(s -> Prefs.setTitle(this, s)));
        card.addView(title, inputLp());

        LinearLayout.LayoutParams msgLabelLp = Ui.wrap();
        msgLabelLp.topMargin = Ui.dp(this, 18);
        card.addView(Ui.label(this, "MESSAGE · 內文"), msgLabelLp);
        EditText message = input(Prefs.message(this), Prefs.DEFAULT_MESSAGE, true);
        message.addTextChangedListener(watcher(s -> Prefs.setMessage(this, s)));
        card.addView(message, inputLp());

        TextView preview = Ui.pill(this, "預覽休息畫面", true);
        preview.setOnClickListener(v -> startActivity(BreakActivity.preview(this)));
        LinearLayout.LayoutParams previewLp = Ui.matchWrap();
        previewLp.topMargin = Ui.dp(this, 18);
        card.addView(preview, previewLp);
        col.addView(card, Ui.matchWrap());
    }

    // ---- 03 震動 ----

    private void buildVibration(LinearLayout col) {
        col.addView(section("03", "VIBRATION", "震動"), sectionLp());
        LinearLayout card = Ui.card(this);

        ToggleView endToggle = new ToggleView(this);
        endToggle.setChecked(Prefs.vibrateEnd(this), false);
        card.addView(toggleRow("休息結束時震動", "時間到時震動提醒，靜音模式下也會震。", endToggle), Ui.matchWrap());

        strength = new SegmentedView(this, new String[]{"輕", "標準", "強"}, Prefs.vibeStrength(this));
        strength.setListener(i -> {
            Prefs.setVibeStrength(this, i);
            Vibes.end(this, i);
        });
        strength.setEnabled(Prefs.vibrateEnd(this));
        LinearLayout.LayoutParams segLp = Ui.matchWrap();
        segLp.topMargin = Ui.dp(this, 14);
        card.addView(strength, segLp);
        endToggle.setListener(on -> {
            Prefs.setVibrateEnd(this, on);
            strength.setEnabled(on);
        });

        card.addView(divider(), dividerLp());

        ToggleView startToggle = new ToggleView(this);
        startToggle.setChecked(Prefs.vibrateStart(this), false);
        startToggle.setListener(on -> Prefs.setVibrateStart(this, on));
        card.addView(toggleRow("跳出時輕震一下", "休息畫面出現時短短震一下。", startToggle), Ui.matchWrap());

        TextView test = Ui.pill(this, "測試震動", false);
        test.setOnClickListener(v -> Vibes.end(this, Prefs.vibeStrength(this)));
        LinearLayout.LayoutParams testLp = Ui.matchWrap();
        testLp.topMargin = Ui.dp(this, 18);
        card.addView(test, testLp);
        col.addView(card, Ui.matchWrap());
    }

    // ---- 04 勿擾時段 ----

    private void buildQuiet(LinearLayout col) {
        col.addView(section("04", "QUIET HOURS", "勿擾時段"), sectionLp());
        LinearLayout card = Ui.card(this);

        ToggleView quietToggle = new ToggleView(this);
        quietToggle.setChecked(Prefs.quietEnabled(this), false);
        card.addView(toggleRow("啟用勿擾時段", "這段時間內不會跳出提醒，結束後自動恢復計時。", quietToggle), Ui.matchWrap());

        StepperView.Format clock = new StepperView.Format() {
            @Override
            public String value(int v) {
                return Fmt.minuteOfDay(v);
            }

            @Override
            public String unit(int v) {
                return "";
            }
        };

        card.addView(divider(), dividerLp());
        card.addView(Ui.label(this, "FROM · 開始"), Ui.wrap());
        quietStart = new StepperView(this, TIMES, true, Prefs.quietStart(this), clock);
        quietStart.setListener(v -> {
            Prefs.setQuietStart(this, v);
            Engine.onQuietChanged(this);
        });
        card.addView(quietStart, stepperLp());

        LinearLayout.LayoutParams toLp = Ui.wrap();
        toLp.topMargin = Ui.dp(this, 12);
        card.addView(Ui.label(this, "TO · 結束"), toLp);
        quietEnd = new StepperView(this, TIMES, true, Prefs.quietEnd(this), clock);
        quietEnd.setListener(v -> {
            Prefs.setQuietEnd(this, v);
            Engine.onQuietChanged(this);
        });
        card.addView(quietEnd, stepperLp());

        boolean on = Prefs.quietEnabled(this);
        quietStart.setEnabled(on);
        quietEnd.setEnabled(on);
        quietToggle.setListener(enabled -> {
            Prefs.setQuietEnabled(this, enabled);
            quietStart.setEnabled(enabled);
            quietEnd.setEnabled(enabled);
            Engine.onQuietChanged(this);
        });
        col.addView(card, Ui.matchWrap());
    }

    // ---- 05 系統權限 ----

    private void buildSystem(LinearLayout col) {
        col.addView(section("05", "SYSTEM", "系統權限"), sectionLp());
        LinearLayout card = Ui.card(this);
        String pkg = "package:" + getPackageName();

        Runnable openOverlay = () -> open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(pkg)));
        addPermission(card, "重疊顯示於其他應用程式上方",
                "讓休息畫面能在你使用其他 app 時直接跳出。",
                () -> Settings.canDrawOverlays(this), openOverlay, true);
        if (Build.VERSION.SDK_INT >= 33) {
            View guide = restrictedGuide(openOverlay);
            card.addView(guide, Ui.matchWrap());
            Runnable refreshGuide = () -> guide.setVisibility(Settings.canDrawOverlays(this) ? View.GONE : View.VISIBLE);
            refreshGuide.run();
            permissionRefreshers.add(refreshGuide);
        }

        addPermission(card, "通知",
                "通知欄的倒數與開關，以及鎖定畫面時的全螢幕提醒。",
                () -> {
                    NotificationManager nm = getSystemService(NotificationManager.class);
                    return nm == null || nm.areNotificationsEnabled();
                },
                () -> open(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())), false);

        if (Build.VERSION.SDK_INT >= 34) {
            addPermission(card, "全螢幕通知",
                    "螢幕關閉或鎖定時，讓休息畫面直接亮起來。",
                    () -> {
                        NotificationManager nm = getSystemService(NotificationManager.class);
                        return nm == null || nm.canUseFullScreenIntent();
                    },
                    () -> open(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse(pkg))), false);
        }

        if (Build.VERSION.SDK_INT >= 31) {
            addPermission(card, "精確鬧鐘",
                    "讓提醒準時跳出，不會被系統延後。",
                    () -> Engine.canExact(this),
                    () -> open(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse(pkg))), false);
        }

        addPermission(card, "不受電池最佳化限制",
                "避免系統在背景把 app 關掉，部分品牌（如小米、三星）特別需要。",
                () -> {
                    PowerManager pm = getSystemService(PowerManager.class);
                    return pm == null || pm.isIgnoringBatteryOptimizations(getPackageName());
                },
                () -> openBatterySettings(pkg), false);

        // 快捷設定方塊
        card.addView(divider(), dividerLp());
        LinearLayout tileRow = Ui.row(this);
        LinearLayout text = rowTitle("快捷設定方塊",
                Build.VERSION.SDK_INT >= 33
                        ? "加入後可以從下拉選單直接開關提醒。"
                        : "下拉快捷設定 → 編輯（鉛筆圖示）→ 把「護眼提醒」拖進來。");
        tileRow.addView(text, Ui.weight(1f));
        if (Build.VERSION.SDK_INT >= 33) {
            TextView add = chip("加入", true);
            add.setOnClickListener(v -> requestAddTile(add));
            tileRow.addView(add, Ui.wrap());
        }
        card.addView(tileRow, Ui.matchWrap());

        col.addView(card, Ui.matchWrap());
    }

    /**
     * 從瀏覽器下載安裝的 app，Android 13 起會把部分權限列為「受限制的設定」，開關變成灰色。
     * 要先在應用程式資訊頁「允許受限制的設定」才能打開。
     */
    private View restrictedGuide(Runnable openOverlay) {
        LinearLayout box = Ui.column(this);
        box.setBackground(Ui.rounded(0xFF050505, 0xFF2E2E2E, Ui.dpf(this, 18), Ui.dp(this, 1)));
        int p = Ui.dp(this, 16);
        box.setPadding(p, p, p, p);

        box.addView(Ui.heading(this, "開關是灰色的、打不開？", 15), Ui.wrap());
        TextView why = Ui.text(this,
                "這是 Android 對「從瀏覽器下載安裝的 App」的保護（受限制的設定），不是 App 壞掉。照下面做一次就好：",
                12.5f, Ui.TEXT_2);
        why.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 4));
        box.addView(why, Ui.matchWrap());

        String[] steps = {
                "點「重疊顯示設定」，在那一頁先點一下灰色的開關，跳出「受限制的設定」後按「確定」。",
                "點「應用程式資訊」，按右上角「⋮」→「允許受限制的設定」，再驗證指紋或密碼。",
                "回到「重疊顯示設定」，把開關打開就完成了。",
        };
        for (int i = 0; i < steps.length; i++) {
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.TOP);
            TextView idx = Ui.label(this, String.valueOf(i + 1));
            idx.setTextColor(Ui.RED);
            idx.setPadding(0, Ui.dp(this, 2), Ui.dp(this, 12), 0);
            row.addView(idx, Ui.wrap());
            row.addView(Ui.text(this, steps[i], 13, Ui.TEXT), Ui.weight(1f));
            LinearLayout.LayoutParams lp = Ui.matchWrap();
            lp.topMargin = Ui.dp(this, 10);
            box.addView(row, lp);
        }

        LinearLayout buttons = Ui.row(this);
        TextView overlay = Ui.pill(this, "重疊顯示設定", false);
        overlay.setTextSize(14);
        overlay.setOnClickListener(v -> openOverlay.run());
        TextView info = Ui.pill(this, "應用程式資訊", false);
        info.setTextSize(14);
        info.setOnClickListener(v -> open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()))));
        buttons.addView(overlay, Ui.weight(1f));
        LinearLayout.LayoutParams infoLp = Ui.weight(1f);
        infoLp.leftMargin = Ui.dp(this, 8);
        buttons.addView(info, infoLp);
        LinearLayout.LayoutParams buttonsLp = Ui.matchWrap();
        buttonsLp.topMargin = Ui.dp(this, 16);
        box.addView(buttons, buttonsLp);

        LinearLayout wrap = Ui.column(this);
        wrap.setPadding(0, Ui.dp(this, 14), 0, 0);
        wrap.addView(box, Ui.matchWrap());
        return wrap;
    }

    private interface Check {
        boolean granted();
    }

    private void addPermission(LinearLayout card, String title, String desc, Check check, Runnable fix, boolean first) {
        if (!first) card.addView(divider(), dividerLp());
        LinearLayout row = Ui.row(this);
        row.addView(rowTitle(title, desc), Ui.weight(1f));
        TextView chip = chip("", false);
        row.addView(chip, Ui.wrap());
        card.addView(row, Ui.matchWrap());
        Runnable refresh = () -> {
            boolean ok = check.granted();
            styleChip(chip, ok ? "已允許" : "前往", !ok);
            chip.setClickable(!ok);
            row.setOnClickListener(ok ? null : v -> fix.run());
            chip.setOnClickListener(ok ? null : v -> fix.run());
        };
        refresh.run();
        permissionRefreshers.add(refresh);
    }

    @SuppressWarnings("BatteryLife")
    private void openBatterySettings(String pkg) {
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse(pkg)));
        } catch (ActivityNotFoundException e) {
            open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        }
    }

    private void requestAddTile(TextView chip) {
        if (Build.VERSION.SDK_INT < 33) return;
        StatusBarManager sbm = getSystemService(StatusBarManager.class);
        if (sbm == null) return;
        sbm.requestAddTileService(
                new ComponentName(this, RestTileService.class),
                getString(R.string.tile_label),
                Icon.createWithResource(this, R.drawable.ic_stat_eye),
                getMainExecutor(),
                result -> {
                    boolean added = result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED
                            || result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED;
                    if (added) {
                        styleChip(chip, "已加入", false);
                        chip.setOnClickListener(null);
                        RestTileService.requestUpdate(this);
                    } else {
                        Toast.makeText(this, "沒有加入，可以之後再試", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---- 小元件 ----

    private LinearLayout section(String index, String en, String zh) {
        LinearLayout row = Ui.row(this);
        TextView idx = Ui.label(this, index);
        idx.setTextColor(Ui.RED);
        row.addView(idx, Ui.wrap());
        TextView label = Ui.label(this, en);
        label.setTextColor(Ui.TEXT);
        label.setPadding(Ui.dp(this, 10), 0, 0, 0);
        row.addView(label, Ui.wrap());
        TextView sub = Ui.text(this, zh, 12, Ui.TEXT_3);
        sub.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 12), 0);
        row.addView(sub, Ui.wrap());
        row.addView(new DotDividerView(this), Ui.weight(1f));
        return row;
    }

    private LinearLayout.LayoutParams sectionLp() {
        LinearLayout.LayoutParams lp = Ui.matchWrap();
        lp.topMargin = Ui.dp(this, 34);
        lp.bottomMargin = Ui.dp(this, 12);
        return lp;
    }

    private LinearLayout rowTitle(String title, String desc) {
        LinearLayout l = Ui.column(this);
        l.addView(Ui.heading(this, title, 16), Ui.wrap());
        TextView d = Ui.text(this, desc, 12.5f, Ui.TEXT_2);
        d.setPadding(0, Ui.dp(this, 4), Ui.dp(this, 10), 0);
        l.addView(d, Ui.wrap());
        return l;
    }

    private LinearLayout toggleRow(String title, String desc, ToggleView toggle) {
        LinearLayout row = Ui.row(this);
        row.addView(rowTitle(title, desc), Ui.weight(1f));
        row.addView(toggle, Ui.wrap());
        row.setOnClickListener(v -> toggle.performClick());
        return row;
    }

    private View divider() {
        return new DotDividerView(this);
    }

    private LinearLayout.LayoutParams dividerLp() {
        LinearLayout.LayoutParams lp = Ui.matchWrap();
        lp.topMargin = Ui.dp(this, 18);
        lp.bottomMargin = Ui.dp(this, 18);
        return lp;
    }

    private LinearLayout.LayoutParams stepperLp() {
        LinearLayout.LayoutParams lp = Ui.matchWrap();
        lp.topMargin = Ui.dp(this, 10);
        return lp;
    }

    private TextView chip(String s, boolean action) {
        TextView t = Ui.text(this, s, 12, Ui.TEXT);
        t.setGravity(Gravity.CENTER);
        t.setMinWidth(Ui.dp(this, 64));
        t.setMinHeight(Ui.dp(this, 34));
        int h = Ui.dp(this, 12);
        t.setPadding(h, 0, h, 0);
        styleChip(t, s, action);
        return t;
    }

    private void styleChip(TextView t, String s, boolean action) {
        t.setText(s);
        float r = Ui.dpf(this, 17);
        if (action) {
            t.setTextColor(Ui.TEXT);
            GradientDrawable bg = Ui.rounded(Ui.BG, Ui.RED, r, Ui.dp(this, 1));
            t.setBackground(Ui.ripple(bg, Ui.rounded(Ui.TEXT, 0, r, 0)));
        } else {
            t.setTextColor(Ui.TEXT_2);
            t.setBackground(Ui.rounded(0xFF161616, 0, r, 0));
        }
    }

    private EditText input(String value, String hint, boolean multiLine) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setHint(hint);
        e.setHintTextColor(Ui.TEXT_3);
        e.setTextColor(Ui.TEXT);
        e.setTextSize(16);
        e.setBackground(Ui.rounded(0xFF050505, 0xFF2E2E2E, Ui.dpf(this, 16), Ui.dp(this, 1)));
        int p = Ui.dp(this, 14);
        e.setPadding(p, p, p, p);
        if (multiLine) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(2);
            e.setMaxLines(5);
            e.setGravity(Gravity.TOP | Gravity.START);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setSingleLine(true);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            GradientDrawable cursor = new GradientDrawable();
            cursor.setColor(Ui.RED);
            cursor.setSize(Ui.dp(this, 2), Ui.dp(this, 20));
            e.setTextCursorDrawable(cursor);
        }
        return e;
    }

    private LinearLayout.LayoutParams inputLp() {
        LinearLayout.LayoutParams lp = Ui.matchWrap();
        lp.topMargin = Ui.dp(this, 8);
        return lp;
    }

    private interface TextSink {
        void accept(String s);
    }

    private TextWatcher watcher(TextSink sink) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                sink.accept(s.toString());
            }
        };
    }
}
