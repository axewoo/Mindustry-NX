![Logo](core/assets-raw/sprites/ui/logo.png)

# Mindustry-NX

An unofficial Nintendo Switch homebrew port of [Mindustry](https://github.com/Anuken/Mindustry) v160.7, the automation tower defense RTS by [Anuke](https://github.com/Anuken).

> **This is a proof of concept.** The game runs and is playable at about 60 fps, but Mindustry is designed for a mouse and keyboard or a touch screen, and it plays better on those. Expect rough edges.

This port builds on [mindustry-switchgdx](https://github.com/SwitchGDX/mindustry-switchgdx) by [TheLogicMaster](https://github.com/TheLogicMaster), which used the [SwitchGDX](https://github.com/TheLogicMaster/switch-gdx) backend and the [Clearwing VM](https://github.com/TheLogicMaster/clearwing-vm) Java to C++ transpiler to bring Mindustry build 147 to the Switch. Mindustry-NX moves that work to the current Mindustry release and adds controller support and performance fixes.

## Installing
1. Download `Mindustry.nro` from the [releases](https://github.com/axewoo/Mindustry-NX/releases).
2. Copy it to `/switch/` on the SD card.
3. Launch it from hbmenu through a game (hold R while launching a game) so it gets the full RAM. Applet mode does not have enough memory.

The very first launch takes about 15 minutes while shaders are compiled and cached. Later launches take about 20 seconds. If something goes wrong, `Mindustry.log` next to the NRO has the details.

## Controls
Using the controller switches to Mindustry's desktop controls, with the right stick moving a mouse cursor. Touching the game world switches back to the touch controls.

| Input | In game | In menus |
|---|---|---|
| Left stick | Move the unit (the camera while dead) | |
| Right stick | Move the cursor (also where you aim) | Move the cursor |
| A | Left click: place, select, mine, hold to shoot, drag to place a line | Click |
| B | Right click: cancel, hold and drag to break, order units in command mode | Back |
| X | Pick the block under the cursor | |
| Y | Toggle command mode | |
| L / R | Rotate | |
| ZL / ZR | Zoom out / in | |
| D-pad left / right | Choose a block | Nudge the cursor |
| D-pad up / down | Change the block category | Scroll |
| L3 | Boost | |
| R3 | Respawn at the core | Center the cursor |
| + / − | Menu / pause | Back |

Quit from the game menu, which closes the game and returns to the Home menu.

## Limitations
- **No multiplayer.** Clearwing's runtime has no NIO channels or datagram sockets.
- Occasional short stutters from Clearwing's stop-the-world garbage collector.
- The main menu background runs at a low frame rate.
- Mod support is poor: Java mods cannot be loaded and JavaScript mods have issues.

## Building
CI (`.github/workflows/switch.yml`) builds the NRO on every push and attaches it to releases tagged `v*`.

To build locally:
1. Install devkitPro with the packages listed at the top of `scripts/build-switch.sh`, plus JDK 17, `git`, `unzip`, `rsync`, `python3`, `cmake` and `ninja`.
2. Build libffi for the Switch once with `sudo -E bash scripts/build-libffi.sh`.
3. Run `bash scripts/setup-switch-deps.sh`. It checks out Arc next to this repo and applies the Switch patches from `switch/arc-patches/`, then checks out rhino.
4. Run `bash scripts/build-switch.sh`. The first build transpiles and compiles the whole game and can take over an hour.

## Credits
- [Anuke](https://github.com/Anuken) and the Mindustry contributors, for [Mindustry](https://github.com/Anuken/Mindustry) and [Arc](https://github.com/Anuken/Arc).
- [TheLogicMaster](https://github.com/TheLogicMaster), for SwitchGDX, Clearwing VM and the original Switch port.

Mindustry is licensed under the GPLv3, and so is this port. Please report bugs in this port here, not to the upstream Mindustry project.

---

The rest of this file is the upstream Mindustry README.

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for general code style and PR guidelines.

If you are a first-time contributor looking for features to implement or bugs to fix, see the issues tagged with 'candidate' [in the Mindustry-Suggestions repostiory](https://github.com/Anuken/Mindustry-Suggestions/issues?q=is%3Aissue%20state%3Aopen%20label%3Acandidate).

## Building

Bleeding-edge builds are generated automatically for every commit. You can see them [here](https://github.com/Anuken/MindustryBuilds/releases).

If you'd rather compile on your own, follow these instructions.
First, make sure you have [JDK 17](https://adoptium.net/temurin/releases/?os=any&arch=any&version=17) installed. **Other JDK versions will not work.** Open a terminal in the Mindustry directory and run the following commands:

### Windows

_Running:_ `gradlew desktop:run`  
_Building:_ `gradlew desktop:dist`  
_Sprite Packing:_ `gradlew tools:pack`

### Linux/Mac OS

_Running:_ `./gradlew desktop:run`  
_Building:_ `./gradlew desktop:dist`  
_Sprite Packing:_ `./gradlew tools:pack`

### Server

Server builds are bundled with each released build (in Releases). If you'd rather compile on your own, replace 'desktop' with 'server', e.g. `gradlew server:dist`.

### Android

1. Install the Android SDK [here.](https://developer.android.com/studio#command-tools) Make sure you're downloading the "Command line tools only", as Android Studio is not required.
2. In the unzipped Android SDK folder, find the cmdline-tools directory. Then create a folder inside of it called `latest` and put all of its contents into the newly created folder.
3. In the same directory run the command `sdkmanager --licenses` (or `./sdkmanager --licenses` if on linux/mac)
4. Set the `ANDROID_HOME` environment variable to point to your unzipped Android SDK directory.
5. Enable developer mode on your device/emulator. If you are on testing on a phone you can follow [these instructions](https://developer.android.com/studio/command-line/adb#Enabling), otherwise you need to google how to enable your emulator's developer mode specifically.
6. Run `gradlew android:assembleDebug` (or `./gradlew` if on linux/mac). This will create an unsigned APK in `android/build/outputs/apk`.

To debug the application on a connected device/emulator, run `gradlew android:installDebug android:run`.

### Troubleshooting

#### Permission Denied

If the terminal returns `Permission denied` or `Command not found` on Mac/Linux, run `chmod +x ./gradlew` before running `./gradlew`. *This is a one-time procedure.*

#### Where is the `mindustry.gen` package?

As the name implies, `mindustry.gen` is generated *at build time* based on other code. You will not find source code for this package in the repository, and it should not be edited by hand.

The following is a non-exhaustive list of the "source" of generated code in `mindustry.gen`:

- `Call`, `*Packet` classes: Generated from methods marked with `@Remote`.
- All entity classes (`Unit`, `EffectState`, `Posc`, etc): Generated from component classes in the `mindustry.entities.comp` package, and combined using definitions in `mindustry.content.UnitTypes`.
- `Sounds`, `Musics`, `Tex`, `Icon`, etc: Generated based on files in the respective asset folders.

---

Gradle may take up to several minutes to download files. Be patient. <br>
After building, the output .JAR file should be in `/desktop/build/libs/Mindustry.jar` for desktop builds, and in `/server/build/libs/server-release.jar` for server builds.

## Feature Requests

Post feature requests and feedback [here](https://github.com/Anuken/Mindustry-Suggestions/issues/new/choose).

## Downloads

| [![](https://static.itch.io/images/badge.svg)](https://anuke.itch.io/mindustry)    |    [![](https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png)](https://play.google.com/store/apps/details?id=io.anuke.mindustry)   |    [![](https://fdroid.gitlab.io/artwork/badge/get-it-on.png)](https://f-droid.org/packages/io.anuke.mindustry)	| [![](https://flathub.org/assets/badges/flathub-badge-en.svg)](https://flathub.org/apps/details/com.github.Anuken.Mindustry)  
|---	|---	|---	|---	|
