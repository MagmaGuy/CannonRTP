package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.cannonrtp.services.CannonRTPManager;

import java.util.List;

public class ProbeCommand extends AbstractCannonRTPCommand {
    public ProbeCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("probe"));
        setUsage("/wc probe");
        setPermission("cannonrtp.admin");
        setSenderType(SenderType.PLAYER);
        setDescription("Checks whether your current location is a valid landing spot.");
    }

    @Override
    public void execute(CommandData commandData) {
        cannonRTPManager.probeLocation(commandData.getPlayerSender(), commandData.getPlayerSender().getLocation());
    }
}


