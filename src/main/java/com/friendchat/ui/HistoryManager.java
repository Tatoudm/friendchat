package com.friendchat.ui;

import com.friendchat.config.BlockScope;
import com.friendchat.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryManager {

    public static boolean bypass = false;

    private static final int MAX_REPLAY = 100;

    private enum Kind {
        LOCAL,
        SYSTEM,
        CHAT,
        MP
    }

    private static final Deque<BufferedMessage> buffer = new ArrayDeque<>();

    private static final Set<String> knownPlayers = new LinkedHashSet<>();
    private static final int MAX_KNOWN_PLAYERS = 200;

    private static List<Pattern> mpPatterns = new ArrayList<>();
    private static List<Pattern> chatPatterns = new ArrayList<>();
    private static List<Pattern> alwaysShowPatterns = new ArrayList<>();

    private static class BufferedMessage {
        final Text text;
        final MessageSignatureData signature;
        final MessageIndicator indicator;
        final String rawText;
        final Kind kind;
        final boolean forced;
        final String player;

        BufferedMessage(Text text, MessageSignatureData signature, MessageIndicator indicator,
                        String rawText, Kind kind, boolean forced, String player) {
            this.text = text;
            this.signature = signature;
            this.indicator = indicator;
            this.rawText = rawText;
            this.kind = kind;
            this.forced = forced;
            this.player = player;
        }
    }

    public static void onIncomingMessage(ChatHud hud, Text message, MessageSignatureData signature, MessageIndicator indicator) {
        String rawText = message.getString();

        String mpPartner = firstGroup(mpPatterns, rawText);
        String sender = mpPartner == null ? firstGroup(chatPatterns, rawText) : null;

        Kind kind;
        String player;
        if (mpPartner != null) {
            kind = Kind.MP;
            player = mpPartner;
        } else if (sender != null) {
            kind = Kind.CHAT;
            player = sender;
        } else {
            kind = Kind.SYSTEM;
            player = null;
        }
        if (player != null) rememberPlayer(player);

        boolean forced = matchesAny(alwaysShowPatterns, rawText);
        BufferedMessage bm = new BufferedMessage(message, signature, indicator, rawText, kind, forced, player);

        if (player != null && isFullyBlocked(player)) return;

        store(bm);
        if (passesFilter(TabManager.activeTab, bm)) {
            display(hud, bm);
        }
    }

    public static void addLocal(Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null) return;
        BufferedMessage bm = new BufferedMessage(text, null, null, text.getString(), Kind.LOCAL, true, null);
        store(bm);
        display(mc.inGameHud.getChatHud(), bm);
    }

    public static void removeBySignature(MessageSignatureData signature) {
        if (signature == null) return;
        buffer.removeIf(bm -> bm.signature != null && Objects.equals(bm.signature, signature));
    }

    private static void store(BufferedMessage bm) {
        buffer.addLast(bm);
        int max = Math.max(1, ConfigManager.config.historySize);
        while (buffer.size() > max) {
            buffer.pollFirst();
        }
    }

    public static void refresh(ChatHud hud) {
        hud.clear(false);
        hud.resetScroll();

        List<BufferedMessage> toShow = new ArrayList<>();
        for (BufferedMessage bm : buffer) {
            if (passesFilter(TabManager.activeTab, bm)) toShow.add(bm);
        }
        int from = Math.max(0, toShow.size() - MAX_REPLAY);
        for (int i = from; i < toShow.size(); i++) {
            display(hud, toShow.get(i));
        }
    }

    private static void display(ChatHud hud, BufferedMessage bm) {
        bypass = true;
        try {
            hud.addMessage(bm.text, bm.signature, bm.indicator);
        } finally {
            bypass = false;
        }
    }

    private static boolean passesFilter(TabManager.TabType tab, BufferedMessage bm) {
        if (bm.kind == Kind.LOCAL || bm.forced) return true;

        switch (tab) {
            case NORMAL: {
                if (bm.player == null) return true;
                return !ConfigManager.isBlocked(bm.player, BlockScope.NORMAL);
            }
            case FRIENDS: {
                boolean included;
                switch (bm.kind) {
                    case CHAT: included = ConfigManager.isFriend(bm.player); break;
                    case MP: included = ConfigManager.isFriend(bm.player) || ConfigManager.config.mpAlwaysInFriendsTab; break;
                    default: included = ConfigManager.config.systemMessagesInAllTabs || mentionsFriend(bm.rawText); break;
                }
                if (!included) return false;
                return bm.player == null || !ConfigManager.isBlocked(bm.player, BlockScope.FRIEND);
            }
            case MP: {
                String target = TabManager.currentMpTarget;
                if (target.isEmpty()) return false;
                switch (bm.kind) {
                    case MP:
                        if (!target.equalsIgnoreCase(bm.player)) return false;
                        return !ConfigManager.isBlocked(target, BlockScope.MP);
                    case CHAT:
                        return ConfigManager.config.mpTabShowsPublicMessages
                                && target.equalsIgnoreCase(bm.player)
                                && !ConfigManager.isBlocked(target, BlockScope.MP);
                    default:
                        return ConfigManager.config.systemMessagesInAllTabs;
                }
            }
        }
        return true;
    }

    private static boolean isFullyBlocked(String player) {
        Set<BlockScope> scopes = ConfigManager.getBlockedScopes(player);
        return scopes.contains(BlockScope.MP) && scopes.contains(BlockScope.FRIEND) && scopes.contains(BlockScope.NORMAL);
    }

    private static boolean mentionsFriend(String rawText) {
        for (String friend : ConfigManager.friends) {
            Pattern p = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(friend) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE);
            if (p.matcher(rawText).find()) return true;
        }
        return false;
    }

    public static void reloadPatterns() {
        mpPatterns = compile(ConfigManager.config.mpPatterns);
        chatPatterns = compile(ConfigManager.config.chatPatterns);
        alwaysShowPatterns = compile(ConfigManager.config.alwaysShowPatterns);
    }

    private static List<Pattern> compile(List<String> sources) {
        List<Pattern> out = new ArrayList<>();
        if (sources == null) return out;
        for (String s : sources) {
            try {
                out.add(Pattern.compile(s));
            } catch (Exception badRegex) {
            }
        }
        return out;
    }

    private static boolean matchesAny(List<Pattern> patterns, String text) {
        for (Pattern p : patterns) {
            if (p.matcher(text).matches()) return true;
        }
        return false;
    }

    private static String firstGroup(List<Pattern> patterns, String text) {
        for (Pattern p : patterns) {
            Matcher m = p.matcher(text);
            if (!m.matches()) continue;
            String player = null;
            try {
                player = m.group("player");
            } catch (IllegalArgumentException noNamedGroup) {
                if (m.groupCount() >= 1) player = m.group(1);
            }
            if (player != null && !player.isEmpty()) return cleanName(player);
        }
        return null;
    }

    private static final Pattern PLAIN_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private static String cleanName(String raw) {
        String name = raw.trim();
        if (PLAIN_NAME.matcher(name).matches()) return name;
        Matcher m = PLAIN_NAME.matcher(name);
        String last = null;
        while (m.find()) last = m.group();
        return last != null ? last : name;
    }

    private static void rememberPlayer(String name) {
        knownPlayers.remove(name);
        knownPlayers.add(name);
        while (knownPlayers.size() > MAX_KNOWN_PLAYERS) {
            knownPlayers.remove(knownPlayers.iterator().next());
        }
    }

    public static Set<String> getKnownPlayers() {
        return knownPlayers;
    }

    public static void clear() {
        buffer.clear();
    }
}
