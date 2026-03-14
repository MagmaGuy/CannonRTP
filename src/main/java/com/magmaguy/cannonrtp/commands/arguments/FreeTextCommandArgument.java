package com.magmaguy.cannonrtp.commands.arguments;

import com.magmaguy.magmacore.command.arguments.ICommandArgument;
import org.bukkit.command.CommandSender;

import java.util.List;

public class FreeTextCommandArgument implements ICommandArgument {
    private final String hint;

    public FreeTextCommandArgument(String hint) {
        this.hint = hint;
    }

    @Override
    public String hint() {
        return hint;
    }

    @Override
    public boolean matchesInput(String input) {
        return input != null && !input.isBlank();
    }

    @Override
    public List<String> literals() {
        return List.of();
    }

    @Override
    public List<String> getSuggestions(CommandSender sender, String partialInput) {
        return partialInput.isEmpty() ? List.of(hint) : List.of();
    }

    @Override
    public boolean isLiteral() {
        return false;
    }
}

