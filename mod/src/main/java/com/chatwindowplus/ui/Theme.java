package com.chatwindowplus.ui;

import com.chatwindowplus.chat.ChatCategory;
import com.chatwindowplus.config.ChatWindowConfig;

/** 统一的配色表（ARGB）。面板背景/边框/文字颜色可在游戏内设置里调整。 */
public final class Theme {
    private Theme() {
    }

    public static int PANEL_BG = 0xFF0A111C;
    public static int PANEL_BORDER = 0xFF233A50;
    public static int PANEL_TOP = 0xFF0A111C;
    public static int PANEL_BOTTOM = 0xFF0E1B2A;
    public static int BORDER_HIGHLIGHT = 0x33FFFFFF;
    public static int BORDER_SHADE = 0x60000000;
    public static String DECOR = "bubbles";
    public static boolean CHAMFER = false;

    public static int TEXT = 0xFFCBD8E6;
    public static final int TEXT_DIM = 0xFF8CA0B6;
    public static final int TEXT_FAINT = 0xFF6B7E93;
    public static int NAME = 0xFFF4F8FF;
    public static int TAG = 0xFF7E92A8;
    public static final int MENTION = 0xFFFFD166;
    public static final int LINK = 0xFF6FB7FF;

    public static final int TAB_TEXT = 0xFF93A7BC;
    public static final int TAB_TEXT_ACTIVE = 0xFF0B1220;
    public static final int TAB_BG_ACTIVE = 0xFFE8EEF5;
    public static final int TAB_BG_HOVER = 0x26FFFFFF;

    public static final int INPUT_BG = 0xE60D1724;
    public static final int INPUT_BORDER = 0xFF2A4159;
    public static final int PLACEHOLDER = 0xFF6B7E93;

    public static final int SCROLL_TRACK = 0x1AFFFFFF;
    public static final int SCROLL_THUMB = 0x66FFFFFF;

    /** 主题强调色（页签选中、输入框描边）。 */
    public static int ACCENT = 0xFF5BC8F5;
    private static final int[] CATEGORY = {
            0xFF5BC8F5, 0xFF7ED88F, 0xFFFF9AD5, 0xFF9AA8FF, 0xFFFFC266, 0xFFFF7A7A, 0xFF9DB0C4
    };

    /** 从配置刷新可自定义的颜色。 */
    public static void apply(ChatWindowConfig config) {
        if (config == null) {
            return;
        }
        PANEL_BG = opaque(parseColor(config.panelColor, 0x0A111C));
        PANEL_BORDER = opaque(parseColor(config.borderColor, 0x233A50));
        PANEL_TOP = opaque(parseColor(config.panelColorTop, PANEL_BG & 0x00FFFFFF));
        PANEL_BOTTOM = opaque(parseColor(config.panelColorBottom, PANEL_TOP & 0x00FFFFFF));
        BORDER_HIGHLIGHT = withAlpha(0xFFFFFFFF, 0.20F);
        BORDER_SHADE = 0x66000000;
        DECOR = config.decor == null ? "none" : config.decor;
        CHAMFER = config.chamfer;
        TEXT = opaque(parseColor(config.textColor, 0xCBD8E6));
        NAME = opaque(parseColor(config.nameColor, 0xF4F8FF));
        TAG = opaque(parseColor(config.tagColor, 0x7E92A8));
        ACCENT = opaque(parseColor(config.accentColor, 0x5BC8F5));
        if (config.categoryColors != null) {
            for (int i = 0; i < CATEGORY.length && i < config.categoryColors.size(); i++) {
                CATEGORY[i] = opaque(parseColor(config.categoryColors.get(i), CATEGORY[i]));
            }
        }
    }

    /** 某个聊天分类的色签颜色。 */
    public static int category(ChatCategory category) {
        if (category == null || category == ChatCategory.ALL) {
            return TEXT;
        }
        int index = category.ordinal() - 1;
        return index >= 0 && index < CATEGORY.length ? CATEGORY[index] : TEXT;
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    /** 解析 #RRGGBB / RRGGBB，失败返回 fallback（RGB）。 */
    public static int parseColor(String hex, int fallback) {
        if (hex == null) {
            return fallback;
        }
        String value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            return fallback;
        }
        try {
            return Integer.parseInt(value, 16) & 0x00FFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static String toHex(int argb) {
        return String.format("#%06X", argb & 0x00FFFFFF);
    }

    public static int withAlpha(int argb, float alpha) {
        int a = (int) (((argb >>> 24) & 0xFF) * Math.max(0F, Math.min(1F, alpha)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** 把 RGB 颜色变成给定透明度的 ARGB。 */
    public static int alpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }
}