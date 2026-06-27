package com.magmaguy.cannonrtp;

import org.bukkit.plugin.java.JavaPlugin;

public class MockCannonRTPPlugin extends JavaPlugin {
    static int loadCalls;
    static int enableCalls;
    static int disableCalls;

    @Override
    public void onLoad() {
        loadCalls++;
    }

    @Override
    public void onEnable() {
        enableCalls++;
    }

    @Override
    public void onDisable() {
        disableCalls++;
    }

    static void resetLifecycleCounters() {
        loadCalls = 0;
        enableCalls = 0;
        disableCalls = 0;
    }
}
