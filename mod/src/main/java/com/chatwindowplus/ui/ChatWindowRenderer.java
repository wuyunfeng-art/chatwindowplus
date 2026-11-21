package com.chatwindowplus.ui;

import com.chatwindowplus.ChatWindowPlus;
import com.chatwindowplus.chat.ChatCategory;
import com.chatwindowplus.chat.ChatEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * 聊天窗口的绘制与交互核心：
 * <ul>
 *     <li>未打开聊天：左下角常驻的紧凑聊天栏；</li>
 *     <li>打开聊天：完整窗口 —— 消息区 + 分类页签 + 输入框。</li>
 * </ul>
 */
public final class ChatWindowRenderer {
    public static final int LINE_HEIGHT = 11;
    private static final int PADDING = 8;
    private static final int TAB_HEIGHT = 14;
    private static final int TAB_GAP = 3;

    private static ChatCategory filter = ChatCategory.ALL;
    private static int scroll;

    private ChatWindowRenderer() {
    }

    private static long lastFadeLogMs;
    /** 常驻栏是否已经淡出到完全隐藏，用来只在状态翻转时打一条日志。 */
    private static boolean hudHidden;
    /** 本轮是否已经打过「开始淡出」的日志。 */
    private static boolean fadeStarted;

    /**
     * 常驻聊天栏的可见度：最后一条消息（或最后一次打开聊天）之后先保持
     * {@code hudFadeDelayTicks}，再用 {@code hudFadeTicks} 线性淡出到 0。
     * 用墙钟毫秒计时（1 tick = 50ms），不受客户端 tick 源异常影响。
     */
    private static float hudFade() {
        var config = ChatWindowPlus.config();
        int holdTicks = config.hudFadeDelayTicks;
        if (holdTicks < 0) {
            return 1.0F;
        }
        long holdMs = holdTicks * 50L;
        long fadeMs = Math.max(1, config.hudFadeTicks) * 50L;
        long idle = ChatWindowPlus.idleMillis();
        float result;
        if (idle <= holdMs) {
            fadeStarted = false;
            result = 1.0F;
        } else if (idle >= holdMs + fadeMs) {
            result = 0.0F;
        } else {
            if (!fadeStarted) {
                fadeStarted = true;
                ChatWindowPlus.LOGGER.info("[CWP] 常驻聊天栏开始淡出（空闲 {} ms，保持 {} ms）", idle, holdMs);
            }
            result = 1.0F - (idle - holdMs) / (float) fadeMs;
        }
        return result;
    }

