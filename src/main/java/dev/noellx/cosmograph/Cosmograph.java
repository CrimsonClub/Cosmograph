package dev.noellx.cosmograph;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import dev.noellx.cosmograph.chat.ChatFormatService;
import dev.noellx.cosmograph.chat.EmojiReplacer;
import dev.noellx.cosmograph.chat.ItemPlaceholder;
import dev.noellx.cosmograph.chat.MentionService;
import dev.noellx.cosmograph.chat.UrlLinkifier;
import dev.noellx.cosmograph.commands.CosmographCommand;
import dev.noellx.cosmograph.listener.AsyncChatListener;
import dev.noellx.cosmograph.listener.ConnectionListener;
import dev.noellx.cosmograph.listener.SpigotChatListener;
import dev.noellx.cosmograph.chat.PlayerWhisper;
import dev.noellx.cosmograph.scheduler.Scheduler;
import dev.noellx.cosmograph.scheduler.Schedulers;

public final class Cosmograph extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private boolean paper;
    private boolean folia;
    private Scheduler scheduler;
    private ChatFormatService chatFormatService;
    private PlayerWhisper playerWhisper;
    private EmojiReplacer emojiReplacer;
    private UrlLinkifier urlLinkifier;
    private MentionService mentionService;

    public static LegacyComponentSerializer getLegacySerializer() {
        return LEGACY_SERIALIZER;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.paper = detectPaper();
        this.folia = detectFolia();
        this.scheduler = Schedulers.create(this);
        this.chatFormatService = new ChatFormatService(this);
        this.playerWhisper = new PlayerWhisper(this);
        this.emojiReplacer = new EmojiReplacer(this);
        this.urlLinkifier = new UrlLinkifier(this);
        this.mentionService = new MentionService(this);
        registerCommand();
        registerListeners();
        logRuntimePlatform();
    }

    /** Logs the detected server + Java version — the single universal jar runs on many, so make
     *  the actual runtime platform visible for support. */
    private void logRuntimePlatform() {
        getLogger().info("Running on " + getServer().getName() + " (API " + getServer().getBukkitVersion()
                + ") on Java " + System.getProperty("java.version")
                + (paper ? " [Paper/Adventure chat]" : " [Spigot/legacy chat]")
                + (folia ? " [Folia]" : ""));
    }

    /**
     * Tells a player why their {@code [item]} did not resolve, when {@code use-item-placeholder} is
     * enabled but they lack the {@code cosmograph.itemplaceholder} permission. Called once per chat message
     * (not per viewer), so it never spams.
     */
    public void maybeItemPlaceholderHint(Player player, String message) {
        if (!getConfig().getBoolean("use-item-placeholder", false)) {
            return;
        }
        if (player.hasPermission("cosmograph.itemplaceholder")) {
            return;
        }
        if (!ItemPlaceholder.containsToken(message)) {
            return;
        }
        send(player, MiniMessage.miniMessage().deserialize(
                "<dark_gray>[<gradient:#B754F4:#FC00FF>LPC</gradient>] <yellow>Ask an admin for the "
                        + "<white>cosmograph.itemplaceholder</white> permission to use <white>[item]</white> in chat."));
    }

    public boolean isPaper() {
        return paper;
    }

    public boolean isFolia() {
        return folia;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.cancelAll();
        }
    }

    public ChatFormatService getChatFormatService() {
        return chatFormatService;
    }

    public EmojiReplacer getEmojiReplacer() {
        return emojiReplacer;
    }

    public UrlLinkifier getUrlLinkifier() {
        return urlLinkifier;
    }

    public MentionService getMentionService() {
        return mentionService;
    }

    /** Re-reads config-derived state for every service. Call after {@code reloadConfig()}. */
    public void reloadServices() {
        chatFormatService.reload();
        emojiReplacer.reload();
        urlLinkifier.reload();
        mentionService.reload();
    }

    /** @return whether chat formatting is disabled in the given world. */
    public boolean isDisabledWorld(String worldName) {
        for (String world : getConfig().getStringList("disabled-worlds")) {
            if (world.equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        return false;
    }

    /** Resolves a player's display name as a component on either platform. */
    @SuppressWarnings("deprecation") // getDisplayName() is the Spigot fallback
    public Component displayNameOf(Player player) {
        return paper ? player.displayName() : LEGACY_SERIALIZER.deserialize(player.getDisplayName());
    }

    /** Sends a component to a sender, falling back to legacy text on Spigot. */
    public void send(CommandSender target, Component component) {
        if (paper) {
            target.sendMessage(component);
        } else {
            target.sendMessage(LEGACY_SERIALIZER.serialize(component));
        }
    }

    private void registerCommand() {
        PluginCommand command = getCommand("cosmograph");
        if (command == null) {
            getLogger().warning("Command 'cosmograph' is missing from plugin.yml; commands are unavailable.");
            return;
        }
        CosmographCommand executor = new CosmographCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        PluginCommand msg = getCommand("msg");
        PluginCommand reply = getCommand("r");
        if (msg != null) {
            msg.setExecutor(playerWhisper);
            msg.setTabCompleter(playerWhisper);
        }
        if (reply != null) {
            reply.setExecutor(playerWhisper);
            reply.setTabCompleter(playerWhisper);
        }
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        if (paper) {
            pluginManager.registerEvents(new AsyncChatListener(this), this);
        } else {
            pluginManager.registerEvents(new SpigotChatListener(this), this);
        }
        pluginManager.registerEvents(new ConnectionListener(this), this);
    }

    private boolean detectPaper() {
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            getLogger().info("Paper API detected — using Adventure chat rendering.");
            return true;
        } catch (ClassNotFoundException notPaper) {
            getLogger().info("Spigot API detected — using legacy chat rendering.");
            return false;
        }
    }

    private boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            getLogger().info("Folia detected — using regionized scheduling.");
            return true;
        } catch (ClassNotFoundException notFolia) {
            return false;
        }
    }
}
