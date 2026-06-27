package com.magmaguy.cannonrtp;

import com.magmaguy.cannonrtp.commands.CenterCommand;
import com.magmaguy.cannonrtp.commands.CreateCommand;
import com.magmaguy.cannonrtp.commands.CreateNamedCommand;
import com.magmaguy.cannonrtp.commands.CannonRTPRootCommand;
import com.magmaguy.cannonrtp.commands.CommandHandler;
import com.magmaguy.cannonrtp.commands.DeleteCommand;
import com.magmaguy.cannonrtp.commands.HelpCommand;
import com.magmaguy.cannonrtp.commands.ListCannonsCommand;
import com.magmaguy.cannonrtp.commands.PlaceCommand;
import com.magmaguy.cannonrtp.commands.ProbeCommand;
import com.magmaguy.cannonrtp.commands.ReloadCommand;
import com.magmaguy.cannonrtp.commands.RemoveCommand;
import com.magmaguy.cannonrtp.commands.StatusCommand;
import com.magmaguy.cannonrtp.commands.TargetCommand;
import com.magmaguy.cannonrtp.config.CannonMessagesConfig;
import com.magmaguy.cannonrtp.services.CannonRTPManager;
import com.magmaguy.magmacore.command.CommandManager;
import com.magmaguy.magmacore.MagmaCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CannonRTPCommandRoutingTest {
    private ServerMock server;
    private MockCannonRTPPlugin plugin;
    private World world;
    private RecordingCannonRTPManager manager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = CannonRTPTestSupport.loadPlugin();
        MagmaCore.createInstance(plugin);
        MetadataHandler.PLUGIN = plugin;
        MockCannonRTPPlugin.resetLifecycleCounters();
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "prefix", "CannonRTP");
        CannonRTPTestSupport.setStaticField(CannonMessagesConfig.class, "helpHeader", "$prefix commands:");
        world = server.addSimpleWorld("world");
        manager = new RecordingCannonRTPManager();

        CommandManager commandManager = new CommandManager(plugin, "cannonrtp");
        CannonRTPTestSupport.setStaticField(CommandHandler.class, "cannonRTPCommandManager", commandManager);
        commandManager.registerCommand(new CannonRTPRootCommand(manager));
        commandManager.registerCommand(new HelpCommand());
        commandManager.registerCommand(new ReloadCommand(manager));
        commandManager.registerCommand(new CreateCommand(manager));
        commandManager.registerCommand(new CreateNamedCommand(manager));
        commandManager.registerCommand(new PlaceCommand(manager));
        commandManager.registerCommand(new TargetCommand(manager));
        commandManager.registerCommand(new CenterCommand(manager));
        commandManager.registerCommand(new ProbeCommand(manager));
        commandManager.registerCommand(new RemoveCommand(manager));
        commandManager.registerCommand(new DeleteCommand(manager));
        commandManager.registerCommand(new ListCannonsCommand(manager));
        commandManager.registerCommand(new StatusCommand(manager));
    }

    @AfterEach
    void tearDown() {
        MagmaCore.shutdown(plugin);
        CannonRTPTestSupport.setStaticField(MagmaCore.class, "instance", null);
        CannonRTPTestSupport.setStaticField(CommandHandler.class, "cannonRTPCommandManager", null);
        MetadataHandler.PLUGIN = null;
        MockCannonRTPPlugin.resetLifecycleCounters();
        MockBukkit.unmock();
    }

    @Test
    void pluginYmlDeclaresAdminPermissionsAndReleaseAliases() {
        YamlConfiguration pluginYml = loadRealPluginYml();

        assertEquals(List.of("crtp", "wc"), pluginYml.getStringList("commands.cannonrtp.aliases"));
        assertEquals("op", pluginYml.getString("permissions.cannonrtp.admin.default"));
        assertEquals("true", pluginYml.getString("permissions.cannonrtp.use.default"));
    }

    @Test
    void playerCommandsRouteToManagerActions() {
        PlayerMock player = server.addPlayer("Tiago");
        player.setOp(true);
        Location playerLocation = new Location(world, 12.5, 70, -8.5);
        player.teleport(playerLocation);
        World targetWorld = server.addSimpleWorld("target");

        assertTrue(server.dispatchCommand(player, "cannonrtp create travel Wild_Cannon"));
        assertEquals("travel", manager.createdId);
        assertEquals("Wild Cannon", manager.createdDisplayName);
        assertSame(player, manager.createdBy);

        assertTrue(server.dispatchCommand(player, "wc place travel"));
        assertEquals("travel", manager.placedId);
        assertSame(player, manager.placedBy);

        assertTrue(server.dispatchCommand(player, "cannonrtp target travel target"));
        assertEquals("travel", manager.targetId);
        assertSame(targetWorld, manager.targetWorld);
        assertSame(player, manager.targetSender);

        assertTrue(server.dispatchCommand(player, "cannonrtp center travel"));
        assertEquals("travel", manager.centerId);
        assertEquals(playerLocation, manager.centerLocation);
        assertSame(player, manager.centerSender);

        assertTrue(server.dispatchCommand(player, "cannonrtp probe"));
        assertSame(player, manager.probeSender);
        assertEquals(playerLocation, manager.probeLocation);

        assertTrue(server.dispatchCommand(player, "cannonrtp remove travel"));
        assertEquals("travel", manager.removedId);
        assertSame(player, manager.removedBy);

        assertTrue(server.dispatchCommand(player, "cannonrtp delete travel"));
        assertEquals("travel", manager.deletedId);
        assertSame(player, manager.deletedSender);

        assertTrue(server.dispatchCommand(player, "cannonrtp list"));
        assertSame(player, manager.listSender);

        assertTrue(server.dispatchCommand(player, "cannonrtp status"));
        assertSame(player, manager.statusSender);
    }

    @Test
    void playerOnlyCommandsDoNotRunFromConsole() {
        assertFalse(server.dispatchCommand(server.getConsoleSender(), "cannonrtp create travel"));
        assertFalse(server.dispatchCommand(server.getConsoleSender(), "cannonrtp remove travel"));
        assertNull(manager.createdId);
    }

    @Test
    void rootHelpAndReloadCommandAreRoutable() {
        PlayerMock player = server.addPlayer("Operator");
        player.setOp(true);

        assertTrue(server.dispatchCommand(player, "wc"));
        assertTrue(player.nextMessage().contains("CannonRTP commands"));
        assertTrue(player.nextMessage().contains("/wc"));

        assertTrue(server.dispatchCommand(server.getConsoleSender(), "wc reload"));
        assertEquals(1, MockCannonRTPPlugin.disableCalls);
        assertEquals(1, MockCannonRTPPlugin.loadCalls);
        assertEquals(1, MockCannonRTPPlugin.enableCalls);
    }

    private static YamlConfiguration loadRealPluginYml() {
        YamlConfiguration configuration = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(
                CannonRTPCommandRoutingTest.class.getResourceAsStream("/plugin.yml"),
                StandardCharsets.UTF_8)) {
            configuration.load(reader);
        } catch (Exception exception) {
            throw new AssertionError("Failed to load CannonRTP plugin.yml", exception);
        }
        return configuration;
    }

    private static final class RecordingCannonRTPManager extends CannonRTPManager {
        private String createdId;
        private String createdDisplayName;
        private Player createdBy;
        private String placedId;
        private Player placedBy;
        private String targetId;
        private World targetWorld;
        private CommandSender targetSender;
        private String centerId;
        private Location centerLocation;
        private CommandSender centerSender;
        private CommandSender probeSender;
        private Location probeLocation;
        private String removedId;
        private Player removedBy;
        private String deletedId;
        private CommandSender deletedSender;
        private CommandSender listSender;
        private CommandSender statusSender;

        private RecordingCannonRTPManager() {
            super(null);
        }

        @Override
        public List<String> getKnownCannonIds() {
            return List.of("travel");
        }

        @Override
        public void createCannon(String id, String displayName, Player player) {
            createdId = id;
            createdDisplayName = displayName;
            createdBy = player;
        }

        @Override
        public void placeCannon(String id, Player player) {
            placedId = id;
            placedBy = player;
        }

        @Override
        public void updateTargetWorld(String id, World world, CommandSender sender) {
            targetId = id;
            targetWorld = world;
            targetSender = sender;
        }

        @Override
        public void updateSearchCenter(String id, Location location, CommandSender sender) {
            centerId = id;
            centerLocation = location;
            centerSender = sender;
        }

        @Override
        public void probeLocation(CommandSender sender, Location location) {
            probeSender = sender;
            probeLocation = location;
        }

        @Override
        public void removeCannonNearPlayer(String id, Player player) {
            removedId = id;
            removedBy = player;
        }

        @Override
        public void deleteCannon(String id, CommandSender sender) {
            deletedId = id;
            deletedSender = sender;
        }

        @Override
        public void sendCannonList(CommandSender sender) {
            listSender = sender;
        }

        @Override
        public void sendStatus(CommandSender sender) {
            statusSender = sender;
        }
    }
}
