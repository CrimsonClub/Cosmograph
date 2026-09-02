package dev.noellx.cosmograph.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.noellx.cosmograph.Cosmograph;
import dev.noellx.cosmograph.chat.ChatFormatService;

/**
 * Replaces the vanilla join / quit / first-join / death messages with operator-authored MiniMessage
 * templates. These events fire on the main thread, so config is read directly. No player chat text
 * is involved; the death cause is injected as a pre-built component via a placeholder.
 */
public class ConnectionListener implements Listener {

    private final Cosmograph plugin;
    private final ChatFormatService service;

    public ConnectionListener(Cosmograph plugin) {
        this.plugin = plugin;
        this.service = plugin.getChatFormatService();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("join-messages.enabled", false)) {
            return;
        }
        Player player = event.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore()
                && plugin.getConfig().getBoolean("join-messages.first-join.enabled", false);
        String template = firstJoin
                ? plugin.getConfig().getString("join-messages.first-join.format", "")
                : plugin.getConfig().getString("join-messages.format", "");

        Component message = renderOrNull(player, template);
        if (plugin.isPaper()) {
            event.joinMessage(message);
        } else {
            event.setJoinMessage(legacyOrNull(message));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("quit-messages.enabled", false)) {
            return;
        }
        Component message = renderOrNull(event.getPlayer(), plugin.getConfig().getString("quit-messages.format", ""));
        if (plugin.isPaper()) {
            event.quitMessage(message);
        } else {
            event.setQuitMessage(legacyOrNull(message));
        }
    }

    private Component renderOrNull(Player player, String template) {
        if (template == null || template.isEmpty()) {
            return null; // suppress the message
        }
        return service.renderTemplate(player, template, plugin.displayNameOf(player));
    }

    private static String legacyOrNull(Component message) {
        return message == null ? null : Cosmograph.getLegacySerializer().serialize(message);
    }
}
