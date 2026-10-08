# <img src="neoforge-1.21.1/src/main/resources/create_clipboard_glance_logo.png" alt="" width="40" height="40" align="absmiddle"> Create: Clipboard Glance

**English** | [한국어](README.ko.md)

Read placed Create clipboards at a glance, without right-clicking to open them!

![Create: Clipboard Glance](media/Create_Clipboard_Glance.gif)

Look at a placed clipboard, and its page appears right next to your crosshair in the style of Create's goggle overlay.

### Works without goggles!

## Features

- Looks just like the clipboard itself. Text, checkboxes, `#` addresses and material lists are laid out in the preview exactly like on Create's clipboard screen.
- Turn pages by holding **Ctrl** and scrolling.
- Right-click the clipboard while previewing it, and it opens on the page you were reading.

You can change the page key in *Options → Controls → Key Binds → Create: Clipboard Glance*.

## Client-side only

Install it on your client and you're ready! It works on any server with Create, even if the server doesn't have this mod, so there's no need to add it to the server. It doesn't affect other players either.

## Requirements

| Minecraft | Loader | Create |
| --- | --- | --- |
| 1.21.1 | NeoForge 21.1.219+ | 6.0.10 |
| 1.20.1 | Forge 47.1+ | 6.0.8 |

Works alongside Sodium, Embeddium, Iris (with shaders), ImmediatelyFast, Jade, Xaero's Minimap, ModernFix, FerriteCore and more (tested on 1.21.1).

## Building

Each Minecraft version is a separate Gradle project. The JAR is built into each folder's `build/libs`.

| Folder | Java | Command |
| --- | --- | --- |
| `neoforge-1.21.1` | 21 | `./gradlew build` |
| `forge-1.20.1` | 17 | `./gradlew build` |

## Credits

- A fan-made addon that depends on [Create](https://github.com/Creators-of-Create/Create) by the Creators of Create team. Not affiliated with or endorsed by the Create team!
- The HUD uses the style and code of the overlay shown when wearing Create's goggles (Create code: MIT License, © The Create Team / The Creators of Create).
- Icon background: "New Create Logo Background" by Bl4zerBo1XXXX from the [Create Wiki on Fandom](https://create.fandom.com/wiki/Create_Addon_Mods), based on the Create logo, licensed under [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/).
- AI was used for the code and translations. No AI was used for the icon, the GIF or any other images.

## License

Copyright (c) 2026 potato815

This addon is licensed under [CC BY-NC-SA 4.0](LICENSE). In addition to CC BY-NC-SA 4.0, potato815 also permits the following.

- You may include official releases in modpacks that are free to use.
- Rewards, revenue shares or advertising income that CurseForge, Modrinth or similar platforms pay to modpack authors are not considered commercial use.
- Selling this addon or a modpack that contains it, or offering them only behind a payment, subscription or donation, is not permitted.
- Unless it is for paid sale as described above, you may freely modify and distribute it, as long as you credit potato815 and keep the same license.

The icon image (`create_clipboard_glance_logo.png`) is shared under the same CC BY-SA license as its background. The Gradle build files derived from the NeoForged MDK are under the MIT license (`TEMPLATE_LICENSE.txt` in each project folder). Create is a separate mod and is not included in this repository.
