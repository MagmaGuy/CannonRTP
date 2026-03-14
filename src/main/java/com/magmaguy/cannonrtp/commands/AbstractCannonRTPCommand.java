package com.magmaguy.cannonrtp.commands;

import com.magmaguy.magmacore.command.AdvancedCommand;
import com.magmaguy.cannonrtp.services.CannonRTPManager;

import java.util.List;

abstract class AbstractCannonRTPCommand extends AdvancedCommand {
    protected final CannonRTPManager cannonRTPManager;

    protected AbstractCannonRTPCommand(CannonRTPManager cannonRTPManager, List<String> aliases) {
        super(aliases);
        this.cannonRTPManager = cannonRTPManager;
    }
}


