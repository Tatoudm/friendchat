package com.friendchat;

import com.friendchat.config.BlockScope;
import com.friendchat.config.ConfigManager;
import com.friendchat.ui.HistoryManager;
import com.friendchat.ui.TabManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FriendChatClient implements ClientModInitializer {

    private static void info(String message) {
        HistoryManager.addLocal(Text.literal("§a[FriendChat]§r " + message));
    }

    private static void error(String message) {
        HistoryManager.addLocal(Text.literal("§c[FriendChat] " + message));
    }

    private static final SuggestionProvider<FabricClientCommandSource> ALL_PLAYER_SUGGESTIONS = (context, builder) -> {
        Set<String> names = new LinkedHashSet<>(context.getSource().getPlayerNames());
        names.addAll(ConfigManager.friends);
        names.addAll(ConfigManager.blocked.keySet());
        names.addAll(HistoryManager.getKnownPlayers());
        for (String name : names) {
            builder.suggest(name);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<FabricClientCommandSource> FRIEND_SUGGESTIONS = (context, builder) -> {
        for (String name : ConfigManager.friends) {
            builder.suggest(name);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<FabricClientCommandSource> SCOPE_SUGGESTIONS = (context, builder) -> {
        for (String s : Arrays.asList("all", "mp", "friend", "normal", "messages")) {
            builder.suggest(s);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<FabricClientCommandSource> CONFIG_KEY_SUGGESTIONS = (context, builder) -> {
        for (String s : Arrays.asList("servermsg", "mpalwaysinfriendstab", "mppublic", "systemmessages", "historysize",
                "friend", "mp", "config", "block", "clearhistory")) {
            builder.suggest(s);
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<FabricClientCommandSource> CONFIG_VALUE_SUGGESTIONS = (context, builder) -> {
        String type;
        try {
            type = StringArgumentType.getString(context, "commandType").toLowerCase();
        } catch (IllegalArgumentException notParsedYet) {
            return builder.buildFuture();
        }
        switch (type) {
            case "mpalwaysinfriendstab":
            case "systemmessages":
            case "mppublic":
                builder.suggest("true");
                builder.suggest("false");
                break;
            case "historysize":
                for (String s : Arrays.asList("100", "300", "500", "1000")) builder.suggest(s);
                break;
            case "servermsg":
                for (String s : Arrays.asList("msg", "tell", "w", "pm", "m", "whisper")) builder.suggest(s);
                break;
            default:
                break;
        }
        return builder.buildFuture();
    };

    private static BlockScope[] scopeFromKeyword(String keyword) {
        switch (keyword.toLowerCase()) {
            case "mp": return new BlockScope[]{BlockScope.MP};
            case "friend": return new BlockScope[]{BlockScope.FRIEND};
            case "normal": return new BlockScope[]{BlockScope.NORMAL};
            case "messages": return new BlockScope[]{BlockScope.FRIEND, BlockScope.NORMAL};
            case "all": return BlockScope.values();
            default: return null;
        }
    }

    private static void refreshChat() {
        TabManager.refresh();
    }

    private static int showConfig(CommandContext<FabricClientCommandSource> context) {
        ConfigManager.ModConfig c = ConfigManager.config;
        info("Configuration actuelle :");
        info(" servermsg = " + c.serverMsgCmd + "  (commande utilisée dans l'onglet MP)");
        info(" mpalwaysinfriendstab = " + c.mpAlwaysInFriendsTab);
        info(" mppublic = " + c.mpTabShowsPublicMessages);
        info(" systemmessages = " + c.systemMessagesInAllTabs);
        info(" historysize = " + c.historySize);
        info(" commandes : /" + c.friendCmd + ", /" + c.mpCmd + ", /" + c.blockCmd + ", /" + c.clearHistoryCmd + ", /" + c.configCmd);
        return 1;
    }

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            HistoryManager.clear();
            TabManager.reset();
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            dispatcher.register(ClientCommandManager.literal(ConfigManager.config.friendCmd)
                .then(ClientCommandManager.literal("add")
                    .then(ClientCommandManager.argument("player", StringArgumentType.string())
                        .suggests(ALL_PLAYER_SUGGESTIONS)
                        .executes(context -> {
                            String player = StringArgumentType.getString(context, "player");
                            if (ConfigManager.addFriend(player)) {
                                info(player + " a été ajouté à la liste d'amis.");
                                refreshChat();
                            } else {
                                error(player + " est déjà dans la liste d'amis.");
                            }
                            return 1;
                        })))
                .then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("player", StringArgumentType.string())
                        .suggests(FRIEND_SUGGESTIONS)
                        .executes(context -> {
                            String player = StringArgumentType.getString(context, "player");
                            if (ConfigManager.removeFriend(player)) {
                                info(player + " a été retiré de la liste d'amis.");
                                refreshChat();
                            } else {
                                error(player + " n'est pas dans la liste d'amis.");
                            }
                            return 1;
                        })))
                .then(ClientCommandManager.literal("list")
                    .executes(context -> {
                        if (ConfigManager.friends.isEmpty()) {
                            info("Aucun ami.");
                        } else {
                            info("Amis (" + ConfigManager.friends.size() + ") : " + String.join(", ", ConfigManager.friends));
                        }
                        return 1;
                    })));

            dispatcher.register(ClientCommandManager.literal(ConfigManager.config.mpCmd)
                .executes(context -> {
                    if (TabManager.currentMpTarget.isEmpty()) {
                        error("Aucune conversation MP active. Utilise /" + ConfigManager.config.mpCmd + " <joueur>.");
                    } else {
                        TabManager.switchTab(TabManager.TabType.MP);
                    }
                    return 1;
                })
                .then(ClientCommandManager.literal("close")
                    .executes(context -> {
                        TabManager.closeMp();
                        info("Conversation MP fermée.");
                        return 1;
                    }))
                .then(ClientCommandManager.argument("player", StringArgumentType.string())
                    .suggests(ALL_PLAYER_SUGGESTIONS)
                    .executes(context -> {
                        String player = StringArgumentType.getString(context, "player");
                        TabManager.setMpTarget(player);
                        info("Onglet MP centré sur " + player + ". Tes messages seront envoyés avec /"
                                + ConfigManager.config.serverMsgCmd + " " + player + " ...");
                        return 1;
                    })));

            dispatcher.register(ClientCommandManager.literal(ConfigManager.config.clearHistoryCmd)
                .executes(context -> {
                    HistoryManager.clear();
                    TabManager.refresh();
                    info("Historique de chat vidé.");
                    return 1;
                }));

            dispatcher.register(ClientCommandManager.literal(ConfigManager.config.configCmd)
                .executes(FriendChatClient::showConfig)
                .then(ClientCommandManager.argument("commandType", StringArgumentType.word())
                    .suggests(CONFIG_KEY_SUGGESTIONS)
                    .then(ClientCommandManager.argument("newValue", StringArgumentType.word())
                        .suggests(CONFIG_VALUE_SUGGESTIONS)
                        .executes(context -> {
                            String type = StringArgumentType.getString(context, "commandType").toLowerCase();
                            String val = StringArgumentType.getString(context, "newValue");
                            boolean requiresRestart = false;
                            boolean needsRefresh = false;

                            switch (type) {
                                case "friend":
                                    ConfigManager.config.friendCmd = val;
                                    requiresRestart = true;
                                    break;
                                case "mp":
                                    ConfigManager.config.mpCmd = val;
                                    requiresRestart = true;
                                    break;
                                case "config":
                                    ConfigManager.config.configCmd = val;
                                    requiresRestart = true;
                                    break;
                                case "block":
                                    ConfigManager.config.blockCmd = val;
                                    requiresRestart = true;
                                    break;
                                case "clearhistory":
                                    ConfigManager.config.clearHistoryCmd = val;
                                    requiresRestart = true;
                                    break;
                                case "servermsg":
                                    ConfigManager.config.serverMsgCmd = val.startsWith("/") ? val.substring(1) : val;
                                    break;
                                case "mpalwaysinfriendstab":
                                    ConfigManager.config.mpAlwaysInFriendsTab = Boolean.parseBoolean(val);
                                    needsRefresh = true;
                                    break;
                                case "mppublic":
                                    ConfigManager.config.mpTabShowsPublicMessages = Boolean.parseBoolean(val);
                                    needsRefresh = true;
                                    break;
                                case "systemmessages":
                                    ConfigManager.config.systemMessagesInAllTabs = Boolean.parseBoolean(val);
                                    needsRefresh = true;
                                    break;
                                case "historysize":
                                    try {
                                        int size = Integer.parseInt(val.trim());
                                        ConfigManager.config.historySize = Math.max(1, size);
                                    } catch (NumberFormatException ex) {
                                        error("Valeur invalide pour historysize: " + val);
                                        return 0;
                                    }
                                    break;
                                default:
                                    error("Clé de configuration inconnue: " + type);
                                    return 0;
                            }

                            ConfigManager.saveConfig();
                            if (needsRefresh) refreshChat();
                            info("Configuration mise à jour (" + type + " = " + val + ")."
                                    + (requiresRestart ? " Redémarrage nécessaire pour ce changement." : ""));
                            return 1;
                        }))));

            dispatcher.register(ClientCommandManager.literal(ConfigManager.config.blockCmd)
                .then(ClientCommandManager.literal("list")
                    .executes(context -> {
                        if (ConfigManager.blocked.isEmpty()) {
                            info("Aucun joueur bloqué.");
                        } else {
                            for (Map.Entry<String, Set<BlockScope>> entry : ConfigManager.blocked.entrySet()) {
                                info(entry.getKey() + " -> " + entry.getValue());
                            }
                        }
                        return 1;
                    }))
                .then(ClientCommandManager.argument("player", StringArgumentType.string())
                    .suggests(ALL_PLAYER_SUGGESTIONS)
                    .executes(context -> {
                        String player = StringArgumentType.getString(context, "player");
                        ConfigManager.block(player);
                        refreshChat();
                        info(player + " est maintenant totalement bloqué (mp, friend, normal).");
                        return 1;
                    })
                    .then(ClientCommandManager.literal("unblock")
                        .executes(context -> {
                            String player = StringArgumentType.getString(context, "player");
                            ConfigManager.unblock(player);
                            refreshChat();
                            info(player + " a été débloqué (tous niveaux).");
                            return 1;
                        })
                        .then(ClientCommandManager.argument("scope", StringArgumentType.word())
                            .suggests(SCOPE_SUGGESTIONS)
                            .executes(context -> {
                                String player = StringArgumentType.getString(context, "player");
                                String scopeKw = StringArgumentType.getString(context, "scope");
                                BlockScope[] scopes = scopeFromKeyword(scopeKw);
                                if (scopes == null) {
                                    error("Niveau inconnu: " + scopeKw + " (all/mp/friend/normal/messages)");
                                    return 0;
                                }
                                ConfigManager.unblock(player, scopes);
                                refreshChat();
                                info(player + " débloqué pour: " + scopeKw);
                                return 1;
                            })))
                    .then(ClientCommandManager.argument("scope", StringArgumentType.word())
                        .suggests(SCOPE_SUGGESTIONS)
                        .executes(context -> {
                            String player = StringArgumentType.getString(context, "player");
                            String scopeKw = StringArgumentType.getString(context, "scope");
                            BlockScope[] scopes = scopeFromKeyword(scopeKw);
                            if (scopes == null) {
                                error("Niveau inconnu: " + scopeKw + " (all/mp/friend/normal/messages)");
                                return 0;
                            }
                            ConfigManager.block(player, scopes);
                            refreshChat();
                            info(player + " bloqué pour: " + scopeKw);
                            return 1;
                        }))));
        });
    }
}
