package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.cannonrtp.services.CannonRTPManager;

import java.util.List;

public class StatusCommand extends AbstractCannonRTPCommand {
    public StatusCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("status"));
        setUsage("/wc status");
        setPermission("cannonrtp.admin");
        setDescription("Shows the current status of all configured CannonRTPs.");
    }

    @Override
    public void execute(CommandData commandData) {
        cannonRTPManager.sendCannonList(commandData.getCommandSender());
    }
}


