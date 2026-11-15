package com.friendchat.config;

import com.friendchat.ui.HistoryManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "friendchat_config.json");
    private static final File FRIENDS_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "friendchat_friends.json");
    private static final File BLOCKED_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "friendchat_blocked.json");

    public static ModConfig config = new ModConfig();
    public static List<String> friends = new ArrayList<>();
    public static Map<String, Set<BlockScope>> blocked = new HashMap<>();

    public static class ModConfig {
        public String friendCmd = "chatfriend";
        public String mpCmd = "chatmp";
        public String configCmd = "friendchatconfig";
        public String blockCmd = "chatblock";
        public String clearHistoryCmd = "chatclearhistory";

        public String serverMsgCmd = "msg";

        public boolean mpAlwaysInFriendsTab = false;

        public boolean mpTabShowsPublicMessages = false;

        public boolean systemMessagesInAllTabs = false;

        public int historySize = 300;

        public List<String> alwaysShowPatterns = new ArrayList<>();

        public List<String> chatPatterns = new ArrayList<>(Arrays.asList(
                "(?s)^\\s*<(?<player>[^>]+)>.*$",
                "(?s)^(?:\\[[^\\]]{1,32}\\]\\s*)*(?<player>[A-Za-z0-9_]{3,16})\\s*(?::|»|›|>>)\\s.*$"
        ));

        public List<String> mpPatterns = new ArrayList<>(Arrays.asList(
                "(?is)^(?<player>[A-Za-z0-9_]{1,16}) whispers to you: .*$",
                "(?is)^You whisper to (?<player>[A-Za-z0-9_]{1,16}): .*$",
                "(?is)^\\[(?<player>[A-Za-z0-9_]{1,16}) -> .*?\\].*$",
                "(?is)^\\[.*? -> (?<player>[A-Za-z0-9_]{1,16})\\].*$"
        ));
    }

    public static void load() {
        try {
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                    if (loaded != null) config = loaded;
                }
            }
            sanitizeConfig();
            saveConfig();

            if (FRIENDS_FILE.exists()) {
                try (FileReader reader = new FileReader(FRIENDS_FILE)) {
                    Type type = new TypeToken<List<String>>() {}.getType();
                    List<String> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) friends = new ArrayList<>(loaded);
                }
            } else {
                saveFriends();
            }

            if (BLOCKED_FILE.exists()) {
                try (FileReader reader = new FileReader(BLOCKED_FILE)) {
                    Type type = new TypeToken<Map<String, Set<BlockScope>>>() {}.getType();
                    Map<String, Set<BlockScope>> loaded = GSON.fromJson(reader, type);
                    if (loaded != null) blocked = new HashMap<>(loaded);
                }
            } else {
                saveBlocked();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        HistoryManager.reloadPatterns();
    }

    private static void sanitizeConfig() {
        ModConfig d = new ModConfig();
        if (config.friendCmd == null || config.friendCmd.isBlank()) config.friendCmd = d.friendCmd;
        if (config.mpCmd == null || config.mpCmd.isBlank()) config.mpCmd = d.mpCmd;
        if (config.configCmd == null || config.configCmd.isBlank()) config.configCmd = d.configCmd;
        if (config.blockCmd == null || config.blockCmd.isBlank()) config.blockCmd = d.blockCmd;
        if (config.clearHistoryCmd == null || config.clearHistoryCmd.isBlank()) config.clearHistoryCmd = d.clearHistoryCmd;
        if (config.serverMsgCmd == null || config.serverMsgCmd.isBlank()) config.serverMsgCmd = d.serverMsgCmd;
        if (config.alwaysShowPatterns == null) config.alwaysShowPatterns = new ArrayList<>();
        if (config.chatPatterns == null || config.chatPatterns.isEmpty()) config.chatPatterns = d.chatPatterns;
        if (config.mpPatterns == null || config.mpPatterns.isEmpty()) config.mpPatterns = d.mpPatterns;
        if (config.historySize < 1) config.historySize = d.historySize;
        for (int i = 0; i < config.chatPatterns.size(); i++) {
            if ("(?s)^<(?<player>[A-Za-z0-9_]{1,16})> .*$".equals(config.chatPatterns.get(i))) {
                config.chatPatterns.set(i, d.chatPatterns.get(0));
            }
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveFriends() {
        try (FileWriter writer = new FileWriter(FRIENDS_FILE)) {
            GSON.toJson(friends, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveBlocked() {
        try (FileWriter writer = new FileWriter(BLOCKED_FILE)) {
            GSON.toJson(blocked, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean addFriend(String name) {
        if (isFriend(name)) return false;
        friends.add(name);
        saveFriends();
        return true;
    }

    public static boolean removeFriend(String name) {
        boolean removed = friends.removeIf(f -> f.equalsIgnoreCase(name));
        if (removed) saveFriends();
        return removed;
    }

    public static boolean isFriend(String name) {
        if (name == null) return false;
        for (String f : friends) {
            if (f.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private static String findBlockedKey(String player) {
        if (player == null) return null;
        for (String key : blocked.keySet()) {
            if (key.equalsIgnoreCase(player)) return key;
        }
        return null;
    }

    public static void block(String player, BlockScope... scopes) {
        String key = findBlockedKey(player);
        if (key == null) key = player;
        Set<BlockScope> set = blocked.computeIfAbsent(key, k -> EnumSet.noneOf(BlockScope.class));
        if (scopes.length == 0) {
            set.addAll(Arrays.asList(BlockScope.values()));
        } else {
            set.addAll(Arrays.asList(scopes));
        }
        saveBlocked();
    }

    public static void unblock(String player, BlockScope... scopes) {
        String key = findBlockedKey(player);
        if (key == null) return;
        if (scopes.length == 0) {
            blocked.remove(key);
        } else {
            Set<BlockScope> set = blocked.get(key);
            set.removeAll(Arrays.asList(scopes));
            if (set.isEmpty()) blocked.remove(key);
        }
        saveBlocked();
    }

    public static boolean isBlocked(String player, BlockScope scope) {
        return getBlockedScopes(player).contains(scope);
    }

    public static Set<BlockScope> getBlockedScopes(String player) {
        String key = findBlockedKey(player);
        if (key == null) return Collections.emptySet();
        return blocked.get(key);
    }
}