    public record Tab(ChatCategory category, int x, int y, int w, int h) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.w && mouseY >= this.y && mouseY < this.y + this.h;
        }
    }

    private record Row(ChatEntry entry, boolean firstLine, OrderedText text) {
    }

    public static ChatCategory filter() {
        return filter;
    }

    public static void setFilter(ChatCategory category) {
        if (category == null) {
            return;
        }
        filter = category;
        scroll = 0;
        ChatWindowPlus.log().markRead(category);
    }

    public static void resetScroll() {
        scroll = 0;
    }

    public static void scrollBy(int lines) {
        scroll += lines;
        if (scroll < 0) {
            scroll = 0;
        }
    }

    public static int chipWidth(TextRenderer font) {
        int width = 0;
        for (ChatCategory category : ChatCategory.tabs()) {
            width = Math.max(width, font.getWidth(category.label()) + 11);
        }
        return Math.max(28, width);
    }

    public static ChatLayout layout(TextRenderer font, int screenW, int screenH, boolean open) {
        var config = ChatWindowPlus.config();
        int panelW = (int) Math.round(screenW * config.widthRatio());
        panelW = MathHelper.clamp(panelW, config.minPanelWidth, config.maxPanelWidth);
        panelW = Math.min(panelW, screenW - 8);
        int panelX = MathHelper.clamp(config.panelOffsetX, 0, Math.max(0, screenW - panelW));
        int chipW = chipWidth(font);

        int listX = panelX + PADDING;
        int listW = panelW - PADDING * 2;
        int textX = listX + chipW + 5;
        int textW = Math.max(40, listX + listW - textX - 6);
        int contentLines = countLines(font, textW);

        if (open) {
            int inputH = 19;
            int gap = 5;
            int tabH = config.showTabs ? TAB_HEIGHT : 0;
            int chrome = PADDING * 2 + tabH + inputH + gap * 2 + 2;
            int ratioRows = Math.max(3, (int) Math.round((screenH * config.openHeightRatio - chrome) / LINE_HEIGHT));
            int rows = MathHelper.clamp(contentLines, 3, Math.max(3, Math.min(config.openMaxLines, ratioRows)));
            int wanted = rows * LINE_HEIGHT + chrome;
            int panelH = Math.min(wanted, (int) Math.round(screenH * config.openHeightRatio));
            int panelY = Math.max(0, screenH - config.panelOffsetY - panelH);
            int inputY = panelY + panelH - PADDING - inputH;
            int tabY = inputY - gap - tabH;
            int listY = panelY + PADDING - 1;
            int listH = Math.max(LINE_HEIGHT * 3, tabY - gap - listY);
            int inputW = panelW - PADDING * 2;
            return new ChatLayout(true, screenW, screenH, panelX, panelY, panelW, panelH,
                    listX, listY, listW, listH, textX, textW, LINE_HEIGHT, chipW,
                    tabY, tabH, listX, inputY, inputW, inputH);
        }

        int rows = MathHelper.clamp(contentLines, 1, Math.max(1, config.hudMaxLines));
        int panelH = rows * LINE_HEIGHT + 16;
        int panelY = Math.max(0, screenH - 28 - config.panelOffsetY - panelH);
        int listY = panelY + 6;
        int hudListW = panelW - 12;
        int hudTextX = panelX + 6 + chipW + 5;
        int hudTextW = Math.max(40, panelX + 6 + hudListW - hudTextX - 4);
        return new ChatLayout(false, screenW, screenH, panelX, panelY, panelW, panelH,
                panelX + 6, listY, hudListW, panelH - 14, hudTextX, hudTextW, LINE_HEIGHT, chipW,
                0, 0, 0, 0, 0, 0);
    }

    private static int countLines(TextRenderer font, int textWidth) {
        int lines = 0;
        for (ChatEntry entry : ChatWindowPlus.log().filtered(filter)) {
            lines += entry.wrapped(font, textWidth).size();
        }
        return lines;
    }
    public static void render(DrawContext context, TextRenderer font, int tick, int mouseX, int mouseY, boolean open) {
        var config = ChatWindowPlus.config();
        if (!config.enabled) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.getChatVisibility().getValue() == ChatVisibility.HIDDEN) {
            return;
        }
        if (!open && !config.hudEnabled) {
            return;
        }

        // 打开聊天时每一帧都在续期；关闭后交给 hudFade 决定“保持 → 淡出 → 隐藏”
        if (open) {
            ChatWindowPlus.noteActivity();
        }
        float fade = open ? 1.0F : hudFade();
        if (!open && fade <= 0.01F) {
            if (!hudHidden) {
                hudHidden = true;
                ChatWindowPlus.LOGGER.info("[CWP] 常驻聊天栏已淡出（空闲 {} ms）", ChatWindowPlus.idleMillis());
            }
            return;
        }
        hudHidden = false;
        if (config.debugChat) {
            long now = System.currentTimeMillis();
            if (now - lastFadeLogMs > 1000L) {
                lastFadeLogMs = now;
                ChatWindowPlus.LOGGER.info("[CWP] hud open={} idle={}ms alpha={} hudEnabled={}",
                        open, ChatWindowPlus.idleMillis(), fade, config.hudEnabled);
            }
        }

        ChatLayout layout = layout(font, context.getScaledWindowWidth(), context.getScaledWindowHeight(), open);
        List<ChatEntry> entries = ChatWindowPlus.log().filtered(filter);
        List<Row> rows = buildRows(font, entries, layout.textW());

        int visibleRows = layout.visibleRows();
        int maxScroll = Math.max(0, rows.size() - visibleRows);
        if (!open) {
            scroll = 0;
        } else {
            scroll = MathHelper.clamp(scroll, 0, maxScroll);
        }
        if (rows.isEmpty()) {
            if (!open) {
                return;
            }
        }

        double opacity = open ? config.openPanelOpacity : config.hudPanelOpacity;
        float alpha = (float) opacity * fade;
        int border = Theme.withAlpha(Theme.PANEL_BORDER, Math.max(0.06F, alpha));
        int radius = MathHelper.clamp(config.cornerRadius, 0, 16);
        UiDraw.shadow(context, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(), radius,
                Theme.withAlpha(0xFF000000, alpha * 0.40F));
        UiDraw.gradientPanel(context, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(), radius,
                Theme.CHAMFER, Theme.withAlpha(Theme.PANEL_TOP, alpha), Theme.withAlpha(Theme.PANEL_BOTTOM, alpha));
        UiDraw.panelBorder(context, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(), radius,
                Theme.CHAMFER, border, Theme.withAlpha(Theme.BORDER_HIGHLIGHT, alpha),
                Theme.withAlpha(Theme.BORDER_SHADE, alpha));
        drawDecor(context, layout.panelX(), layout.panelY(), layout.panelW(), layout.panelH(), alpha);

        if (rows.isEmpty()) {
            context.drawText(font, "暂无聊天记录", layout.listX() + 6, layout.listBottom() - LINE_HEIGHT - 2,
                    Theme.withAlpha(Theme.TEXT_FAINT, fade), false);
        } else {
            drawRows(context, font, layout, rows, visibleRows, mouseX, mouseY, fade);
        }

        if (open) {
            if (config.showTabs) {
                List<Tab> tabs = computeTabs(font, layout);
                context.fill(layout.listX(), layout.tabY() - 5, layout.listX() + layout.listW(), layout.tabY() - 4,
                        0x1FFFFFFF);
                drawTabs(context, font, tabs, mouseX, mouseY);
                drawSettingsButton(context, font, layout, mouseX, mouseY);
            }
            UiDraw.roundRectBordered(context, layout.inputX(), layout.inputY(), layout.inputW(), layout.inputH(), Math.max(0, radius - 2),
                    Theme.withAlpha(Theme.ACCENT, 0.55F),
                    Theme.withAlpha(Theme.INPUT_BG, (float) opacity));
            drawScrollbar(context, layout, rows.size(), visibleRows);
        }
    }

    private static List<Row> buildRows(TextRenderer font, List<ChatEntry> newestFirst, int textWidth) {
        List<Row> rows = new ArrayList<>();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            ChatEntry entry = newestFirst.get(i);
            List<OrderedText> lines = entry.wrapped(font, textWidth);
            for (int line = 0; line < lines.size(); line++) {
                rows.add(new Row(entry, line == 0, lines.get(line)));
            }
        }
        return rows;
    }

    private static int hash(int index, int salt) {
        int h = index * 0x9E3779B9 + salt * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0xC2B2AE3D;
        h ^= h >>> 13;
        return h;
    }

    /** 主题装饰纹理：余烬、星点、花瓣、红石线路、苔藓、晶面等。 */
    public static void drawDecor(DrawContext context, int x, int y, int w, int h, float alpha) {
        String decor = Theme.DECOR;
        if (decor == null || decor.isEmpty() || "none".equals(decor)) {
            return;
        }
        if (w < 20 || h < 20) {
            return;
        }
        int accent = Theme.ACCENT;
        context.enableScissor(x + 2, y + 2, x + w - 2, y + h - 2);
        switch (decor) {
            case "embers" -> {
                for (int i = 0; i < 34; i++) {
                    int px = x + 4 + Math.floorMod(hash(i, 11), Math.max(1, w - 8));
                    int py = y + (int) (h * 0.34F) + Math.floorMod(hash(i, 23), Math.max(1, (int) (h * 0.62F)));
                    int size = 1 + Math.floorMod(hash(i, 37), 2);
                    float a = alpha * (0.35F + (Math.floorMod(hash(i, 41), 48)) / 120F);
                    context.fill(px, py, px + size, py + size, Theme.withAlpha(accent, a));
                }
            }
            case "stars" -> {
                for (int i = 0; i < 36; i++) {
                    int px = x + 4 + Math.floorMod(hash(i, 5), Math.max(1, w - 8));
                    int py = y + 4 + Math.floorMod(hash(i, 7), Math.max(1, h - 8));
                    int color = (i % 5 == 0) ? 0xFFE8E0FF : accent;
                    float a = alpha * (0.30F + (Math.floorMod(hash(i, 9), 42)) / 140F);
                    context.fill(px, py, px + 1, py + 1, Theme.withAlpha(color, a));
                }
            }
            case "petals" -> {
                for (int i = 0; i < 22; i++) {
                    int px = x + 4 + Math.floorMod(hash(i, 13), Math.max(1, w - 8));
                    int py = y + 4 + Math.floorMod(hash(i, 17), Math.max(1, h - 8));
                    float a = alpha * (0.32F + (Math.floorMod(hash(i, 19), 40)) / 130F);
                    context.fill(px, py, px + 2, py + 1, Theme.withAlpha(accent, a));
                }
            }
            case "moss" -> {
                for (int i = 0; i < 26; i++) {
                    int px = x + 3 + Math.floorMod(hash(i, 29), Math.max(1, (int) (w * 0.45F)));
                    int py = y + (int) (h * 0.55F) + Math.floorMod(hash(i, 31), Math.max(1, (int) (h * 0.42F)));
                    float a = alpha * (0.30F + (Math.floorMod(hash(i, 33), 36)) / 150F);
                    context.fill(px, py, px + 2, py + 2, Theme.withAlpha(accent, a));
                }
            }
            case "circuit" -> {
                for (int i = 0; i < 9; i++) {
                    int py = y + 6 + Math.floorMod(hash(i, 3), Math.max(1, h - 12));
                    int len = 18 + Math.floorMod(hash(i, 43), 70);
                    int px = x + 6 + Math.floorMod(hash(i, 47), Math.max(1, w - 12 - len));
                    int color = Theme.withAlpha(accent, alpha * 0.30F);
                    context.fill(px, py, px + len, py + 1, color);
                    context.fill(px + len, py, px + len + 1, py + 7, color);
                    context.fill(px, py - 4, px + 1, py, color);
                }
            }
            case "facets" -> {
                for (int i = 0; i < 10; i++) {
                    int px = x + 6 + Math.floorMod(hash(i, 53), Math.max(1, w - 12));
                    int py = y + 6 + Math.floorMod(hash(i, 59), Math.max(1, h - 12));
                    int len = 8 + Math.floorMod(hash(i, 61), 14);
                    int color = Theme.withAlpha(accent, alpha * 0.26F);
                    for (int s = 0; s < len; s++) {
                        context.fill(px + s, py + s, px + s + 1, py + s + 1, color);
                    }
                }
            }
            case "glints" -> {
                for (int i = 0; i < 14; i++) {
                    int px = x + 6 + Math.floorMod(hash(i, 67), Math.max(1, w - 12));
                    int py = y + 6 + Math.floorMod(hash(i, 71), Math.max(1, h - 12));
                    int color = Theme.withAlpha(accent, alpha * 0.45F);
                    context.fill(px - 1, py, px + 2, py + 1, color);
                    context.fill(px, py - 1, px + 1, py + 2, color);
                }
            }
            case "bubbles" -> {
                for (int i = 0; i < 18; i++) {
                    int px = x + 4 + Math.floorMod(hash(i, 73), Math.max(1, w - 8));
                    int py = y + 4 + Math.floorMod(hash(i, 79), Math.max(1, (int) (h * 0.60F)));
                    int size = 1 + Math.floorMod(hash(i, 83), 2);
                    float a = alpha * (0.22F + (Math.floorMod(hash(i, 89), 30)) / 170F);
                    context.fill(px, py, px + size, py + size, Theme.withAlpha(accent, a));
                }
            }
            default -> {
            }
        }
        context.disableScissor();
    }

    private static void drawRows(DrawContext context, TextRenderer font, ChatLayout layout, List<Row> rows,
                                 int visibleRows, int mouseX, int mouseY, float fade) {
        int total = rows.size();
        int from = firstVisibleRow(rows, visibleRows, scroll);
        int to = Math.min(total, from + visibleRows);
        int x = layout.listX();
        int textX = layout.textX();
        int bottom = layout.listBottom();
        int y = bottom - layout.lineH();

        context.enableScissor(layout.listX(), layout.listY(), layout.listX() + layout.listW(), bottom);
        int hovered = rowIndexAt(layout, rows, mouseX, mouseY);
        for (int index = to - 1; index >= from; index--) {
            Row row = rows.get(index);
            if (index == hovered) {
                context.fill(layout.listX(), y, layout.listX() + layout.listW(), y + layout.lineH(),
                        Theme.withAlpha(0x14FFFFFF, fade));
            }
            if (row.firstLine()) {
                drawChip(context, font, row.entry().category(), x, y + 1, layout.chipW(), fade);
            }
            context.drawText(font, row.text(), textX, y + 1, Theme.withAlpha(0xFFFFFFFF, fade), false);
            y -= layout.lineH();
        }
        context.disableScissor();
    }

    private static void drawChip(DrawContext context, TextRenderer font, ChatCategory category, int x, int y, int width,
                                 float fade) {
        int color = Theme.category(category);
        UiDraw.roundRect(context, x, y, width - 2, 9, 2, Theme.alpha(color, (int) (0x38 * fade)));
        context.drawText(font, category.label(), x + 3, y, Theme.withAlpha(color, fade), false);
    }

    /** 页签行最右侧的设置按钮。 */
    private static int[] settingsRect(ChatLayout layout) {
        return new int[] {layout.listX() + layout.listW() - 34, layout.tabY(), 34, layout.tabH()};
    }

    private static boolean inRect(double mouseX, double mouseY, int[] rect) {
        return mouseX >= rect[0] && mouseX < rect[0] + rect[2] && mouseY >= rect[1] && mouseY < rect[1] + rect[3];
    }

    private static void drawSettingsButton(DrawContext context, TextRenderer font, ChatLayout layout, int mouseX,
                                           int mouseY) {
        int[] rect = settingsRect(layout);
        boolean hover = inRect(mouseX, mouseY, rect);
        if (hover) {
            UiDraw.roundRect(context, rect[0], rect[1], rect[2], rect[3], 3, Theme.withAlpha(Theme.ACCENT, 0.22F));
        }
        Text label = Text.translatable("chatwindowplus.settings.button");
        int width = font.getWidth(label);
        context.drawText(font, label, rect[0] + (rect[2] - width) / 2, rect[1] + 3,
                hover ? Theme.TEXT : Theme.TAB_TEXT, false);
    }

    private static List<Tab> computeTabs(TextRenderer font, ChatLayout layout) {
        List<ChatCategory> categories = new ArrayList<>();
        categories.add(ChatCategory.ALL);
        for (ChatCategory category : ChatCategory.tabs()) {
            categories.add(category);
        }
        int available = layout.listW();
        int pad = 10;
        int gap = TAB_GAP;
        while (tabRowWidth(font, categories, pad, gap) > available && pad > 4) {
            pad -= 2;
        }
        while (tabRowWidth(font, categories, pad, gap) > available && gap > 2) {
            gap--;
        }
        while (tabRowWidth(font, categories, pad, gap) > available && pad > 2) {
            pad--;
        }

        List<Tab> tabs = new ArrayList<>();
        int x = layout.listX();
        for (ChatCategory category : categories) {
            int width = font.getWidth(category.label()) + pad;
            tabs.add(new Tab(category, x, layout.tabY(), width, layout.tabH()));
            x += width + gap;
        }
        return tabs;
    }

    private static int tabRowWidth(TextRenderer font, List<ChatCategory> categories, int pad, int gap) {
        int total = 0;
        for (ChatCategory category : categories) {
            total += font.getWidth(category.label()) + pad + gap;
        }
        return total - gap;
    }

    private static void drawTabs(DrawContext context, TextRenderer font, List<Tab> tabs, int mouseX, int mouseY) {
        for (Tab tab : tabs) {
            boolean active = tab.category() == filter;
            boolean hover = tab.contains(mouseX, mouseY);
            if (active) {
                UiDraw.roundRect(context, tab.x(), tab.y(), tab.w(), tab.h(), 3, Theme.ACCENT);
            } else if (hover) {
                UiDraw.roundRect(context, tab.x(), tab.y(), tab.w(), tab.h(), 3, Theme.withAlpha(Theme.ACCENT, 0.22F));
            }
            int textColor = active ? Theme.TAB_TEXT_ACTIVE : (hover ? Theme.TEXT : Theme.TAB_TEXT);
            context.drawText(font, tab.category().label(), tab.x() + (tab.w() - font.getWidth(tab.category().label())) / 2,
                    tab.y() + 3, textColor, false);
        }
    }

    private static void drawScrollbar(DrawContext context, ChatLayout layout, int total, int visible) {
        if (total <= visible) {
            return;
        }
        int x = layout.listX() + layout.listW() - 3;
        int top = layout.listY();
        int track = layout.listH();
        int thumb = Math.max(10, track * visible / total);
        int maxScroll = total - visible;
        float ratio = maxScroll == 0 ? 1F : 1F - (float) scroll / maxScroll;
        int y = top + Math.round((track - thumb) * ratio);
        context.fill(x, top, x + 2, top + track, Theme.SCROLL_TRACK);
        context.fill(x, y, x + 2, y + thumb, Theme.withAlpha(Theme.ACCENT, 0.55F));
    }

    public static int rowIndexAt(ChatLayout layout, List<Row> rows, double mouseX, double mouseY) {
        if (mouseX < layout.listX() || mouseX >= layout.listX() + layout.listW()
                || mouseY < layout.listY() || mouseY >= layout.listBottom()) {
            return -1;
        }
        int visible = layout.visibleRows();
        int total = rows.size();
        int from = firstVisibleRow(rows, visible, scroll);
        int to = Math.min(total, from + visible);
        int bottom = layout.listBottom();
        int index = to - 1 - (int) ((bottom - mouseY) / layout.lineH());
        return index >= from && index < to ? index : -1;
    }

    /** 滚动时把首行对齐到一条消息的第一行，避免顶部出现半截消息。 */
    private static int firstVisibleRow(List<Row> rows, int visible, int scroll) {
        int total = rows.size();
        int from = Math.max(0, total - visible - scroll);
        while (from > 0 && !rows.get(from).firstLine()) {
            from--;
        }
        return from;
    }

    public static boolean handleClick(double mouseX, double mouseY, TextRenderer font, int screenW, int screenH) {
        ChatLayout layout = layout(font, screenW, screenH, true);
        if (ChatWindowPlus.config().showTabs && inRect(mouseX, mouseY, settingsRect(layout))) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new ChatSettingsScreen());
            return true;
        }
        for (Tab tab : computeTabs(font, layout)) {
            if (tab.contains(mouseX, mouseY)) {
                setFilter(tab.category());
                return true;
            }
        }
        List<ChatEntry> entries = ChatWindowPlus.log().filtered(filter);
        List<Row> rows = buildRows(font, entries, layout.textW());
        int index = rowIndexAt(layout, rows, mouseX, mouseY);
        if (index >= 0) {
            String url = rows.get(index).entry().firstUrl();
            if (url != null) {
                ChatWindowPlus.openUrl(url);
                return true;
            }
        }
        return false;
    }
}