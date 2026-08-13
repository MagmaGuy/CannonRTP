package com.magmaguy.cannonrtp.commands;

import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.magmacore.command.CommandData;

import java.util.List;

/**
 * Fallback for {@code /wc} with no arguments. Prints the greeting followed by every
 * registered CannonRTP subcommand — same content as {@link HelpCommand} so admins
 * discover commands without having to know {@code /wc help} exists.
 */
public class CannonRTPRootCommand extends AbstractCannonRTPCommand {
    public CannonRTPRootCommand(CannonRTPManager cannonRTPManager) {
        super(cannonRTPManager, List.of());
        setUsage("/wc");
        setPermission("cannonrtp.admin");
        setDescription("Shows CannonRTP help.");
    }

    @Override
    public void execute(CommandData commandData) {
        HelpCommand.printHelp(commandData.getCommandSender());
    }
}
