package dev.noellx.cosmograph.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import dev.noellx.cosmograph.Cosmograph;

import java.util.List;

public class CosmographCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "version", "help");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Cosmograph plugin;

    public CosmographCommand(Cosmograph plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "version" -> handleVersion(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("cosmograph.reload")) {
            plugin.send(sender, mini("<red>You don't have permission to do that."));
            return;
        }
        plugin.reloadConfig();
        plugin.reloadServices();
        String raw = plugin.getConfig().getString("reload-message", "<green>Reloaded Cosmograph configuration!");
        plugin.send(sender, mini(raw));
    }

    @SuppressWarnings("deprecation") // getDescription() is cross-platform
    private void handleVersion(CommandSender sender) {
        String platform = plugin.isFolia() ? "Folia" : plugin.isPaper() ? "Paper" : "Spigot";
        plugin.send(sender, mini("<gradient:#B754F4:#FC00FF>Cosmograph</gradient> <gray>v<white>"
                + plugin.getDescription().getVersion() + "</white> <dark_gray>— <gray>MiniMessage chat formatter."));
        java.util.Properties build = readBuildInfo();
        plugin.send(sender, mini("<dark_gray>Build: <gray>compiled for Minecraft <white>"
                + build.getProperty("minecraft", "?") + "</white> <dark_gray>(Java "
                + build.getProperty("java", "?") + " · Adventure " + build.getProperty("adventure", "?") + ")"));
        plugin.send(sender, mini("<dark_gray>Running on: <white>" + platform + " "
                + plugin.getServer().getBukkitVersion() + "</white> <dark_gray>(Java "
                + System.getProperty("java.version") + ")"));
    }

    /** Reads the build-time target descriptor baked into the jar (or empty on failure). */
    private static java.util.Properties readBuildInfo() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream in = CosmographCommand.class.getResourceAsStream("/cosmograph-build.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
            // missing/unreadable build info is non-fatal
        }
        return props;
    }

    private void sendHelp(CommandSender sender) {
        plugin.send(sender, mini("<gradient:#B754F4:#FC00FF>Cosmograph</gradient> <gray>commands:"));
        plugin.send(sender, mini("<dark_gray>- <white>/cosmograph reload</white> <dark_gray>» <gray>Reload the configuration"));
        plugin.send(sender, mini("<dark_gray>- <white>/cosmograph version</white> <dark_gray>» <gray>Show the plugin version"));
    }

    private static Component mini(String raw) {
        return MM.deserialize(raw);
    }

    private static Component mini(String raw, String key, String value) {
        return MM.deserialize(raw, Placeholder.unparsed(key, value));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUBCOMMANDS.stream().filter(sub -> sub.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
