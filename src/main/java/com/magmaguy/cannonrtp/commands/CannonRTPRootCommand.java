package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.cannonrtp.util.MessageUtils;

import java.util.List;

public class CannonRTPRootCommand extends AbstractCannonRTPCommand {
    public CannonRTPRootCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of());
        setUsage("/cannonrtp");
        setPermission("cannonrtp.admin");
        setDescription("Shows CannonRTP help.");
    }

    @Override
    public void execute(CommandData commandData) {
        MessageUtils.sendRaw(commandData.getCommandSender(), DefaultConfig.getHelpHeader());
    }
}


