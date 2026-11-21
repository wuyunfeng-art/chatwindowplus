package com.chatwindowplus;

import com.chatwindowplus.chat.ChatCategory;
import com.chatwindowplus.chat.ChatLog;
import com.chatwindowplus.config.ChatWindowConfig;
import com.chatwindowplus.ui.ChatSettingsScreen;
import com.chatwindowplus.ui.ChatWindowRenderer;
import com.chatwindowplus.ui.Theme;
import java.net.URI;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chat Window Plus —— 重新设计的 Minecraft 聊天窗口。
 */
public final class ChatWindowPlus implements ClientModInitializer {
    public static final String MOD_ID = "chatwindowplus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ChatWindowPlus instance;

    private ChatWindowConfig config;
    private final ChatLog chatLog = new ChatLog();
    private long configStamp;
    private boolean dirty;

    /** 最后一次「有动静」时的 tick：收到消息或打开聊天时刷新，常驻栏据此淡出。 */
    private int lastActivityTick;
    /** 客户端 tick 计数，和渲染线程无关，专门给淡出计时用。 */
    private int ticks;

    private KeyBinding settingsKey;

    @Override
    public void onInitializeClient() {
        instance = this;
        this.config = ChatWindowConfig.load();
        this.configStamp = ChatWindowConfig.lastModified();
        Theme.apply(this.config);
        ChatWindowRenderer.setFilter(parseFilter(this.config.defaultFilter));

        this.settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chatwindowplus.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                KeyBinding.Category.create(Identifier.of(MOD_ID, "settings"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            this.ticks++;
            while (this.settingsKey.wasPressed()) {
                client.setScreen(new ChatSettingsScreen());
            }
            if (this.dirty && client.inGameHud.getTicks() % 100 == 0) {
                this.config.save();
                this.dirty = false;
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal("cwp").executes(context -> {
                    MinecraftClient.getInstance().setScreen(new ChatSettingsScreen());
                    return 1;
                })));

        PreviewHarness.register();
        LOGGER.info("Chat Window Plus 已加载");
    }

    public static ChatWindowPlus get() {
        return instance;
    }

    public static ChatWindowConfig config() {
        return instance == null ? new ChatWindowConfig() : instance.config;
    }

    public static ChatLog log() {
        return instance == null ? new ChatLog() : instance.chatLog;
    }

    /** 收到消息或打开聊天时调用，重置常驻栏的淡出计时。 */
    public static void noteActivity() {
        if (instance != null) {
            instance.lastActivityTick = instance.ticks;
        }
    }

    public static int lastActivityTick() {
        return instance == null ? 0 : instance.lastActivityTick;
    }

    public static int ticks() {
        return instance == null ? 0 : instance.ticks;
    }

    /** 设置界面改动后调用：稍后自动落盘。 */
    public static void markDirty() {
        if (instance != null) {
            instance.dirty = true;
        }
    }

    /** 打开聊天窗口/发送消息时顺手检查配置是否有改动。 */
    public static void reloadIfChanged() {
        if (instance == null) {
            return;
        }
        long stamp = ChatWindowConfig.lastModified();
        if (stamp != instance.configStamp) {
            instance.configStamp = stamp;
            instance.config = ChatWindowConfig.load();
            Theme.apply(instance.config);
            instance.chatLog.rebuildAll();
            ChatWindowRenderer.setFilter(parseFilter(instance.config.defaultFilter));
            LOGGER.info("已重新加载 chatwindowplus.json");
        }
    }

    public static ChatCategory parseFilter(String name) {
        if (name == null) {
            return ChatCategory.ALL;
        }
        try {
            return ChatCategory.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ChatCategory.ALL;
        }
    }

    public static String selfName() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.getSession() == null ? null : client.getSession().getUsername();
    }

    public static void openUrl(String url) {
        try {
            Util.getOperatingSystem().open(new URI(url));
        } catch (Exception e) {
            LOGGER.warn("无法打开链接 {}: {}", url, e.getMessage());
        }
    }
}