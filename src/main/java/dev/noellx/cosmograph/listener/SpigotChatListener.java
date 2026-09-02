package dev.noellx.cosmograph.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import dev.noellx.cosmograph.Cosmograph;
import dev.noellx.cosmograph.chat.ChatFormatService;
import dev.noellx.cosmograph.chat.ItemPlaceholder;
import dev.noellx.cosmograph.chat.MentionService;

/**
 * Legacy Spigot chat listener. Decorates the message, renders through the shared
 * {@link ChatFormatService}, and bakes the result into the chat format as a legacy string.
 * Hover/click cannot survive legacy serialization, so URL links are styled-only on Spigot.
 */
public class SpigotChatListener implements Listener {

    private final Cosmograph plugin;
    private final ChatFormatService service;

    public SpigotChatListener(Cosmograph plugin) {
        this.plugin = plugin;
        this.service = plugin.getChatFormatService();
    }

    @SuppressWarnings("deprecation") // AsyncPlayerChatEvent is the only chat hook on Spigot
    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.isDisabledWorld(player.getWorld().getName())) {
            return;
        }

        String effectiveRaw = event.getMessage();
        plugin.maybeItemPlaceholderHint(player, effectiveRaw);

        boolean allowColor = player.hasPermission("cosmograph.chatcolor");
        Component base = service.messageComponent(effectiveRaw, allowColor);
        base = plugin.getEmojiReplacer().apply(player, base);
        base = plugin.getUrlLinkifier().apply(player, base, false);

        MentionService.Result mention = plugin.getMentionService()
                .highlight(base, MentionService.onlineNames(plugin.getServer().getOnlinePlayers()));
        plugin.getMentionService().pingAll(mention.mentioned(), player.getName());

        Component rendered = service.render(player, mention.message(), plugin.displayNameOf(player));
        rendered = ItemPlaceholder.apply(plugin, player, rendered, false);

        // Bake the rendered line into the format; escape % so the server's String.format is safe.
        String legacy = Cosmograph.getLegacySerializer().serialize(rendered).replace("%", "%%");
        event.setFormat(legacy);
    }
}
