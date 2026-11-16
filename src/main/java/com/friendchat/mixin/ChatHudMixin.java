package com.friendchat.mixin;

import com.friendchat.ui.HistoryManager;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatHudMixin {

    @Inject(method = "addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void friendchat$onAddPlayerMessage(Component message, MessageSignature signature, GuiMessageTag indicator, CallbackInfo ci) {
        if (HistoryManager.bypass) {
            return;
        }
        try {
            HistoryManager.onIncomingPlayerMessage((ChatComponent) (Object) this, message, signature, indicator);
            ci.cancel();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Inject(method = "addServerSystemMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"), cancellable = true)
    private void friendchat$onAddServerSystemMessage(Component message, CallbackInfo ci) {
        if (HistoryManager.bypass) {
            return;
        }
        try {
            HistoryManager.onIncomingServerSystemMessage((ChatComponent) (Object) this, message);
            ci.cancel();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Inject(method = "addClientSystemMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"), cancellable = true)
    private void friendchat$onAddClientSystemMessage(Component message, CallbackInfo ci) {
        if (HistoryManager.bypass) {
            return;
        }
        try {
            HistoryManager.onIncomingClientSystemMessage((ChatComponent) (Object) this, message);
            ci.cancel();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Inject(method = "deleteMessage(Lnet/minecraft/network/chat/MessageSignature;)V", at = @At("HEAD"))
    private void friendchat$onRemoveMessage(MessageSignature signature, CallbackInfo ci) {
        HistoryManager.removeBySignature(signature);
    }
}
