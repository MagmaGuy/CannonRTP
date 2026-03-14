package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.cannonrtp.commands.arguments.FreeTextCommandArgument;
import com.magmaguy.cannonrtp.services.CannonRTPManager;

import java.util.List;

public class CreateCommand extends AbstractCannonRTPCommand {
    public CreateCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("create"));
        addArgument("id", new FreeTextCommandArgument("<id>"));
        setUsage("/wc create <id>");
        setPermission("cannonrtp.admin");
        setSenderType(SenderType.PLAYER);
        setDescription("Creates a cannon at your location.");
    }

    @Override
    public void execute(CommandData commandData) {
        cannonRTPManager.createCannon(commandData.getStringArgument("id"), null, commandData.getPlayerSender());
    }
}


