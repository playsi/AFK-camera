### AFK Camera

<img src="src/main/resources/icon/icon.png"  width="190px" alt="mod logo"/>

---

## Overview

AFK Camera is a Minecraft mod that automatically activates cinematic camera movements when you're idle. After a configurable period of inactivity (default 30 seconds), the mod seamlessly transitions your camera into a free-cam mode and begins playing smooth, pre-defined camera animations around your world.
![AFK Camera](https://github.com/playsi/AFK-camera/blob/1.21.5/AFK%20Camera%20preview.gif?raw=true)
## Features

- **Automatic AFK Detection**: Monitors player activity including movement, mouse input, key presses, and damage
- **Smooth Camera Transitions**: Automatically switches to free camera mode with cinematic animations
- **Configurable Timing**: Customizable AFK timeout period
- **HUD Management**: Automatically hides the HUD during AFK mode
- **Animation System**: Supports custom camera keyframe animations with position and rotation interpolation
- **Smart Deactivation**: Instantly returns to normal gameplay when any player activity is detected
- **Death Protection**: Optional setting to disable AFK mode when player dies

## How It Works

The mod continuously monitors for player activity including:
- Movement keys (WASD, jump, sneak, sprint)
- Mouse movement
- Action keys (attack, use, inventory)
- Player taking damage
- Menu interactions

When no activity is detected for the configured time period, AFK Camera:
1. Enables free camera mode
2. Hides the game HUD
3. Loads and plays cinematic camera animations in a random order
4. Cycles through available animations continuously

The moment any player input is detected, the mod immediately:
1. Stops the current animation
2. Restores the HUD
3. Returns camera control to the player
4. Disables free camera mode

## Block Bench Compatibility

This mod is designed to work seamlessly with **Blockbench** camera animations. You can create custom camera paths and keyframe animations in Blockbench, and the mod will automatically load and play them during AFK periods. The animation system supports:

- Position keyframes with smooth interpolation
- Rotation keyframes (pitch and yaw)
- Custom timing and duration
- Automatic scaling for Minecraft world coordinates

## Custom camera animations tutorial
Template [resource pack](https://github.com/playsi/AFK-camera/raw/refs/heads/1.21.5/Rp%20template%20-%20AFK%20Camera.zip).
Template [BlockBench file](https://github.com/playsi/AFK-camera/blob/1.21.5/Template.bbmodel).
Detailed text guide - coming soon.

## Fabric Only
This mod is developed for the Fabric mod loader. A **Forge** version is **not planned** due to the significant architectural differences between the platforms and the mod's deep integration with Fabric-specific APIs.

## Dependencies

### Required
- **[Fabric API](https://modrinth.com/mod/fabric-api)** - Core Fabric mod loader functionality
- **[YACL](https://modrinth.com/mod/yacl) (Yet Another Config Library)** - Configuration management

### Optional
- **[Mod Menu](https://modrinth.com/mod/modmenu)** - Provides in-game configuration interface

## Issues

If you find a problem that is not listed, you can report it [here](https://github.com/playsi/AFK-camera/issues).

## Acknowledgments

Big thanks to [hashalite](https://modrinth.com/user/hashalite) for creating [Freecam](https://modrinth.com/mod/freecam)! I used a bit of the code in my project, and it really helped me out. Great work!

Thanks to [ZipeStudio](https://modrinth.com/user/ZipeStudio) for help with IDE.

Thanks to [LopyMine](https://modrinth.com/user/LopyMine/mods) for [Mossy](https://github.com/LopyMine/Mossy) template!

Thanks to [Danrus1100](https://modrinth.com/user/danrus110) for contributing!

## License

This project is licensed under the BY-ND 4.0 License - see the LICENSE file for details.