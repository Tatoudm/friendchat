package com.friendchat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "friendchat_config.json");
    private static final File FRIENDS_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "friendchat_friends.json");

    public static ModConfig config = new ModConfig();
    public static List<String> friends = new ArrayList<>();

    public static class ModConfig {
        public String friendCmd = "chatfriend";
        public String mpCmd = "chatmp";
        public String configCmd = "friendchatconfig";
        public String serverMsgCmd = "msg";
    }

    public static void load() {
        try {
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    config = GSON.fromJson(reader, ModConfig.class);
                }
            } else {
                saveConfig();
            }

            if (FRIENDS_FILE.exists()) {
                try (FileReader reader = new FileReader(FRIENDS_FILE)) {
                    friends = GSON.fromJson(reader, List.class);
                }
            } else {
                saveFriends();
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public static void addFriend(String name) {
        if (!friends.contains(name)) {
            friends.add(name);
            saveFriends();
        }
    }

    public static void removeFriend(String name) {
        friends.remove(name);
        saveFriends();
    }
}