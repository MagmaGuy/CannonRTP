package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.cannonrtp.commands.arguments.FreeTextCommandArgument;
import com.magmaguy.cannonrtp.services.CannonRTPManager;

import java.util.List;

public class CreateNamedCommand extends AbstractCannonRTPCommand {
    public CreateNamedCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("create"));
        addArgument("id", new FreeTextCommandArgument("<id>"));
        addArgument("displayName", new FreeTextCommandArgument("<display_name>"));
        setUsage("/wc create <id> <display_name>");
        setPermission("cannonrtp.admin");
        setSenderType(SenderType.PLAYER);
        setDescription("Creates a cannon at your location with a display name. Use underscores for spaces.");
    }

    @Override
    public void execute(CommandData commandData) {
        String displayName = commandData.getStringArgument("displayName");
        if (displayName != null) {
            displayName = displayName.replace('_', ' ');
        }
        cannonRTPManager.createCannon(commandData.getStringArgument("id"), displayName, commandData.getPlayerSender());
    }
}


