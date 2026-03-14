package com.magmaguy.cannonrtp.commands;

import com.magmaguy.cannonrtp.CannonRTP;
import com.magmaguy.cannonrtp.MetadataHandler;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginBootstrap;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand extends AbstractCannonRTPCommand {
    public ReloadCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("reload"));
        setUsage("/wc reload");
        setPermission("cannonrtp.admin");
        setDescription("Reloads CannonRTP and all cannon configs.");
    }

    @Override
    public void execute(CommandData commandData) {
        reload(commandData.getCommandSender());
    }

    public static void reload(CommandSender commandSender) {
        NightbreakPluginBootstrap.setPendingReloadSender(MetadataHandler.PLUGIN, commandSender);
        MetadataHandler.PLUGIN.onDisable();
        MetadataHandler.PLUGIN.onLoad();
        MetadataHandler.PLUGIN.onEnable();
    }
}


