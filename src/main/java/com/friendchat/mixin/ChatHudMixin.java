package com.friendchat.mixin;

import com.friendchat.config.ConfigManager;
import com.friendchat.ui.TabManager;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, net.minecraft.network.message.MessageSignatureData signature, net.minecraft.client.gui.hud.MessageIndicator indicator, CallbackInfo ci) {
        String rawText = message.getString();
        
        if (TabManager.activeTab == TabManager.TabType.FRIENDS) {
            boolean isFriend = false;
            for (String friend : ConfigManager.friends) {
                if (rawText.contains(friend)) {
                    isFriend = true;
                    break;
                }
            }
            if (!isFriend) {
                ci.cancel();
            }
        }
        
        if (TabManager.activeTab == TabManager.TabType.MP) {
            if (!TabManager.currentMpTarget.isEmpty() && !rawText.contains(TabManager.currentMpTarget)) {
                ci.cancel();
            }
        }
    }
}