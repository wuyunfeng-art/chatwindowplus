package com.chatwindowplus.mixin;

import com.chatwindowplus.ChatWindowPlus;
import com.chatwindowplus.ui.ChatLayout;
import com.chatwindowplus.ui.ChatWindowRenderer;
import com.chatwindowplus.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把原版聊天输入界面重排成分类聊天窗口：输入框贴到底部面板、页签可点、滚轮翻历史。
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    private static MinecraftClient client() {
        return MinecraftClient.getInstance();
    }

    private static TextRenderer font() {
        return client().textRenderer;
    }

    private static int screenWidth() {
        return client().getWindow().getScaledWidth();
    }

    private static int screenHeight() {
        return client().getWindow().getScaledHeight();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void chatwindowplus$layout(CallbackInfo ci) {
        ChatWindowPlus.reloadIfChanged();
        if (!ChatWindowPlus.config().enabled || this.chatField == null) {
            return;
        }
        ChatWindowRenderer.resetScroll();
        placeField();
    }

    private void placeField() {
        if (this.chatField == null) {
            return;
        }
        ChatLayout layout = ChatWindowRenderer.layout(font(), screenWidth(), screenHeight(), true);
        int width = Math.max(24, layout.inputW() - 14);
        int x = layout.inputX() + 7;
        int y = layout.inputY() + 4;
        if (this.chatField.getX() != x || this.chatField.getY() != y || this.chatField.getWidth() != width) {
            this.chatField.setWidth(width);
            this.chatField.setHeight(12);
            this.chatField.setX(x);
            this.chatField.setY(y);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void chatwindowplus$placeFieldBeforeRender(DrawContext context, int mouseX, int mouseY, float deltaTicks,
                                                       CallbackInfo ci) {
        if (ChatWindowPlus.config().enabled) {
            placeField();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void chatwindowplus$placeholder(DrawContext context, int mouseX, int mouseY, float deltaTicks,
                                            CallbackInfo ci) {
        if (!ChatWindowPlus.config().enabled || this.chatField == null || !this.chatField.getText().isEmpty()) {
            return;
        }
        ChatLayout layout = ChatWindowRenderer.layout(font(), screenWidth(), screenHeight(), true);
        Text placeholder = Text.translatable("chatwindowplus.input.placeholder");
        context.drawText(font(), placeholder, layout.inputX() + 7, layout.inputY() + 4, Theme.PLACEHOLDER, false);
    }

    /** 原版会在屏幕底部铺一条全宽的黑色输入条，改画成我们面板内的圆角输入框。 */
    @Redirect(method = "render", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void chatwindowplus$skipVanillaInputBar(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        if (!ChatWindowPlus.config().enabled) {
            context.fill(x1, y1, x2, y2, color);
        }
    }

    @Redirect(method = "mouseClicked", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/font/DrawnTextConsumer;IIZ)V"))
    private void chatwindowplus$skipVanillaTextHitTest(ChatHud hud, DrawnTextConsumer consumer, int windowHeight,
                                                       int currentTick, boolean expanded) {
        if (!ChatWindowPlus.config().enabled) {
            hud.render(consumer, windowHeight, currentTick, expanded);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void chatwindowplus$click(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!ChatWindowPlus.config().enabled || click.button() != 0) {
            return;
        }
        if (ChatWindowRenderer.handleClick(click.x(), click.y(), font(), screenWidth(), screenHeight())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void chatwindowplus$scroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!ChatWindowPlus.config().enabled) {
            return;
        }
        double amount = Math.max(-1.0, Math.min(1.0, verticalAmount));
        if (!client().isShiftPressed()) {
            amount *= 7.0;
        }
        ChatWindowRenderer.scrollBy((int) Math.round(amount));
        cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void chatwindowplus$key(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!ChatWindowPlus.config().enabled) {
            return;
        }
        if (input.key() == 266 || input.key() == 267) {
            ChatLayout layout = ChatWindowRenderer.layout(font(), screenWidth(), screenHeight(), true);
            int step = Math.max(1, layout.visibleRows() - 1);
            ChatWindowRenderer.scrollBy(input.key() == 266 ? step : -step);
            cir.setReturnValue(true);
        }
    }
}