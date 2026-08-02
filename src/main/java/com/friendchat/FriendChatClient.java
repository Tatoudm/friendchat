package com.friendchat;

import com.friendchat.config.ConfigManager;
import com.friendchat.ui.TabManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

public class FriendChatClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal(ConfigManager.config.friendCmd)
                .then(ClientCommandManager.literal("add")
                    .then(ClientCommandManager.argument("player", StringArgumentType.string())
                        .executes(context -> {
                            String player = StringArgumentType.getString(context, "player");
                            ConfigManager.addFriend(player);
                            context.getSource().sendFeedback(Text.literal("Player added to friends list"));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("player", StringArgumentType.string())
                        .executes(context -> {
                            String player = StringArgumentType.getString(context, "player");
                            ConfigManager.removeFriend(player);
                            context.getSource().sendFeedback(Text.literal("Player removed from friends list"));
                            return 1;
                        }))));

            dispatcher.register(ClientCommandManager.literal(ConfigManager.config.mpCmd)
                .then(ClientCommandManager.argument("player", StringArgumentType.string())
                    .executes(context -> {
                        String player = StringArgumentType.getString(context, "player");
                        TabManager.currentMpTarget = player;
                        TabManager.activeTab = TabManager.TabType.MP;
                        context.getSource().sendFeedback(Text.literal("MP tab focused on " + player));
                        return 1;
                    })));
                    
            dispatcher.register(ClientCommandManager.literal(ConfigManager.config.configCmd)
                .then(ClientCommandManager.argument("commandType", StringArgumentType.string())
                    .then(ClientCommandManager.argument("newValue", StringArgumentType.string())
                        .executes(context -> {
                            String type = StringArgumentType.getString(context, "commandType");
                            String val = StringArgumentType.getString(context, "newValue");
                            if (type.equals("friend")) ConfigManager.config.friendCmd = val;
                            if (type.equals("mp")) ConfigManager.config.mpCmd = val;
                            ConfigManager.saveConfig();
                            context.getSource().sendFeedback(Text.literal("Configuration updated, restart required for commands."));
                            return 1;
                        }))));
        });
    }
}