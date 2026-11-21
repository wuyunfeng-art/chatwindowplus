package com.chatwindowplus;

import com.chatwindowplus.chat.ChatCategory;
import com.chatwindowplus.ui.ChatSettingsScreen;
import com.chatwindowplus.ui.ChatWindowRenderer;
import com.chatwindowplus.ui.Theme;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;

/**
 * 开发期自动测试装置：
 * <ul>
 *     <li>{@code -Dcwo.preview=true}：标题界面直接预览窗口样式；</li>
 *     <li>{@code -Dcwo.chattest=true}：连接本地测试服后打开真实 ChatScreen 并截图。</li>
 * </ul>
 */
public final class PreviewHarness {
    private static final int MODE_OFF = 0;
    private static final int MODE_PREVIEW = 1;
    private static final int MODE_CHAT_TEST = 2;

    private static int mode = MODE_OFF;
    private static boolean resized;
    private static int stage;
    private static int idle;

    private PreviewHarness() {
    }

    public static void register() {
        if (Boolean.getBoolean("cwo.preview")) {
            mode = MODE_PREVIEW;
        } else if (Boolean.getBoolean("cwo.chattest")) {
            mode = MODE_CHAT_TEST;
        }
        if (mode == MODE_OFF) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(PreviewHarness::tick);
    }

    public static void tick(MinecraftClient client) {
        if (mode == MODE_PREVIEW) {
            previewTick(client);
        } else if (mode == MODE_CHAT_TEST) {
            chatTestTick(client);
        }
    }

    private static void previewTick(MinecraftClient client) {
        idle++;
        if (client.currentScreen == null || client.getOverlay() != null) {
            return;
        }
        if (!resized) {
            resized = true;
            client.getWindow().setWindowedSize(1600, 900);
            client.options.getGuiScale().setValue(2);
            try {
                client.getLanguageManager().setLanguage("zh_cn");
            } catch (Exception e) {
                ChatWindowPlus.LOGGER.warn("切换中文失败: {}", e.getMessage());
            }
            client.reloadResources();
            seed();
            return;
        }
        stage++;
        switch (stage) {
            case 20 -> client.setScreen(new PreviewScreen(true));
            case 50 -> screenshot(client, "cwo-preview-open");
            case 60 -> {
                try {
                    client.setScreen(new ChatScreen("", false));
                } catch (Throwable t) {
                    ChatWindowPlus.LOGGER.error("无法在标题界面打开 ChatScreen（预览跳过）", t);
                    client.setScreen(new PreviewScreen(true));
                }
            }
            case 90 -> screenshot(client, "cwo-preview-chatscreen");
            case 100 -> client.setScreen(new PreviewScreen(false));
            case 130 -> screenshot(client, "cwo-preview-hud");
            case 145 -> client.scheduleStop();
            default -> {
            }
        }
    }

