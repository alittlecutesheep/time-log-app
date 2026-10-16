package io.github.alittlecutesheep.eyerest;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** 快捷設定方塊：點一下開關提醒，副標顯示下次休息時間。 */
public class RestTileService extends TileService {

    @Override
    public void onStartListening() {
        refresh();
    }

    @Override
    public void onClick() {
        if (Prefs.enabled(this)) Engine.disable(this, false);
        else Engine.enable(this);
        refresh();
    }

    private void refresh() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean on = Prefs.enabled(this);
        String sub = subtitle(this);
        tile.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(getString(R.string.tile_label));
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat_eye));
        tile.setContentDescription(getString(R.string.tile_label) + "，" + sub);
        if (Build.VERSION.SDK_INT >= 29) tile.setSubtitle(sub);
        tile.updateTile();
    }

    static String subtitle(Context c) {
        if (!Prefs.enabled(c)) return "已關閉";
        String time = Fmt.clock(Prefs.phaseEnd(c));
        switch (Prefs.phase(c)) {
            case Prefs.PHASE_REST:
                return "休息中";
            case Prefs.PHASE_QUIET:
                return time + " 恢復";
            default:
                return Quiet.active(c, Prefs.phaseEnd(c)) ? time + " 勿擾" : time + " 休息";
        }
    }

    static void requestUpdate(Context c) {
        try {
            TileService.requestListeningState(c, new ComponentName(c, RestTileService.class));
        } catch (RuntimeException ignored) {
            // 方塊還沒加入快捷設定時可以忽略
        }
    }
}
