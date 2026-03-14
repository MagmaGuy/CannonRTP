package com.magmaguy.cannonrtp.commands.arguments;

import com.magmaguy.magmacore.command.arguments.ListStringCommandArgument;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import org.bukkit.command.CommandSender;

import java.util.List;

public class KnownCannonIdCommandArgument extends ListStringCommandArgument {
    private final CannonRTPManager cannonRTPManager;

    public KnownCannonIdCommandArgument(CannonRTPManager cannonRTPManager) {
        super("<id>");
        this.cannonRTPManager = cannonRTPManager;
    }

    @Override
    public boolean matchesInput(String input) {
        return cannonRTPManager.getKnownCannonIds().stream().anyMatch(id -> id.equalsIgnoreCase(input));
    }

    @Override
    public List<String> literals() {
        return cannonRTPManager.getKnownCannonIds();
    }

    @Override
    public List<String> getSuggestions(CommandSender sender, String partialInput) {
        String lower = partialInput.toLowerCase();
        return cannonRTPManager.getKnownCannonIds().stream()
                .filter(id -> id.toLowerCase().startsWith(lower))
                .toList();
    }
}


