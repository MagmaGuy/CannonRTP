package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.SenderType;
import com.magmaguy.magmacore.command.arguments.WorldCommandArgument;
import com.magmaguy.cannonrtp.commands.arguments.KnownCannonIdCommandArgument;
import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.cannonrtp.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.List;

public class TargetCommand extends AbstractCannonRTPCommand {
    public TargetCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of("target"));
        addArgument("id", new KnownCannonIdCommandArgument(cannonRTPManager));
        addArgument("world", new WorldCommandArgument("<world>"));
        setUsage("/wc target <id> <world>");
        setPermission("cannonrtp.admin");
        setSenderType(SenderType.PLAYER);
        setDescription("Changes a cannon's target world.");
    }

    @Override
    public void execute(CommandData commandData) {
        World world = Bukkit.getWorld(commandData.getStringArgument("world"));
        if (world == null) {
            MessageUtils.send(commandData.getCommandSender(),
                    CannonMessagesConfig.getInvalidConfigurationMessage(),
                    "cannon", commandData.getStringArgument("id"),
                    "reason", "Target world " + commandData.getStringArgument("world") + " is not loaded.");
            return;
        }
        cannonRTPManager.updateTargetWorld(commandData.getStringArgument("id"), world, commandData.getCommandSender());
    }
}


