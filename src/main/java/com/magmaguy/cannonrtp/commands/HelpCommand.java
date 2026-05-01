package com.magmaguy.cannonrtp.commands;

import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import com.magmaguy.cannonrtp.util.MessageUtils;
import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.magmacore.command.CommandData;
import com.magmaguy.magmacore.command.CommandManager;

import java.util.List;

/**
 * Prints the greeting line from CannonMessagesConfig followed by every registered
 * CannonRTP subcommand, pulling usage and description directly from the MagmaCore
 * command metadata. No hardcoded command list to drift out of sync.
 *
 * <p>Reads the CannonRTP-specific {@link CommandManager} from {@link CommandHandler}
 * — {@code CommandManager.getCommandManagers()} is a shared registry across every
 * MagmaCore-based plugin on the server, so iterating it directly would leak other
 * plugins' commands into {@code /wc help}.</p>
 */
public class HelpCommand extends AdvancedCommand {
    public HelpCommand() {
        super(List.of("help"));
        setUsage("/wc help");
        setPermission("cannonrtp.admin");
        setDescription("Lists CannonRTP commands.");
    }

    @Override
    public void execute(CommandData commandData) {
        MessageUtils.sendRaw(commandData.getCommandSender(), CannonMessagesConfig.getHelpHeader());
        CommandManager commandManager = CommandHandler.getCannonRTPCommandManager();
        if (commandManager == null) return;
        for (AdvancedCommand command : commandManager.commands) {
            if (!command.getPermission().isBlank()
                    && !commandData.getCommandSender().hasPermission(command.getPermission())) {
                continue;
            }
            commandData.getCommandSender().sendMessage(
                    "  " + command.getUsage() + " - " + command.getDescription());
        }
    }
}
