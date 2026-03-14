package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.cannonrtp.commands.arguments.KnownCannonIdCommandArgument;
import com.magmaguy.cannonrtp.services.CannonRTPManager;

import java.util.List;

public class CenterCommand extends AbstractCannonRTPCommand {
    public CenterCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("center"));
        addArgument("id", new KnownCannonIdCommandArgument(cannonRTPManager));
        setUsage("/wc center <id>");
        setPermission("cannonrtp.admin");
        setSenderType(SenderType.PLAYER);
        setDescription("Sets a cannon's search center to your current location.");
    }

    @Override
    public void execute(CommandData commandData) {
        cannonRTPManager.updateSearchCenter(
                commandData.getStringArgument("id"),
                commandData.getPlayerSender().getLocation(),
                commandData.getCommandSender());
    }
}


