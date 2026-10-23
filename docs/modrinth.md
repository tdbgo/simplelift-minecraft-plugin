# Modrinth submission

## Metadata

| Field | Value |
|---|---|
| Title | SimpleLift |
| Byline | SimpleLift by PLAYCITY BLOCK |
| Slug | simplelift |
| Categories | Transportation, Utility, Game Mechanics |
| Platforms | Bukkit, Spigot, Paper, Purpur; not Folia |
| Target Minecraft versions | 1.13–26.3; see the testing caveat below |
| Environment | Server required; client unsupported |
| License | GPL-3.0-only |
| Source | https://github.com/tdbgo/simplelift-minecraft-plugin |
| Issues | https://github.com/tdbgo/simplelift-minecraft-plugin/issues |

## Summary

Block-built elevators with scroll-to-select floors, group rides, and configurable chimes. Reuse compatible Lift/LiftReloaded structures.

## Description

**SimpleLift by PLAYCITY BLOCK**

Build elevators with blocks, buttons, and signs. Scroll to choose a floor, press the button, and travel together. No registration commands, database, client mod, or resource pack.

Reuse compatible Lift/LiftReloaded structures and import their configuration on first start. Test existing lifts on a server copy before switching.

![An elevator control sign showing Current Floor: 1 and Destination: 4, with a wooden button below.](https://cdn.modrinth.com/data/cached_images/91daaa35e8a33a042424f1a3f23a3ab3389b8aba.jpeg)

*In-game controls with a custom resource pack and shaders. Neither is required by SimpleLift.*

### Build, select, ride

Use **iron blocks at the bottom** and **glass at upper stops**, aligned vertically. Attach controls to an adjacent wall at each stop:

```text
Floor + 3   Wall sign
Floor + 2   Button
Floor + 1   Clear space
Floor       Iron base / glass upper floor
```

Build at least two stops, keep the shaft clear, then right-click a blank control sign to initialize.

1. Right-click the sign **with an empty hand** and scroll. Sneak-scroll skips several floors; the action bar shows your choice.
2. Confirm with another sign click or the swap-hand key (**F** by default).
3. Stand on the floor and **press the button**. Everyone inside the departure footprint rides together.

Holding an item lets you cycle destinations by clicking the sign instead. Busy shafts refuse additional calls; calls are not queued. Players waiting on intermediate floors stay in place.

### Customize

Change materials, speed, height and footprint limits, or enable redstone calls in `plugins/SimpleLift/config.yml`.

**In 0.3.3-SNAPSHOT:**

- **Button chimes:** a two-note `warm` bell for wooden buttons and a three-note `crystal` chime for stone buttons. Assign profiles by vanilla button tag or exact material; unmatched buttons use the default. Sounds use built-in Minecraft instruments. `soundVolume` controls the overall level; explicit existing profile assignments are preserved.
- **Hotel chime:** opt into a low, descending three-note `hotel` chime through a material, tag, or default assignment. For example, set `arrivalChimes.materials.OAK_BUTTON: hotel` to apply it to oak buttons. Existing defaults are preserved.
- **Language:** choose one of 30 bundled locales, including `en`, `ko`, `de`, `ja`, `pt_BR`, `zh`, `zh_HK`, and `zh_TW`, or use `custom`. Presets apply server-wide; custom mode keeps your message translations. Floor names are not translated.

Apply edits with `/lift reload`. Invalid settings keep the previous configuration active.

### Install

Stop the server and back up the world and plugins. Remove any Lift/LiftReloaded JAR, put SimpleLift in `plugins`, and restart normally. Check `/lift info`. Do not use plugin hot-reload tools.

Targets **Minecraft 1.13–26.3** on Bukkit, Spigot, Paper, and Purpur. Use the Java version required by your server. **Folia is unsupported.**

Runtime checks covered Paper 1.13.2, 1.16.5, 1.20.6, 1.21.8, and 26.2 on 0.3.0-SNAPSHOT. Other versions/server families are not individually verified. The 0.3.3-SNAPSHOT source passed clean builds and all 47 tests against both Spigot 1.13.2 and Paper 26.3 build 218. The existing 0.3.3-26.3-beta.2 JAR enabled normally on Paper 26.3 build 157; its executable classes and resources match this release, with differences limited to version metadata. This confirms startup only; full ride scenarios and in-game listening checks remain pending on 26.3.

### Good to know

- Permissions: `lift` and `lift.change` default to everyone; `lift.reload` to operators.
- No telemetry, external connections, or runtime downloads. Configuration stays in the plugin folder; lift blocks and signs stay in the world save.
- NoteBlockAPI `.nbs` music is not implemented. A crash during travel can leave temporarily removed floor blocks unrestored; keep backups.

[Guide](https://github.com/tdbgo/simplelift-minecraft-plugin#readme) · [한국어](https://github.com/tdbgo/simplelift-minecraft-plugin/blob/main/README.ko.md) · [Issues](https://github.com/tdbgo/simplelift-minecraft-plugin/issues)

Includes code adapted from [LiftReloaded](https://github.com/kikelkik/LiftReloaded) by kikelkik, the successor to [Lift](https://github.com/croxis/Lift) by croxis. Reworked and extended for SimpleLift. Licensed under GPL-3.0-only.

[0.3.3-SNAPSHOT source and build instructions (ZIP)](https://cdn.modrinth.com/data/JLI7cKt8/versions/A7Rt012X/SimpleLift-0.3.3-SNAPSHOT-source.zip)

## Version

| Field | Value |
|---|---|
| Title | SimpleLift 0.3.3-SNAPSHOT |
| Number | 0.3.3-SNAPSHOT |
| Channel | Beta |
| Primary file | SimpleLift-0.3.3-SNAPSHOT.jar |
| SHA-256 | 1430667f99d9eac3f09fcb03deeb1502d41f001279a091a1d285bdd6305601ca |
| Version page | https://modrinth.com/plugin/simplelift/version/A7Rt012X |
| Source ZIP SHA-256 | 1989fb44460cfd8ef82bba397954796db7a9a2cecbc54fc4733d0155b41f850e |

### Version changelog

- Include Minecraft 26.3 in the compatibility target; confirm clean API compilation and the existing 26.3 build's normal startup.
- Add the opt-in `hotel` arrival chime: a low, descending three-note major triad using built-in Minecraft instruments.
- Assign `hotel` by exact button material, vanilla button tag, or the fallback default. Exact material assignments retain priority.
- Keep the existing warm wooden-button and crystal stone-button defaults, explicit assignments, language presets, and custom translations.

Beta snapshot. Clean builds against Spigot 1.13.2 and Paper 26.3 build 218: 47 tests passed in each build. Java 8 bytecode. The existing 26.3 build has identical executable classes and resources, with differences limited to version metadata. Its startup was observed on Paper 26.3 build 157; full ride scenarios and in-game listening checks remain pending on 26.3. Minecraft 1.13–26.3 is the compatibility target, not a claim that every server/version combination has been tested. Folia is unsupported.

The supplementary source ZIP includes source, tests, Gradle wrapper, build instructions, and license notices.
