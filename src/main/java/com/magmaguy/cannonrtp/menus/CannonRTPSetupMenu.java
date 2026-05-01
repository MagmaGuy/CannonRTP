package com.magmaguy.cannonrtp.menus;

import com.magmaguy.cannonrtp.MetadataHandler;
import com.magmaguy.cannonrtp.content.CannonRTPPackage;
import com.magmaguy.cannonrtp.content.CannonRTPPackageRefresher;
import com.magmaguy.magmacore.menus.ContentPackage;
import com.magmaguy.magmacore.menus.MenuButton;
import com.magmaguy.magmacore.menus.SetupMenu;
import com.magmaguy.magmacore.nightbreak.DownloadAllContentPackage;
import com.magmaguy.magmacore.nightbreak.NightbreakAccount;
import com.magmaguy.magmacore.util.ChatColorConverter;
import com.magmaguy.magmacore.util.ItemStackGenerator;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.SpigotMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CannonRTPSetupMenu {
    private CannonRTPSetupMenu() {
    }

    public static void createMenu(Player player) {
        List<CannonRTPPackage> packages = new ArrayList<>(CannonRTPPackage.getCannonRTPPackages().values()).stream()
                .sorted(Comparator.comparing(pkg ->
                        ChatColor.stripColor(ChatColorConverter.convert(pkg.getContentPackageConfigFields().getName()))))
                .collect(Collectors.toList());
        CannonRTPPackageRefresher.refreshContentAndAccess();

        MenuButton infoButton = new MenuButton(ItemStackGenerator.generateSkullItemStack("magmaguy",
                "&2Installation instructions:",
                List.of(
                        "&61) &fLink your Nightbreak account: &a/nightbreaklogin",
                        "&62) &fDownload all cannon content: &a/wc downloadall",
                        "&63) &fOr browse and manage it here: &a/wc setup"))) {
            @Override
            public void onClick(Player p) {
                p.closeInventory();
                Logger.sendSimpleMessage(p, "<g:#8B0000:#CC4400:#DAA520>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</g>");
                Logger.sendSimpleMessage(p, "&6&lCannonRTP installation resources:");
                p.spigot().sendMessage(
                        SpigotMessage.simpleMessage("&2&lNightbreak account: "),
                        SpigotMessage.hoverLinkMessage("&ahttps://nightbreak.io/account/",
                                "&7Click to open the Nightbreak account page.",
                                "https://nightbreak.io/account/"));
                p.spigot().sendMessage(
                        SpigotMessage.simpleMessage("&2&lContent: "),
                        SpigotMessage.hoverLinkMessage("&ahttps://nightbreak.io/plugin/cannonrtp/",
                                "&7Click to browse CannonRTP content.",
                                "https://nightbreak.io/plugin/cannonrtp/"));
                p.spigot().sendMessage(
                        SpigotMessage.commandHoverMessage("&2&lBulk download: &a/wc downloadall",
                                "&7Click to download all available CannonRTP content.",
                                "/wc downloadall"));
                if (NightbreakAccount.hasToken()) {
                    p.spigot().sendMessage(
                            SpigotMessage.commandHoverMessage("&2&lBulk update: &a/wc updatecontent",
                                    "&7Click to update all outdated CannonRTP content.",
                                    "/wc updatecontent"));
                }
                Logger.sendSimpleMessage(p, "<g:#8B0000:#CC4400:#DAA520>▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬</g>");
            }
        };

        List<ContentPackage> allPackages = new ArrayList<>(packages);
        allPackages.add(new DownloadAllContentPackage<>(() -> new ArrayList<>(CannonRTPPackage.getCannonRTPPackages().values()),
                "CannonRTP",
                "https://nightbreak.io/plugin/cannonrtp/",
                "wc downloadall"));

        new SetupMenu((JavaPlugin) MetadataHandler.PLUGIN, player, infoButton, allPackages, List.of(), "Setup menu");
    }
}
