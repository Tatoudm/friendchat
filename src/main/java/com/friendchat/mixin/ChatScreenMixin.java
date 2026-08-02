package com.friendchat.mixin;

import com.friendchat.config.ConfigManager;
import com.friendchat.ui.TabManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (!chatText.startsWith("/") && TabManager.activeTab == TabManager.TabType.MP && !TabManager.currentMpTarget.isEmpty()) {
            String newCommand = ConfigManager.config.serverMsgCmd + " " + TabManager.currentMpTarget + " " + chatText;
            this.client.getNetworkHandler().sendChatCommand(newCommand);  
            this.client.inGameHud.getChatHud().addToMessageHistory(chatText);
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();

        int startX = 2;
        int startY = this.height - 40;
        int tabWidth = 50;
        int tabHeight = 15;

        if (mouseY >= startY && mouseY <= startY + tabHeight) {
            if (mouseX >= startX && mouseX <= startX + tabWidth) {
                TabManager.activeTab = TabManager.TabType.NORMAL;
                cir.setReturnValue(true);
            } else if (mouseX >= startX + tabWidth + 2 && mouseX <= startX + (tabWidth * 2) + 2) {
                TabManager.activeTab = TabManager.TabType.FRIENDS;
                cir.setReturnValue(true);
            } else if (mouseX >= startX + (tabWidth * 2) + 4 && mouseX <= startX + (tabWidth * 3) + 4) {
                if (!TabManager.currentMpTarget.isEmpty()) {
                    TabManager.activeTab = TabManager.TabType.MP;
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void onRender(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int startX = 2;
        int startY = this.height - 40;
        int tabWidth = 50;

        int normalColor = TabManager.activeTab == TabManager.TabType.NORMAL ? 0xFF00FF00 : 0xFFFFFFFF;
        context.drawText(this.textRenderer, "Normal", startX, startY, normalColor, true);

        int friendColor = TabManager.activeTab == TabManager.TabType.FRIENDS ? 0xFF00FF00 : 0xFFFFFFFF;
        context.drawText(this.textRenderer, "Friends", startX + tabWidth + 2, startY, friendColor, true);

        if (!TabManager.currentMpTarget.isEmpty()) {
            int mpColor = TabManager.activeTab == TabManager.TabType.MP ? 0xFF00FF00 : 0xFFFFFFFF;
            context.drawText(this.textRenderer, "MP: " + TabManager.currentMpTarget, startX + (tabWidth * 2) + 4, startY, mpColor, true);
        }
    }
}