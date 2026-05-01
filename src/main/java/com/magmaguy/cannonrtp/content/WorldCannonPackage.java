package com.magmaguy.cannonrtp.content;

import com.magmaguy.cannonrtp.MetadataHandler;
import com.magmaguy.cannonrtp.commands.ReloadCommand;
import com.magmaguy.cannonrtp.config.contentpackages.ContentPackageConfigFields;
import com.magmaguy.magmacore.nightbreak.AbstractNightbreakContentPackage;
import com.magmaguy.magmacore.nightbreak.NightbreakFileUtils;
import com.magmaguy.magmacore.util.Logger;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WorldCannonPackage extends AbstractNightbreakContentPackage {
    @Getter
    private static final Map<String, WorldCannonPackage> worldCannonPackages = new HashMap<>();
    @Getter
    private final ContentPackageConfigFields contentPackageConfigFields;

    public WorldCannonPackage(ContentPackageConfigFields contentPackageConfigFields) {
        this.contentPackageConfigFields = contentPackageConfigFields;
        worldCannonPackages.put(contentPackageConfigFields.getFilename(), this);
    }

    public static void shutdown() {
        worldCannonPackages.clear();
    }

    @Override
    protected void doInstall(Player player) {
        player.closeInventory();
        List<File> disabledEntries = collectManagedEntries(getDisabledCannonsFolder());
        if (disabledEntries.isEmpty()) {
            Logger.sendMessage(player, "&cCould not find the disabled cannon package files for " + getDisplayName());
            return;
        }

        NightbreakFileUtils.moveEntriesFlat(disabledEntries, getInstalledCannonsFolder());
        handleStateSave(player,
                contentPackageConfigFields.setEnabledAndSave(true),
                () -> {
                    if (player.isOnline()) {
                        Logger.sendSimpleMessage(player, "&aReloading CannonRTP so the cannon package is enabled...");
                    }
                    ReloadCommand.reload(player);
                },
                "&cFailed to update CannonRTP package state. Check the console.");
    }

    @Override
    protected void doUninstall(Player player) {
        player.closeInventory();
        List<File> installedEntries = collectManagedEntries(getInstalledCannonsFolder());
        if (installedEntries.isEmpty()) {
            Logger.sendMessage(player, "&cCould not find the installed cannon package files for " + getDisplayName());
            return;
        }

        NightbreakFileUtils.moveEntriesFlat(installedEntries, getDisabledCannonsFolder());
        handleStateSave(player,
                contentPackageConfigFields.setEnabledAndSave(false),
                () -> {
                    if (player.isOnline()) {
                        Logger.sendSimpleMessage(player, "&aReloading CannonRTP so the cannon package is disabled...");
                    }
                    ReloadCommand.reload(player);
                },
                "&cFailed to update CannonRTP package state. Check the console.");
    }

    private File getInstalledCannonsFolder() {
        return new File(MetadataHandler.PLUGIN.getDataFolder(), "cannons");
    }

    private File getDisabledCannonsFolder() {
        return new File(MetadataHandler.PLUGIN.getDataFolder(), "cannons_disabled");
    }

    private List<File> collectManagedEntries(File rootFolder) {
        return NightbreakFileUtils.collectRootEntries(rootFolder,
                contentPackageConfigFields.getFolderName(),
                contentPackageConfigFields.getContentFilePrefixes());
    }

    @Override
    protected JavaPlugin getOwnerPlugin() {
        return MetadataHandler.PLUGIN;
    }

    @Override
    protected String getPluginDisplayName() {
        return "CannonRTP";
    }

    @Override
    protected String getContentPageUrl() {
        return "https://nightbreak.io/plugin/world_cannon/";
    }

    @Override
    protected List<String> getPackageDescription() {
        return contentPackageConfigFields.getDescription();
    }

    @Override
    protected String getManualImportsFolderName() {
        return "CannonRTP imports";
    }

    @Override
    protected String getManualReloadCommand() {
        return "/wc reload";
    }

    @Override
    protected void onDownloadStateSaved(Player player) {
        if (player.isOnline()) {
            Logger.sendSimpleMessage(player, "&aReloading CannonRTP so the new cannon package is picked up...");
        }
        ReloadCommand.reload(player);
    }

    @Override
    public String getNightbreakSlug() {
        return contentPackageConfigFields.getNightbreakSlug();
    }

    @Override
    public String getDisplayName() {
        return contentPackageConfigFields.getName();
    }

    @Override
    public String getDownloadLink() {
        return contentPackageConfigFields.getDownloadLink();
    }

    @Override
    public int getLocalVersion() {
        return contentPackageConfigFields.getVersion();
    }

    @Override
    public CompletableFuture<Void> enableAfterDownload() {
        return contentPackageConfigFields.setEnabledAndSave(true);
    }

    @Override
    public boolean isInstalled() {
        return contentPackageConfigFields.isEnabled() && !collectManagedEntries(getInstalledCannonsFolder()).isEmpty();
    }

    @Override
    public boolean isDownloaded() {
        return !collectManagedEntries(getInstalledCannonsFolder()).isEmpty()
                || !collectManagedEntries(getDisabledCannonsFolder()).isEmpty();
    }
}
