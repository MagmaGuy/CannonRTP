package com.magmaguy.cannonrtp.menus;

import com.magmaguy.cannonrtp.MetadataHandler;
import com.magmaguy.cannonrtp.CannonRTP;
import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.magmacore.menus.FirstTimeSetupMenu;
import com.magmaguy.magmacore.menus.MenuButton;
import com.magmaguy.magmacore.nightbreak.NightbreakSetupMenuHelper;
import com.magmaguy.magmacore.util.ItemStackGenerator;
import com.magmaguy.magmacore.util.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CannonRTPFirstTimeSetupMenu {
    private CannonRTPFirstTimeSetupMenu() {
    }

    public static void createMenu(Player player) {
        new FirstTimeSetupMenu(
                (JavaPlugin) MetadataHandler.PLUGIN,
                player,
                "&6CannonRTP",
                "&6Nightbreak content setup",
                createInfoItem(),
                List.of(createRecommendedItem(), createManualItem(), createSkipItem()));
    }

    private static MenuButton createInfoItem() {
        return new MenuButton(ItemStackGenerator.generateSkullItemStack(
                "magmaguy",
                "&2Welcome to CannonRTP!",
                List.of("&7Link Nightbreak, install cannon content, and let CannonRTP import it automatically."))) {
            @Override
            public void onClick(Player player) {
                player.closeInventory();
                NightbreakSetupMenuHelper.sendFirstTimeSetupResources(player, CannonRTP.FIRST_TIME_SETUP_SPEC);
            }
        };
    }

    private static MenuButton createRecommendedItem() {
        return new MenuButton(ItemStackGenerator.generateItemStack(
                Material.GREEN_STAINED_GLASS_PANE,
                "&2Recommended Setup",
                List.of("&aMarks setup complete and points you to Nightbreak-managed cannon content."))) {
            @Override
            public void onClick(Player player) {
                player.closeInventory();
                DefaultConfig.toggleSetupDone(true);
                NightbreakSetupMenuHelper.sendRecommendedSetupInstructions(player, CannonRTP.FIRST_TIME_SETUP_SPEC);
            }
        };
    }

    private static MenuButton createManualItem() {
        return new MenuButton(ItemStackGenerator.generateItemStack(
                Material.YELLOW_STAINED_GLASS_PANE,
                "&6Manual Setup",
                List.of("&eMarks setup complete and leaves content management up to you."))) {
            @Override
            public void onClick(Player player) {
                player.closeInventory();
                DefaultConfig.toggleSetupDone(true);
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                Logger.sendSimpleMessage(player, "&6Setup complete. Use &a/wc setup &6when you want to manage cannon content.");
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
            }
        };
    }

    private static MenuButton createSkipItem() {
        return new MenuButton(ItemStackGenerator.generateItemStack(
                Material.RED_STAINED_GLASS_PANE,
                "&cUse Current Content",
                List.of("&cDismisses the setup prompt and keeps your current cannon configs as-is."))) {
            @Override
            public void onClick(Player player) {
                player.closeInventory();
                DefaultConfig.toggleSetupDone(true);
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
                Logger.sendSimpleMessage(player, "&aSetup complete. CannonRTP will keep using your current cannon configs.");
                Logger.sendSimpleMessage(player, "&7Run &a/wc reload &7if you import new content later.");
                Logger.sendSimpleMessage(player, "&8&m-----------------------------------------------------");
            }
        };
    }
}
