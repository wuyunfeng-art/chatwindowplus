package com.chatwindowplus.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 把原版 {@link Screen#handleClickEvent} 暴露出来。
 * 聊天里的点击（包括 Paletale 这类服务端 UI 用的自定义事件）统一交给原版分发，
 * 保证「原版能点的，这里也能点」。
 */
@Mixin(Screen.class)
public interface ScreenClickInvoker {

    @Invoker("handleClickEvent")
    static void cwp$handleClickEvent(ClickEvent event, MinecraftClient client, Screen screen) {
        throw new AssertionError("mixin 未应用");
    }
}