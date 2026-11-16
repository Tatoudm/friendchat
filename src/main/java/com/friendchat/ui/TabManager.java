package com.friendchat.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;

public class TabManager {
    public enum TabType {
        NORMAL,
        FRIENDS,
        MP
    }

    public static TabType activeTab = TabType.NORMAL;
    public static String currentMpTarget = "";

    public static void switchTab(TabType tab) {
        if (tab == TabType.MP && currentMpTarget.isEmpty()) return;
        activeTab = tab;
        refresh();
    }

    public static void setMpTarget(String player) {
        currentMpTarget = player;
        activeTab = TabType.MP;
        refresh();
    }

    public static void closeMp() {
        currentMpTarget = "";
        activeTab = TabType.NORMAL;
        refresh();
    }

    public static void reset() {
        activeTab = TabType.NORMAL;
        currentMpTarget = "";
    }

    public static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui == null) return;
        ChatComponent hud = mc.gui.hud.getChat();
        HistoryManager.refresh(hud);
    }
}
