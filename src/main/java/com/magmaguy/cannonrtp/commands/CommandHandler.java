package com.magmaguy.cannonrtp.commands;

import com.magmaguy.cannonrtp.CannonRTP;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.cannonrtp.content.WorldCannonPackage;
import com.magmaguy.cannonrtp.menus.WorldCannonFirstTimeSetupMenu;
import com.magmaguy.cannonrtp.menus.WorldCannonSetupMenu;
import com.magmaguy.magmacore.command.CommandManager;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginBootstrap;

public final class CommandHandler {
    private static CommandManager cannonRTPCommandManager;

    private CommandHandler() {
    }

    public static void registerCommands(CannonRTP plugin, CannonRTPManager cannonRTPManager) {
        cannonRTPCommandManager = new CommandManager(plugin, "cannonrtp");
        cannonRTPCommandManager.registerCommand(new CannonRTPRootCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new ReloadCommand(cannonRTPManager));
        NightbreakPluginBootstrap.registerStandardCommands(plugin,
                cannonRTPCommandManager,
                CannonRTP.NIGHTBREAK_PLUGIN_SPEC,
                WorldCannonSetupMenu::createMenu,
                WorldCannonFirstTimeSetupMenu::createMenu,
                () -> new java.util.ArrayList<>(WorldCannonPackage.getWorldCannonPackages().values()),
                ReloadCommand::reload);
        cannonRTPCommandManager.registerCommand(new ListCannonsCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new StatusCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new ProbeCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new CreateCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new CreateNamedCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new MoveCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new DeleteCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new TargetCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new CenterCommand(cannonRTPManager));
    }
}