    private static void chatTestTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        if (!resized) {
            resized = true;
            ChatWindowPlus.LOGGER.info("chattest: 已进入世界，开始测试");
            client.getWindow().setWindowedSize(2560, 1600);
            client.options.getGuiScale().setValue(3);
            seed();
            return;
        }
        stage++;
        switch (stage) {
            case 30 -> client.setScreen(new ChatScreen("", false));
            case 55 -> screenshot(client, "chat-open");
            case 60 -> ChatWindowRenderer.scrollBy(6);
            case 70 -> screenshot(client, "chat-scrolled");
            case 75 -> {
                ChatWindowRenderer.resetScroll();
                ChatWindowRenderer.setFilter(ChatCategory.WORLD);
            }
            case 90 -> screenshot(client, "chat-world-tab");
            case 95 -> ChatWindowRenderer.setFilter(ChatCategory.ALL);
            case 100 -> client.setScreen(new ChatSettingsScreen());
            case 120 -> screenshot(client, "chat-settings");
            case 125 -> clickThemeButton(client);
            case 135 -> screenshot(client, "chat-settings-theme");
            case 140 -> applyPreset(client, "末地虚空");
            case 160 -> screenshot(client, "chat-settings-theme2");
            case 165 -> client.setScreen(null);
            case 175 -> screenshot(client, "chat-hud-ingame");
            case 190 -> client.scheduleStop();
            default -> {
            }
        }
    }

    private static void applyPreset(MinecraftClient client, String name) {
        for (com.chatwindowplus.ui.ThemePresets.Preset preset : com.chatwindowplus.ui.ThemePresets.ALL) {
            if (!preset.name().equals(name)) {
                continue;
            }
            ChatWindowPlus.config().panelColor = preset.panel();
            ChatWindowPlus.config().panelColorTop = preset.top();
            ChatWindowPlus.config().panelColorBottom = preset.bottom();
            ChatWindowPlus.config().borderColor = preset.border();
            ChatWindowPlus.config().textColor = preset.text();
            ChatWindowPlus.config().nameColor = preset.nameColor();
            ChatWindowPlus.config().tagColor = preset.tag();
            ChatWindowPlus.config().accentColor = preset.accent();
            ChatWindowPlus.config().decor = preset.decor();
            ChatWindowPlus.config().chamfer = preset.chamfer();
            ChatWindowPlus.config().categoryColors = com.chatwindowplus.ui.ThemePresets.copyCategories(preset);
            com.chatwindowplus.ui.Theme.apply(ChatWindowPlus.config());
            ChatWindowPlus.log().rebuildAll();
            client.setScreen(new ChatSettingsScreen());
            return;
        }
    }

    private static void clickThemeButton(MinecraftClient client) {
        int panelW = Math.min(360, client.getWindow().getScaledWidth() - 24);
        int panelX = client.getWindow().getScaledWidth() - panelW - 12;
        int panelH = Math.min(302, client.getWindow().getScaledHeight() - 24);
        int panelY = Math.max(12, (client.getWindow().getScaledHeight() - panelH) / 2);
        int x = panelX + 12;
        int w = panelW - 24;
        int y = panelY + 34 + 7 * 21 + 21;
        double cx = x + w / 2 + 2 + (w / 2 - 2) / 2.0;
        double cy = y + 9;
        if (client.currentScreen != null) {
            client.currentScreen.mouseClicked(
                    new net.minecraft.client.gui.Click(cx, cy,
                            new net.minecraft.client.input.MouseInput(0, 0)), false);
        }
    }

    private static void screenshot(MinecraftClient client, String name) {
        ScreenshotRecorder.saveScreenshot(client.runDirectory, name + ".png", client.getFramebuffer(), 1,
                message -> ChatWindowPlus.LOGGER.info("screenshot: {}", message.getString()));
    }

    private static void seed() {
        String[] samples = {
                "[太空] [G]  [史黛少年团♥我们喜欢你♥]  Huo_long : 今晚8点 河港篝火活动，记得来",
                "[G]  [流浪者]  ChengCingSyuan : 2",
                "[交易]  [奥术大师]  lzbtr> 叫卖 刚铎蓝湾商业港 533.531 钻石镐只需7000一把，接了附魔装备批量订购",
                "隐秘的声音>> 新的RP故事接口已经发布！ https://docs.qq.com/aio/DZXd2eVBYc1ZuWGVs",
                "[流浪者] |miao_yan试图在熔岩里游泳",
                "[私聊] 阿澈悄悄地对你说：在吗，来河港",
                "[附近] 阿澈：前方是领地边界，别走过去",
                "[G]  [SIMMC最萌萌之人]  xiaochen0604 : 我今晚也去！",
                "BLUEJHON加入了游戏",
                "[交易]  [讨口子の魔女]  tao_kou_zi_Baize> 叫卖 灰原港全品类商店，圆石/羊毛无限收购",
                "[G]  [轻云初见]  |NaPigeon_622从高处摔了下来",
                "[G]  [流浪者]  Lamb_0102 : 我爱SIMMC",
                "[交易]  [奥术大师]  lzbtr> 叫卖 210w收秘典，300w收冰龙娘，量大从优",
                "隐秘的声音>>【苍白·一周年庆典】将于本周六晚8点开场！参加RP需自备装备武器",
                "[附近] 阿澈：别往枫叶林深处走，那边有猎手",
                "[G]  [太空]  sishi2 : 噩梦剑3=1速刷",
                "[私聊] 阿澈悄悄地对你说：篝火晚会你来吗？我带了两瓶酒",
                "[G]  [史黛少年团♥我们喜欢你♥]  Huo_long : 来来来，港口集合",
                "[流浪者] |12138yidayida被冰之德鲁伊-猫头鹰杀死了",
                "[交易]  [讨口子の魔女]  tao_kou_zi_Baize> 叫卖 全新源能熔炉 2.1w 一个，陶瓦无限收",
                "[G]  [BLG]  CHEN_XIONG : 1",
                "CookiePotion加入了游戏"
        };
        for (String sample : samples) {
            ChatWindowPlus.log().add(Text.literal(sample));
        }
    }

    /** 仅用于视觉预览的假屏幕。 */
    public static final class PreviewScreen extends Screen {
        private final boolean open;

        public PreviewScreen(boolean open) {
            super(Text.literal("ChatWindowPlus Preview"));
            this.open = open;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            context.fill(0, 0, this.width, this.height, 0xFF23303E);
            context.fill(0, this.height - 60, this.width, this.height, 0xFF1A2431);
            ChatWindowRenderer.render(context, this.textRenderer, 0, mouseX, mouseY, this.open);
            if (this.open) {
                context.drawText(this.textRenderer, Text.translatable("chatwindowplus.input.placeholder"),
                        4 + 8 + 7, this.height - 6 - 8 - 19 + 5, 0xFF6B7E93, false);
            }
            context.drawText(this.textRenderer, "preview: " + (this.open ? "open" : "hud"), 8, 8, 0xFFFFFFFF, true);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}