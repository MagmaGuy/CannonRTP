package com.magmaguy.cannonrtp.commands;

import com.magmaguy.cannonrtp.CannonRTP;
import com.magmaguy.cannonrtp.content.CannonRTPPackage;
import com.magmaguy.cannonrtp.menus.CannonRTPFirstTimeSetupMenu;
import com.magmaguy.cannonrtp.menus.CannonRTPSetupMenu;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.magmacore.command.CommandManager;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginBootstrap;

public final class CommandHandler {
    private static CommandManager cannonRTPCommandManager;

    private CommandHandler() {
    }

    public static CommandManager getCannonRTPCommandManager() {
        return cannonRTPCommandManager;
    }

    public static void registerCommands(CannonRTP plugin, CannonRTPManager cannonRTPManager) {
        cannonRTPCommandManager = new CommandManager(plugin, "cannonrtp");
        cannonRTPCommandManager.registerCommand(new CannonRTPRootCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new HelpCommand());
        cannonRTPCommandManager.registerCommand(new ReloadCommand(cannonRTPManager));
        NightbreakPluginBootstrap.registerStandardCommands(plugin,
                cannonRTPCommandManager,
                CannonRTP.NIGHTBREAK_PLUGIN_SPEC,
                CannonRTPSetupMenu::createMenu,
                CannonRTPFirstTimeSetupMenu::createMenu,
                () -> new java.util.ArrayList<>(CannonRTPPackage.getCannonRTPPackages().values()),
                ReloadCommand::reload);
        cannonRTPCommandManager.registerCommand(new ListCannonsCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new StatusCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new ProbeCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new CreateCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new CreateNamedCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new PlaceCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new RemoveCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new DeleteCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new TargetCommand(cannonRTPManager));
        cannonRTPCommandManager.registerCommand(new CenterCommand(cannonRTPManager));
    }
}
