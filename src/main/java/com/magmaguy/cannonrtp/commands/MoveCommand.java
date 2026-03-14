package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.cannonrtp.commands.arguments.KnownCannonIdCommandArgument;
import com.magmaguy.cannonrtp.services.CannonRTPManager;

import java.util.List;

public class MoveCommand extends AbstractCannonRTPCommand {
    public MoveCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("move"));
        addArgument("id", new KnownCannonIdCommandArgument(cannonRTPManager));
        setUsage("/wc move <id>");
        setPermission("cannonrtp.admin");
        setSenderType(SenderType.PLAYER);
        setDescription("Moves a cannon to your current location.");
    }

    @Override
    public void execute(CommandData commandData) {
        cannonRTPManager.moveCannon(commandData.getStringArgument("id"), commandData.getPlayerSender());
    }
}


