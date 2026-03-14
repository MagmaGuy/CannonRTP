package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.cannonrtp.services.CannonRTPManager;

import java.util.List;

public class ListCannonsCommand extends AbstractCannonRTPCommand {
    public ListCannonsCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("list"));
        setUsage("/wc list");
        setPermission("cannonrtp.admin");
        setDescription("Lists all configured CannonRTPs.");
    }

    @Override
    public void execute(CommandData commandData) {
        cannonRTPManager.sendCannonList(commandData.getCommandSender());
    }
}


