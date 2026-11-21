package com.chatwindowplus.ui;

import net.minecraft.client.gui.DrawContext;

/** 一些绘制小工具：圆角/切角矩形、渐变、描边等。 */
public final class UiDraw {
    private UiDraw() {
    }

    public static void roundRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        if (r == 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }
        context.fill(x, y + r, x + width, y + height - r, color);
        int r2 = r * r;
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.floor(Math.sqrt(Math.max(0, r2 - dy * dy)));
            context.fill(x + inset, y + i, x + width - inset, y + i + 1, color);
            context.fill(x + inset, y + height - 1 - i, x + width - inset, y + height - i, color);
        }
    }

    /** 先铺满边框色，再在内侧铺背景色，得到 1px 圆角描边。 */
    public static void roundRectBordered(DrawContext context, int x, int y, int width, int height, int radius,
                                         int borderColor, int fillColor) {
        roundRect(context, x, y, width, height, radius, borderColor);
        roundRect(context, x + 1, y + 1, width - 2, height - 2, Math.max(0, radius - 1), fillColor);
    }

    public static void shadow(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        roundRect(context, x + 1, y + 2, width, height, radius, color);
    }

    /**
     * 竖直渐变面板。chamfer=true 时是 Minecraft 式的 2px 切角，否则是圆角。
     */
    public static void gradientPanel(DrawContext context, int x, int y, int width, int height, int radius,
                                     boolean chamfer, int topColor, int bottomColor) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int r = chamfer ? Math.min(2, Math.min(width / 2, height / 2))
                : Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        int r2 = r * r;
        for (int row = 0; row < height; row++) {
            int inset = 0;
            if (r > 0) {
                int dy = Math.min(row, height - 1 - row);
                if (dy < r) {
                    double d = r - dy - 0.5;
                    inset = r - (int) Math.floor(Math.sqrt(Math.max(0, r2 - d * d)));
                }
            }
            float t = height <= 1 ? 0F : (float) row / (height - 1);
            context.fill(x + inset, y + row, x + width - inset, y + row + 1, lerp(topColor, bottomColor, t));
        }
    }

    /** 面板描边：外框 + 左上高光、右下阴影，做出 MC 界面的立体感。 */
    public static void panelBorder(DrawContext context, int x, int y, int width, int height, int radius,
                                   boolean chamfer, int borderColor, int highlight, int shade) {
        int r = chamfer ? 2 : Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        // 外框
        context.fill(x + r, y, x + width - r, y + 1, borderColor);
        context.fill(x + r, y + height - 1, x + width - r, y + height, borderColor);
        context.fill(x, y + r, x + 1, y + height - r, borderColor);
        context.fill(x + width - 1, y + r, x + width, y + height - r, borderColor);
        for (int i = 0; i < r; i++) {
            int inset = r - 1 - i;
            context.fill(x + inset, y + i, x + inset + 1, y + i + 1, borderColor);
            context.fill(x + width - inset - 1, y + i, x + width - inset, y + i + 1, borderColor);
            context.fill(x + inset, y + height - i - 1, x + inset + 1, y + height - i, borderColor);
            context.fill(x + width - inset - 1, y + height - i - 1, x + width - inset, y + height - i, borderColor);
        }
        // 内侧高光/阴影
        context.fill(x + 1, y + 1, x + width - 1, y + 2, highlight);
        context.fill(x + 1, y + 1, x + 2, y + height - 1, highlight);
        context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, shade);
        context.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, shade);
    }

    public static int lerp(int from, int to, float t) {
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}