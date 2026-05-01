package com.magmaguy.cannonrtp;

import com.magmaguy.cannonrtp.commands.CommandHandler;
import com.magmaguy.cannonrtp.commands.ReloadCommand;
import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import com.magmaguy.cannonrtp.config.CannonSoundsConfig;
import com.magmaguy.cannonrtp.config.DefaultConfig;
import com.magmaguy.cannonrtp.config.LandingSearchConfig;
import com.magmaguy.cannonrtp.config.ProtectionSettingsConfig;
import com.magmaguy.cannonrtp.config.contentpackages.ContentPackageConfig;
import com.magmaguy.cannonrtp.content.CannonRTPPackage;
import com.magmaguy.cannonrtp.content.CannonRTPPackageRefresher;
import com.magmaguy.cannonrtp.listeners.ChunkLifecycleListener;
import com.magmaguy.cannonrtp.listeners.FMMIntegrationListener;
import com.magmaguy.cannonrtp.listeners.PlayerQuitListener;
import com.magmaguy.cannonrtp.listeners.WorldLifecycleListener;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.magmacore.MagmaCore;
import com.magmaguy.magmacore.initialization.PluginInitializationConfig;
import com.magmaguy.magmacore.initialization.PluginInitializationContext;
import com.magmaguy.magmacore.initialization.PluginInitializationState;
import com.magmaguy.magmacore.nightbreak.NightbreakFirstTimeSetupSpec;
import com.magmaguy.magmacore.nightbreak.NightbreakFirstTimeSetupWarner;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginBootstrap;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginHooks;
import com.magmaguy.magmacore.nightbreak.NightbreakPluginSpec;
import com.magmaguy.magmacore.util.Logger;
import com.magmaguy.magmacore.util.VersionChecker;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class CannonRTP extends JavaPlugin {
    public static final NightbreakPluginSpec NIGHTBREAK_PLUGIN_SPEC = new NightbreakPluginSpec(
            "CannonRTP",
            "wc",
            "cannonrtp.admin",
            "cannonrtp.admin",
            "cannonrtp.admin",
            "https://nightbreak.io/plugin/cannonrtp/",
            "Reloaded CannonRTP.",
            true, false, true);
    public static final NightbreakFirstTimeSetupSpec FIRST_TIME_SETUP_SPEC = new NightbreakFirstTimeSetupSpec(
            "CannonRTP",
            "cannonrtp.admin",
            null,
            "/wc setup",
            "/wc downloadall",
            "https://nightbreak.io/plugin/cannonrtp/",
            "",
            java.util.List.of(),
            java.util.List.of());
    @Getter
    private static CannonRTP instance;
    @Getter
    private CannonRTPManager cannonRTPManager;

    @Override
    public void onLoad() {
        instance = this;
        MetadataHandler.PLUGIN = this;
        MagmaCore.createInstance(this);
        MagmaCore.exportSharedAssets(this);
    }

    @Override
    public void onEnable() {
        NightbreakPluginBootstrap.startInitialization(this,
                new PluginInitializationConfig("CannonRTP", "cannonrtp.admin", 10),
                NIGHTBREAK_PLUGIN_SPEC,
                new NightbreakPluginHooks() {
                    @Override
                    public void asyncInitialization(PluginInitializationContext initializationContext) {
                        CannonRTP.this.asyncInitialization(initializationContext);
                    }

                    @Override
                    public void syncInitialization(PluginInitializationContext initializationContext) {
                        CannonRTP.this.syncInitialization(initializationContext);
                    }

                    @Override
                    public void onInitializationSuccess() {
                        Logger.info("CannonRTP fully initialized!");
                    }

                    @Override
                    public void onInitializationFailure(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }

    @Override
    public void onDisable() {
        MagmaCore.requestInitializationShutdown(this);
        if (MagmaCore.getInitializationState(this.getName()) == PluginInitializationState.INITIALIZING) {
            getServer().getScheduler().cancelTasks(this);
            MagmaCore.shutdown(this);
            HandlerList.unregisterAll(this);
            CannonRTPPackage.shutdown();
            CannonRTPPackageRefresher.reset();
            Logger.info("[CannonRTP] Shutdown during initialization.");
            return;
        }
        if (cannonRTPManager != null) {
            cannonRTPManager.shutdown();
        }
        getServer().getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        CannonRTPPackage.shutdown();
        CannonRTPPackageRefresher.reset();
        MagmaCore.shutdown(this);
    }

    public boolean isPotentialLandingLocationAllowed(Location location) {
        return cannonRTPManager != null && cannonRTPManager.isPotentialLandingLocationAllowed(location);
    }

    private void asyncInitialization(PluginInitializationContext initializationContext) {
        initializationContext.step("Default Config");
        new DefaultConfig();

        initializationContext.step("Landing & Search Config");
        new LandingSearchConfig();

        initializationContext.step("Sounds Config");
        new CannonSoundsConfig();

        initializationContext.step("Messages Config");
        new CannonMessagesConfig();

        initializationContext.step("Protection Config");
        new ProtectionSettingsConfig();

        initializationContext.step("Content Importer");
        MagmaCore.initializeImporter(this);

        initializationContext.step("Content Packages");
        new ContentPackageConfig();
    }

    private void syncInitialization(PluginInitializationContext initializationContext) {
        initializationContext.step("Cannon Manager");
        cannonRTPManager = new CannonRTPManager(this);
        cannonRTPManager.initialize();

        initializationContext.step("Event Listeners");
        getServer().getPluginManager().registerEvents(new ChunkLifecycleListener(cannonRTPManager), this);
        getServer().getPluginManager().registerEvents(new WorldLifecycleListener(cannonRTPManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(cannonRTPManager), this);
        getServer().getPluginManager().registerEvents(new FMMIntegrationListener(cannonRTPManager), this);
        getServer().getPluginManager().registerEvents(new NightbreakFirstTimeSetupWarner(this, FIRST_TIME_SETUP_SPEC, DefaultConfig::isSetupDone), this);
        getServer().getPluginManager().registerEvents(new VersionChecker.VersionCheckerEvents(), this);

        initializationContext.step("Version Check");
        String resourceId = DefaultConfig.getSpigotResourceId();
        if (resourceId != null && !resourceId.isBlank()) {
            VersionChecker.VersionCheckerEvents.setDownloadURL("https://nightbreak.io/plugin/cannonrtp/");
            VersionChecker.checkPluginVersion(resourceId);
        }

        initializationContext.step("Commands");
        if (getCommand("cannonrtp") != null) {
            CommandHandler.registerCommands(this, cannonRTPManager);
        } else {
            Logger.warn("Failed to register /cannonrtp because it is missing from plugin.yml.");
        }
    }

    public void reloadImportedContent(CommandSender sender) {
        ReloadCommand.reload(sender);
    }
}
