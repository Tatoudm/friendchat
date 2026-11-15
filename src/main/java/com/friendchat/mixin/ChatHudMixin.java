package com.friendchat.mixin;

import com.friendchat.ui.HistoryManager;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), cancellable = true)
    private void friendchat$onAddMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        if (HistoryManager.bypass) {
            return;
        }
        try {
            HistoryManager.onIncomingMessage((ChatHud) (Object) this, message, signature, indicator);
            ci.cancel();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Inject(method = "removeMessage(Lnet/minecraft/network/message/MessageSignatureData;)V", at = @At("HEAD"))
    private void friendchat$onRemoveMessage(MessageSignatureData signature, CallbackInfo ci) {
        HistoryManager.removeBySignature(signature);
    }
}
