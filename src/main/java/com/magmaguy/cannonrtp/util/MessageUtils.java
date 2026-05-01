package com.magmaguy.cannonrtp.util;

import com.magmaguy.magmacore.util.ChatColorConverter;
import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtils {
    private MessageUtils() {
    }

    public static void send(CommandSender sender, String template, String... placeholders) {
        sender.sendMessage(format(template, placeholders));
    }

    public static void sendRaw(CommandSender sender, String template) {
        sender.sendMessage(format(template));
    }

    public static void sendTitle(Player player, String title, String subtitle, String... placeholders) {
        sendTitle(player, title, subtitle, 0, 25, 10, placeholders);
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut, String... placeholders) {
        player.sendTitle(
                format(title, placeholders),
                format(subtitle, placeholders),
                fadeIn,
                stay,
                fadeOut);
    }

    public static String format(String template, String... placeholders) {
        String formatted = template.replace("$prefix", CannonMessagesConfig.getPrefix());
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            formatted = formatted.replace("$" + placeholders[index], placeholders[index + 1]);
        }
        return ChatColorConverter.convert(formatted);
    }
}

