# CannonRTP

CannonRTP gives players a cannon-based random teleport. Cannons prepare safe destinations in a configured world, launch nearby players, and apply slow falling for the airdrop. Administrators can configure landing searches, queues, launch timing, protection checks, messages, and optional custom models.

[Download](https://nightbreak.io/plugin/cannonrtp/) · [Modrinth](https://modrinth.com/plugin/cannonrtp) · [Documentation](https://wiki.nightbreak.io/) · [Support](https://discord.gg/nightbreak)

## Requirements and installation

Use Java 21 or the newer Java version required by your server and a compatible Spigot/Paper server. The plugin declares Minecraft API version `1.21.4`; the download page lists release compatibility. MagmaCore is included in the JAR.

1. Put `CannonRTP.jar` in `plugins/` and start the server.
2. Run `/wc initialize` in-game, then use `/wc setup` to manage cannon model packs.
3. Stand where the cannon should be and run `/wc create spawn`.
4. Set its destination world with `/wc target spawn <world>` and review its generated settings.
5. Check `/wc status`. The cannon needs ready landing locations before it can launch players entering its trigger area.

FreeMinecraftModels supplies optional cannon models. WorldGuard, Towny, Lands, GriefPrevention, HuskClaims, and HuskTowns provide optional landing-protection checks. A failure to query protection is treated as an unsafe candidate by default.

## Commands and permissions

The root command is `/cannonrtp`; `/crtp` and `/wc` are aliases.

| Command | Purpose |
| --- | --- |
| `/wc help` | List the registered commands and their descriptions. |
| `/wc create <id> [display_name]` | Create a cannon at your location. |
| `/wc place <id>` | Place another instance of an existing cannon. |
| `/wc target <id> <world>` | Select a loaded destination world. |
| `/wc center <id>` | Set the destination search center from your location. |
| `/wc list` | List configured cannons. |
| `/wc status` | Inspect readiness and current cannon state. |
| `/wc remove <id>` | Remove a placed cannon instance. |
| `/wc delete <id>` | Delete a cannon definition. |
| `/wc reload` | Reload configuration. |
| `/wc initialize` / `/wc setup` | First-time setup and content management. |

`cannonrtp.use` allows player use and defaults to everyone. `cannonrtp.admin` grants administration and defaults to operators. Tab completion supplies known cannon IDs and loaded worlds.

## Configuration

Generated files live under `plugins/CannonRTP/`. Configure the cannon's destination world and search area, landing queue and readiness thresholds, cooldowns, launch timing, slow-falling duration, unsafe materials, and protection behavior. Messages and visible status labels have their own configuration.

The landing check considers the full occupied column and world border. If a cannon remains in a searching or exhausted state, check its world and search limits, the availability of safe terrain, and claims or region restrictions. `/wc probe` provides additional diagnostics.

Back up cannon configuration before deleting definitions or changing a live destination. Include `/wc status`, server and plugin versions, the affected cannon settings, and full logs when reporting a problem.

## Building and integration

Build from the repository root with JDK 21 and Maven:

```powershell
mvn -DskipTests package
```

The deployable file is `target/CannonRTP.jar`. Set `MC_DIST_DIR` to mirror it into a shared output directory. Publish a changed MagmaCore dependency to Maven Local before rebuilding.

This checkout's release is `1.2.2`. Integrations can use `com.magmaguy:CannonRTP:1.2.2` from [MagmaGuy's Maven repository](https://repo.magmaguy.com/releases), with Maven `provided` or Gradle `compileOnly` scope.
