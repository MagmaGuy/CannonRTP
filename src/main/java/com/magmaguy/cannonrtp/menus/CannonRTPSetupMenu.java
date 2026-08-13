package com.magmaguy.cannonrtp.menus;

import com.magmaguy.cannonrtp.MetadataHandler;
import com.magmaguy.cannonrtp.CannonRTP;
import com.magmaguy.cannonrtp.content.CannonRTPPackage;
import com.magmaguy.cannonrtp.content.CannonRTPPackageRefresher;
import com.magmaguy.magmacore.menus.MenuButton;
import com.magmaguy.magmacore.menus.SetupMenuBuilder;
import com.magmaguy.magmacore.nightbreak.DownloadAllContentPackage;
import com.magmaguy.magmacore.nightbreak.NightbreakSetupControls;
import com.magmaguy.magmacore.util.ChatColorConverter;
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

        MenuButton infoButton = NightbreakSetupControls.setupInfoButton(
                CannonRTP.NIGHTBREAK_PLUGIN_SPEC,
                "https://nightbreak.io/plugin/cannonrtp/#setup");

        SetupMenuBuilder builder = new SetupMenuBuilder((JavaPlugin) MetadataHandler.PLUGIN, player)
                .title("Setup menu")
                .infoButton(infoButton)
                .packages(packages)
                .appendPackage(new DownloadAllContentPackage<>(() -> new ArrayList<>(CannonRTPPackage.getCannonRTPPackages().values()),
                        "CannonRTP",
                        "https://nightbreak.io/plugin/cannonrtp/",
                        "wc downloadall"));
        NightbreakSetupControls.prependStandardControls(builder, (JavaPlugin) MetadataHandler.PLUGIN, CannonRTP.NIGHTBREAK_PLUGIN_SPEC)
                .open();
    }
}
