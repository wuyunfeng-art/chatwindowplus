package com.chatwindowplus.mixin;

import com.chatwindowplus.ChatWindowPlus;
import com.chatwindowplus.ui.ChatWindowRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 接管原版聊天栏：捕获新消息并改画成分类聊天窗口。
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("TAIL"))
    private void chatwindowplus$capture(Text message, MessageSignatureData signature, MessageIndicator indicator,
                                        CallbackInfo ci) {
        ChatWindowPlus.log().add(message);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
            at = @At("HEAD"), cancellable = true)
    private void chatwindowplus$render(DrawContext context, TextRenderer textRenderer, int currentTick, int mouseX,
                                       int mouseY, boolean interactable, boolean bl, CallbackInfo ci) {
        if (!ChatWindowPlus.config().enabled) {
            return;
        }
        ChatWindowRenderer.render(context, textRenderer, currentTick, mouseX, mouseY, interactable);
        ci.cancel();
    }
}