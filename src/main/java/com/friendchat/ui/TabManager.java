package com.friendchat.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null) return;
        ChatHud hud = mc.inGameHud.getChatHud();
        HistoryManager.refresh(hud);
    }
}
