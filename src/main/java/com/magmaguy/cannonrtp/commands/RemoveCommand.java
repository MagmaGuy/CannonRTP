package com.magmaguy.cannonrtp.commands;

import com.magmaguy.cannonrtp.commands.arguments.KnownCannonIdCommandArgument;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;

import java.util.List;

public class RemoveCommand extends AbstractCannonRTPCommand {
    public RemoveCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("remove"));
        addArgument("id", new KnownCannonIdCommandArgument(cannonRTPManager));
        setUsage("/wc remove <id>");
        setPermission("cannonrtp.admin");
        setSenderType(SenderType.PLAYER);
        setDescription("Removes the nearest placed instance of a cannon.");
    }

    @Override
    public void execute(CommandData commandData) {
        cannonRTPManager.removeCannonNearPlayer(
                commandData.getStringArgument("id"),
                commandData.getPlayerSender());
    }
}
