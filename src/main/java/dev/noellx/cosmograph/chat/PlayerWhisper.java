package dev.noellx.cosmograph.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import dev.noellx.cosmograph.Cosmograph;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Handles formatted private messages and the reply target used by {@code /r}. */
public final class PlayerWhisper implements CommandExecutor, TabCompleter {

    private final Cosmograph plugin;
    private final Map<UUID, UUID> replies = new ConcurrentHashMap<>();

    public PlayerWhisper(Cosmograph plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.send(sender, Component.text("Only players can use private messages."));
            return true;
        }
        Player target;
        int messageStart;
        if (label.equalsIgnoreCase("r") || label.equalsIgnoreCase("reply")) {
            UUID targetId = replies.get(player.getUniqueId());
            target = targetId == null ? null : plugin.getServer().getPlayer(targetId);
            messageStart = 0;
            if (target == null) {
                plugin.send(player, Component.text("There is nobody to reply to."));
                return true;
            }
        } else {
            if (args.length < 2) {
                plugin.send(player, Component.text("Usage: /msg <player> <message>"));
                return true;
            }
            target = plugin.getServer().getPlayerExact(args[0]);
            messageStart = 1;
            if (target == null) {
                plugin.send(player, Component.text("That player is not online."));
                return true;
            }
        }
        if (args.length <= messageStart) {
            plugin.send(player, Component.text("Usage: /" + label + " " + (messageStart == 1 ? "<player> " : "") + "<message>"));
            return true;
        }
        if (player.getUniqueId().equals(target.getUniqueId())) {
            plugin.send(player, Component.text("You cannot message yourself."));
            return true;
        }
        String raw = String.join(" ", java.util.Arrays.copyOfRange(args, messageStart, args.length));
        Component message = plugin.getChatFormatService().messageComponent(raw, player.hasPermission("cosmograph.chatcolor"));
        Component formatted = plugin.getChatFormatService().renderWhisper(player, target, message);
        plugin.send(player, formatted);
        plugin.send(target, formatted);
        replies.put(player.getUniqueId(), target.getUniqueId());
        replies.put(target.getUniqueId(), player.getUniqueId());
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1 || alias.equalsIgnoreCase("r")) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        UUID playerId = sender instanceof Player player ? player.getUniqueId() : null;
        return plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> !player.getUniqueId().equals(playerId))
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix)).toList();
    }
}
