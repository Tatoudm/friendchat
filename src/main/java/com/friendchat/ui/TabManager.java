package com.friendchat.ui;

public class TabManager {
    public enum TabType {
        NORMAL,
        FRIENDS,
        MP
    }

    public static TabType activeTab = TabType.NORMAL;
    public static String currentMpTarget = "";
}