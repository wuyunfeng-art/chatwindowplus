package com.chatwindowplus.ui;

import com.chatwindowplus.ChatWindowPlus;
import com.chatwindowplus.config.ChatWindowConfig;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * 游戏内聊天窗口设置：位置、大小、透明度、颜色、风格都可以在这里实时调整，
 * 左侧的原版聊天栏就是实时预览。按 F7 或输入 /cwp 也可以打开。
 */
public final class ChatSettingsScreen extends Screen {
    private static final int ROW = 21;

    private ChatWindowConfig config;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private TextFieldWidget panelColorField;
    private TextFieldWidget textColorField;
    private ButtonWidget themeButton;
    private int themeIndex;
    private int colorLabelY;
    private boolean syncing;

    public ChatSettingsScreen() {
        super(Text.translatable("chatwindowplus.settings.title"));
    }

    @Override
    protected void init() {
        this.config = ChatWindowPlus.config();
        this.panelW = Math.min(360, this.width - 24);
        this.panelH = Math.min(302, this.height - 24);
        this.panelX = this.width - this.panelW - 12;
        this.panelY = Math.max(12, (this.height - this.panelH) / 2);

        int x = this.panelX + 12;
        int w = this.panelW - 24;
        int y = this.panelY + 34;

        addSlider(x, y, w, "面板宽度", 0.20, 0.60, this.config.panelWidthRatio,
                v -> this.config.panelWidthRatio = v, v -> Math.round(v * 100) + "%");
        y += ROW;
        addSlider(x, y, w, "窗口高度", 0.25, 0.75, this.config.openHeightRatio,
                v -> this.config.openHeightRatio = v, v -> Math.round(v * 100) + "%");
        y += ROW;
        addSlider(x, y, w, "背景不透明度", 0.05, 1.00, this.config.openPanelOpacity,
                v -> {
                    this.config.openPanelOpacity = v;
                    this.config.hudPanelOpacity = v;
                }, v -> Math.round(v * 100) + "%");
        y += ROW;
        addSlider(x, y, w, "圆角", 0.0, 16.0, this.config.cornerRadius,
                v -> this.config.cornerRadius = (int) Math.round(v), v -> String.valueOf(Math.round(v)));
        y += ROW;
        addSlider(x, y, w, "横向位置", 0.0, 300.0, this.config.panelOffsetX,
                v -> this.config.panelOffsetX = (int) Math.round(v), v -> Math.round(v) + "px");
        y += ROW;
        addSlider(x, y, w, "纵向位置", 0.0, 200.0, this.config.panelOffsetY,
                v -> this.config.panelOffsetY = (int) Math.round(v), v -> Math.round(v) + "px");
        y += ROW;
        addSlider(x, y, w, "常驻行数", 3.0, 16.0, this.config.hudMaxLines,
                v -> this.config.hudMaxLines = (int) Math.round(v), v -> String.valueOf(Math.round(v)));
        y += ROW;

        addToggle(x, y, w / 2 - 2, "页签", this.config.showTabs, value -> this.config.showTabs = value);
        addToggle(x + w / 2 + 2, y, w / 2 - 2, "称号标签", this.config.showTags,
                value -> this.config.showTags = value);
        y += ROW;
        addToggle(x, y, w / 2 - 2, "常驻聊天栏", this.config.hudEnabled,
                value -> this.config.hudEnabled = value);
        this.themeButton = addButton(x + w / 2 + 2, y, w / 2 - 2, "主题：" + currentThemeName(), b -> cycleTheme());
        y += ROW;

        this.colorLabelY = y;
        this.panelColorField = addColorField(x + w - 100, y, 100, this.config.panelColor, true);
        y += ROW;
        this.textColorField = addColorField(x + w - 100, y, 100, this.config.textColor, false);
        y += ROW + 4;

        addButton(x, y, w / 2 - 2, "恢复默认", b -> resetDefaults());
        addButton(x + w / 2 + 2, y, w / 2 - 2, "完成", b -> this.close());
    }

    private void addSlider(int x, int y, int w, String label, double min, double max, double value,
                           DoubleConsumer apply, DoubleFunction<String> format) {
        this.addDrawableChild(new OptionSlider(x, y, w, 18, label, min, max, value, apply, format));
    }

    private void addToggle(int x, int y, int w, String label, boolean initial, java.util.function.Consumer<Boolean> apply) {
        boolean[] state = {initial};
        ButtonWidget button = ButtonWidget.builder(toggleLabel(label, state[0]), b -> {
            state[0] = !state[0];
            apply.accept(state[0]);
            b.setMessage(toggleLabel(label, state[0]));
            ChatWindowPlus.markDirty();
        }).dimensions(x, y, w, 18).build();
        this.addDrawableChild(button);
    }

    private static Text toggleLabel(String label, boolean on) {
        return Text.literal(label + "：" + (on ? "开" : "关"));
    }

    private ButtonWidget addButton(int x, int y, int w, String label, ButtonWidget.PressAction action) {
        return this.addDrawableChild(ButtonWidget.builder(Text.literal(label), action).dimensions(x, y, w, 18).build());
    }

