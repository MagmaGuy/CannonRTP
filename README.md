<p align="center">
  <a href="https://discord.gg/GKyzTgT">
    <img src="https://imgur.com/mvyJwOf.png" alt="Discord" width="180"/>
  </a>
  <a href="https://nightbreak.io/plugin/cannonrtp/">
    <img src="https://imgur.com/OLYJlM8.png" alt="Nightbreak" width="180"/>
  </a>
  <a href="https://www.patreon.com/magmaguy">
    <img src="https://imgur.com/rWCe9W9.png" alt="Patreon" width="180"/>
  </a>
  <a href="https://wiki.nightbreak.io/WorldCannon/info">
    <img src="https://imgur.com/M48vTUC.png" alt="Wiki" width="180"/>
  </a>
</p>

<div align="center">
  <iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/KLJkSCnCh9w" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
  <p>Note: CannonRTP trailer coming soon, for now here's a short video showing how it works!</p>
</div>

# 🟠 About CannonRTP:

**CannonRTP** transforms random teleportation into a cinematic experience. Instead of instantly appearing somewhere new, players step into a cannon, watch it calibrate their destination, and get launched across the world.

As players enter the cannon, they are lifted into the air while the system locks in a safe destination, complete with coordinate previews. Once ready, the cannon fires—blasting them skyward before dropping them above their landing zone with smooth descent and impact effects.



# 🟠 Main Features:

