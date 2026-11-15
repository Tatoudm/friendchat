package com.friendchat.mixin;

import com.friendchat.config.ConfigManager;
import com.friendchat.ui.HistoryManager;
import com.friendchat.ui.TabManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    private static final int TAB_HEIGHT = 11;
    private static final int TAB_GAP = 2;
    private static final int TAB_PADDING = 5;
    private static final int TAB_START_X = 2;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void friendchat$onSendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (TabManager.activeTab != TabManager.TabType.MP || TabManager.currentMpTarget.isEmpty()) return;

        String text = chatText.trim();
        if (text.isEmpty() || text.startsWith("/")) return;

        ClientPlayNetworkHandler handler = this.client.getNetworkHandler();
        if (handler == null) return;

        String cmd = ConfigManager.config.serverMsgCmd.trim();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        handler.sendChatCommand(cmd + " " + TabManager.currentMpTarget + " " + text);
        if (addToHistory) {
            this.client.inGameHud.getChatHud().addToMessageHistory(text);
        }
        ci.cancel();
    }

    private String[] friendchat$labels() {
        String mp = TabManager.currentMpTarget.isEmpty() ? "DM" : "DM: " + TabManager.currentMpTarget;
        return new String[]{"General", "Friends", mp};
    }

    private static final TabManager.TabType[] TABS = {
            TabManager.TabType.NORMAL, TabManager.TabType.FRIENDS, TabManager.TabType.MP
    };

    private int[][] friendchat$bounds() {
        String[] labels = friendchat$labels();
        int[][] bounds = new int[labels.length][2];
        int x = TAB_START_X;
        for (int i = 0; i < labels.length; i++) {
            int w = this.textRenderer.getWidth(labels[i]) + TAB_PADDING * 2;
            bounds[i][0] = x;
            bounds[i][1] = x + w;
            x += w + TAB_GAP;
        }
        return bounds;
    }

    private int friendchat$tabY() {
        return this.height - 38;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void friendchat$onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0) return;

        double mouseX = click.x();
        double mouseY = click.y();
        int y = friendchat$tabY();
        if (mouseY < y || mouseY > y + TAB_HEIGHT) return;

        int[][] bounds = friendchat$bounds();
        for (int i = 0; i < bounds.length; i++) {
            if (mouseX >= bounds[i][0] && mouseX <= bounds[i][1]) {
                if (TABS[i] == TabManager.TabType.MP && TabManager.currentMpTarget.isEmpty()) {
                    HistoryManager.addLocal(Text.literal("§7No active DM conversation. Use §f/"
                            + ConfigManager.config.mpCmd + " <player>§7."));
                } else {
                    TabManager.switchTab(TABS[i]);
                }
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void friendchat$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        String[] labels = friendchat$labels();
        int[][] bounds = friendchat$bounds();
        int y = friendchat$tabY();

        for (int i = 0; i < labels.length; i++) {
            boolean active = TabManager.activeTab == TABS[i];
            boolean hovered = mouseX >= bounds[i][0] && mouseX <= bounds[i][1] && mouseY >= y && mouseY <= y + TAB_HEIGHT;
            boolean disabled = TABS[i] == TabManager.TabType.MP && TabManager.currentMpTarget.isEmpty();

            int background = active ? 0xB0000000 : 0x70000000;
            context.fill(bounds[i][0], y, bounds[i][1], y + TAB_HEIGHT, background);
            if (active) {
                context.fill(bounds[i][0], y + TAB_HEIGHT - 1, bounds[i][1], y + TAB_HEIGHT, 0xFF55FF55);
            }

            int color = active ? 0xFF55FF55 : disabled ? 0xFF777777 : hovered ? 0xFFFFFFFF : 0xFFBBBBBB;
            context.drawText(this.textRenderer, labels[i], bounds[i][0] + TAB_PADDING, y + 2, color, true);
        }
    }
}
