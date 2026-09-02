package dev.noellx.cosmograph.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import dev.noellx.cosmograph.Cosmograph;
import dev.noellx.cosmograph.chat.ChatFormatService;
import dev.noellx.cosmograph.chat.ItemPlaceholder;
import dev.noellx.cosmograph.chat.MentionService;

/**
 * Paper chat listener. Decorates the safe message component (emoji, URLs, mention highlighting), then
 * installs a per-viewer {@link io.papermc.paper.chat.ChatRenderer}.
 */
public class AsyncChatListener implements Listener {

    private final Cosmograph plugin;
    private final ChatFormatService service;

    public AsyncChatListener(Cosmograph plugin) {
        this.plugin = plugin;
        this.service = plugin.getChatFormatService();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.isDisabledWorld(player.getWorld().getName())) {
            return;
        }

        String effectiveRaw = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.maybeItemPlaceholderHint(player, effectiveRaw);

        boolean allowColor = player.hasPermission("cosmograph.chatcolor");
        Component base = service.messageComponent(effectiveRaw, allowColor);
        base = plugin.getEmojiReplacer().apply(player, base);
        base = plugin.getUrlLinkifier().apply(player, base, true);

        MentionService.Result mention = plugin.getMentionService()
                .highlight(base, MentionService.onlineNames(plugin.getServer().getOnlinePlayers()));
        plugin.getMentionService().pingAll(mention.mentioned(), player.getName());

        Component finalMessage = mention.message();
        Component displayName = plugin.displayNameOf(player);
        event.renderer((source, sourceDisplayName, message, viewer) ->
                ItemPlaceholder.apply(plugin, source, service.render(source, finalMessage, displayName), true));
    }
}