* **Fully Configurable Cannons:** Place cannons anywhere and set custom RTP distances, target worlds, and search centers per unit.
* **Safety First:** Built-in safe location checks ensure players never land in a wall or over lava.
* **Deep Integration:** Full support for popular protection plugins like WorldGuard, Towny, Lands, and more.
* **Total Customization:** Everything from timing to particles and sounds can be adjusted to match your server's aesthetic.
* **Visual Flexibility:** Supports custom cannon models using [**FreeMinecraftModels**](https://nightbreak.io/plugin/freeminecraftmodels/) and [**ResourcePackManager**](https://nightbreak.io/plugin/resourcepackmanager/) with an automatic fallback to particle visuals if no models are installed.
* **Quick Setup:** One command places a working cannon, using the premium or free cannon model when one is installed.
* **One Cannon, Many Places:** A single cannon config can stand in several spots. Run `/cannonrtp place <cannon_name>` to add another one. [Commands](https://wiki.nightbreak.io/WorldCannon/commands_and_permissions#admin-commands)
* **Other Worlds and the Nether:** A cannon can land players in a different world. In the Nether it looks for open cavern floors below the bedrock roof. [Landing rules](https://wiki.nightbreak.io/WorldCannon/info#landing-safety-rules)
* **No Waiting:** Each cannon keeps a queue of checked landing spots ready, and a floating label above it shows Ready, Charging or Exhausted. [Cannon states](https://wiki.nightbreak.io/WorldCannon/info#cannon-states)
* **Permissions and Cooldowns:** Lock a cannon behind its own permission, and set how long players wait between launches (30 seconds by default). [Configuration](https://wiki.nightbreak.io/WorldCannon/configuration#per-cannon-config-fields)
* **In-Game Setup Menu:** Download the cannon models from `/cannonrtp setup` and apply plugin updates without restarting the server. [Content packages](https://wiki.nightbreak.io/WorldCannon/content_packages)


# 🟠 How to Setup:

1. **Put the CannonRTP.jar into your server** and **restart your server**
2. Do **/cannonrtp setup** in-game and pick the cannon models you want. In-game downloads need your server linked to your [Nightbreak account](https://wiki.nightbreak.io/global/nightbreak_account_linking_and_tokens) with `/nightbreaklogin <token>`.

   You can also download the content you want from the additional downloads and put it, **without unzipping**, into the **~plugins/CannonRTP/imports** folder, then do **/cannonrtp reload** or **restart your server**. Restarting is recommended.
3. Place your first cannon (see Managing Cannons below).
4. You're done!

**Important:**
No cannon is placed until you create one. The example cannon config in plugins\CannonRTP\cannons\example_cannonrtp.yml is disabled and is only there as a reference for writing cannon files by hand.

Cannons only land players on terrain that already exists. Pre-generate or explore the area each cannon searches (500 to 5000 blocks from the cannon by default) before players use it.


# 🟠 Managing Cannons:

* **Creation:** To place a new cannon, stand where you want it to be and run `/cannonrtp create <cannon_name>`.
* **More placements:** To put the same cannon somewhere else too, stand there and run `/cannonrtp place <cannon_name>`.
* **Landing world:** Run `/cannonrtp target <cannon_name> <world>` to send players to a different world.
* **Removal:** To remove the placement nearest to you, run `/cannonrtp remove <cannon_name>`. To delete the cannon and all of its placements, run `/cannonrtp delete <cannon_name>`.
* **Status:** Run `/cannonrtp status` to see whether each cannon is ready.

All commands and permissions are on the [wiki](https://wiki.nightbreak.io/WorldCannon/commands_and_permissions).

**Requirements:** Minecraft version 1.21.4+ is required.


<p align="center">
  <a href="https://itch.io/s/201155/yearly-sale">
    <img src="https://img.itch.zone/aW1nLzMwMDA3NjM3LnBuZw==/original/Mz69Hw.png" alt="Nightbreak Yearly Sale - 20% off through November 15" width="804"/>
  </a>
</p>

# 🟠 Additional downloads:

<p align="center">
  Or create your own content by <strong><a href="https://wiki.nightbreak.io/WorldCannon/configuration#freeminecraftmodels-integration">following the documentation</a></strong>!
</p>
<p align="center">
  <a href="https://nightbreak.io/plugin/cannonrtp/#free-cannon-model">
    <img src="https://i.postimg.cc/sgXqBcSv/Group-7102.png" alt="Free Cannon Model" width="400"/>
  </a>
  <a href="https://nightbreak.io/plugin/cannonrtp/#cannonrtp-premium-cannon-model">
    <img src="https://i.postimg.cc/0jPH9dP2/Group-7101.png" alt="Premium Cannon Model" width="400"/>
  </a>
</p>



# 🟠 View More by Magmaguy!

<p align="center">
  <a href="https://nightbreak.io/plugin/elitemobs">
    <img src="https://i.postimg.cc/zXQRn1jD/Elite-Mobs.png" width="300"/>
  </a>
  <a href="https://nightbreak.io/plugin/betterstructures">
    <img src="https://i.postimg.cc/3rVK1rPq/Better-Structures.png" width="300"/>
  </a>
</p>
<p align="center">
  <a href="https://nightbreak.io/plugin/extractioncraft">
    <img src="https://i.postimg.cc/gjV6VMYz/extractioncraft.png" width="300"/>
  </a>
  <a href="https://nightbreak.io/plugin/eternaltd">
    <img src="https://i.postimg.cc/Kjf8cCGM/ETD.png" width="300"/>
  </a>
</p>

<p align="center">
  <a href="https://nightbreak.io/plugin/resourcepackmanager">
    <img src="https://i.postimg.cc/Y0GdtBfX/Resource-Pack-Manager.png" width="300"/>
  </a>
  <a href="https://nightbreak.io/plugin/freeminecraftmodels">
    <img src="https://i.postimg.cc/0Qxv6ZHd/FMM.png" width="300"/>
  </a>
</p>
<p align="center">
  <a href="https://nightbreak.io/plugin/betterfood">
    <img src="https://i.postimg.cc/JzcDSB80/Better-Food.png" width="300"/>
  </a>
  <a href="https://nightbreak.io/plugin/resurrectionchest">
    <img src="https://i.imgur.com/qpgXCge.png" width="300"/>
  </a>
</p>


# 🟠 Partner servers:

**Matecraft is the official partner server!** It is run by me and is always **up-to-date with the latest experimental features and new dungeons**! Check it out if you want to see my plugins the way they were originally developed to work! It's got a cozy community of dedicated and welcoming players that call it home!

<p align="center">
  <strong>eternaltd.matecraft.org</strong><br>
  <img src="https://i.postimg.cc/wBhbQ8vs/header.gif" width="600"/>
</p>

# 🟠 Developers

- [Java API guide](https://wiki.nightbreak.io/WorldCannon/api): launch, destination validation and landing events, plus building from source
- [Generated Java reference](https://wiki.nightbreak.io/javadoc/cannonrtp/index.html)
- [Maven repository and coordinates](https://wiki.nightbreak.io/developers)
- [Source code](https://github.com/MagmaGuy/CannonRTP)
