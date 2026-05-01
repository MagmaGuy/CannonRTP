package com.magmaguy.cannonrtp.commands;

import com.magmaguy.cannonrtp.commands.arguments.KnownCannonIdCommandArgument;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;

import java.util.List;

public class PlaceCommand extends AbstractCannonRTPCommand {
    public PlaceCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("place"));
        addArgument("id", new KnownCannonIdCommandArgument(cannonRTPManager));
        setUsage("/wc place <id>");
        setPermission("cannonrtp.admin");
        setSenderType(SenderType.PLAYER);
        setDescription("Places another instance of an existing cannon at your location.");
    }

    @Override
    public void execute(CommandData commandData) {
        cannonRTPManager.placeCannon(
                commandData.getStringArgument("id"),
                commandData.getPlayerSender());
    }
}