    private TextFieldWidget addColorField(int x, int y, int w, String value, boolean panelBackground) {
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y, w, 18, Text.literal("颜色"));
        field.setMaxLength(7);
        field.setText(value);
        field.setChangedListener(text -> {
            if (this.syncing) {
                return;
            }
            if (!text.matches("#?[0-9a-fA-F]{6}")) {
                return;
            }
            String hex = text.startsWith("#") ? text : "#" + text;
            if (panelBackground) {
                this.config.panelColor = hex;
                this.config.panelColorTop = hex;
                this.config.panelColorBottom = hex;
            } else {
                this.config.textColor = hex;
            }
            Theme.apply(this.config);
            if (!panelBackground) {
                ChatWindowPlus.log().rebuildAll();
            }
            refreshThemeButton();
            ChatWindowPlus.markDirty();
        });
        this.addDrawableChild(field);
        return field;
    }

    private static int themeIndexOf(ChatWindowConfig config) {
        return ThemePresets.indexOf(config.panelColor, config.borderColor, config.textColor,
                config.nameColor, config.tagColor, config.accentColor, config.panelColorTop,
                config.panelColorBottom, config.decor, config.chamfer, config.categoryColors);
    }

    private String currentThemeName() {
        int index = themeIndexOf(this.config);
        return index >= 0 ? ThemePresets.ALL.get(index).name() : "自定义";
    }

    private void refreshThemeButton() {
        if (this.themeButton != null) {
            this.themeButton.setMessage(Text.literal("主题：" + currentThemeName()));
        }
    }

    private void cycleTheme() {
        int current = themeIndexOf(this.config);
        this.themeIndex = (current + 1) % ThemePresets.ALL.size();
        ThemePresets.Preset theme = ThemePresets.ALL.get(this.themeIndex);
        this.config.panelColor = theme.panel();
        this.config.borderColor = theme.border();
        this.config.textColor = theme.text();
        this.config.nameColor = theme.nameColor();
        this.config.tagColor = theme.tag();
        this.config.accentColor = theme.accent();
        this.config.panelColorTop = theme.top();
        this.config.panelColorBottom = theme.bottom();
        this.config.decor = theme.decor();
        this.config.chamfer = theme.chamfer();
        this.config.categoryColors = ThemePresets.copyCategories(theme);
        Theme.apply(this.config);
        ChatWindowPlus.log().rebuildAll();
        ChatWindowPlus.markDirty();
        this.syncing = true;
        try {
            this.panelColorField.setText(this.config.panelColor);
            this.textColorField.setText(this.config.textColor);
        } finally {
            this.syncing = false;
        }
        refreshThemeButton();
    }

    private void resetDefaults() {
        ChatWindowConfig defaults = new ChatWindowConfig();
        this.config.copyFrom(defaults);
        Theme.apply(this.config);
        ChatWindowPlus.log().rebuildAll();
        ChatWindowPlus.markDirty();
        this.client.setScreen(new ChatSettingsScreen());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, this.width, this.height, 0x50000000);
        UiDraw.gradientPanel(context, this.panelX, this.panelY, this.panelW, this.panelH, 8, Theme.CHAMFER,
                Theme.withAlpha(Theme.PANEL_TOP, 0.94F), Theme.withAlpha(Theme.PANEL_BOTTOM, 0.94F));
        UiDraw.panelBorder(context, this.panelX, this.panelY, this.panelW, this.panelH, 8, Theme.CHAMFER,
                Theme.PANEL_BORDER, Theme.BORDER_HIGHLIGHT, Theme.BORDER_SHADE);
        ChatWindowRenderer.drawDecor(context, this.panelX, this.panelY, this.panelW, this.panelH, 0.94F);
        context.drawText(this.textRenderer, this.title, this.panelX + 12, this.panelY + 10, Theme.NAME, false);
        context.drawText(this.textRenderer, Text.translatable("chatwindowplus.settings.hint"),
                this.panelX + 12, this.panelY + 22, Theme.TEXT_FAINT, false);
        context.drawText(this.textRenderer, Text.literal("背景色"), this.panelX + 16, this.colorLabelY + 5,
                Theme.TEXT_DIM, false);
        context.drawText(this.textRenderer, Text.literal("文字色"), this.panelX + 16, this.colorLabelY + ROW + 5,
                Theme.TEXT_DIM, false);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // 不画模糊背景，保留游戏画面作为预览
    }

    @Override
    public void close() {
        this.config.save();
        super.close();
    }

    private final class OptionSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleConsumer apply;
        private final DoubleFunction<String> format;

        OptionSlider(int x, int y, int width, int height, String label, double min, double max, double value,
                     DoubleConsumer apply, DoubleFunction<String> format) {
            super(x, y, width, height, Text.literal(label), MathHelper.clamp((value - min) / (max - min), 0.0, 1.0));
            this.label = label;
            this.min = min;
            this.max = max;
            this.apply = apply;
            this.format = format;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(this.label + "：" + this.format.apply(this.min + this.value * (this.max - this.min))));
        }

        @Override
        protected void applyValue() {
            this.apply.accept(this.min + this.value * (this.max - this.min));
            ChatWindowPlus.markDirty();
        }
    }
}