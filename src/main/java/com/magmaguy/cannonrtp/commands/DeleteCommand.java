package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.cannonrtp.commands.arguments.KnownCannonIdCommandArgument;
import com.magmaguy.cannonrtp.services.CannonRTPManager;

import java.util.List;

public class DeleteCommand extends AbstractCannonRTPCommand {
    public DeleteCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("delete"));
        addArgument("id", new KnownCannonIdCommandArgument(cannonRTPManager));
        setUsage("/wc delete <id>");
        setPermission("cannonrtp.admin");
        setDescription("Deletes a configured cannon.");
    }

    @Override
    public void execute(CommandData commandData) {
        cannonRTPManager.deleteCannon(commandData.getStringArgument("id"), commandData.getCommandSender());
    }
}


