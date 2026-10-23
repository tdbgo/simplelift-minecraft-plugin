# SimpleLift

**SimpleLift by PLAYCITY BLOCK**

[한국어](README.ko.md)

SimpleLift is an elevator plugin for Bukkit-family Minecraft servers. It moves players, mobs, and vehicles between the floors of a shaft built entirely out of ordinary blocks. It preserves the configuration keys, command name, permission nodes, and sign layout used by Lift and LiftReloaded. Compatible existing elevators need no conversion; verify an upgrade on a disposable server copy before production use.

The elevator is the build itself. A lift is defined by a base block at the lowest floor, a floor block in the same column at every higher floor, and a button with a wall sign above it on each floor. There is no lift database and no registration command: every ride re-reads the structure from the world, so breaking a floor block or moving a sign changes the lift immediately.

That design is where the practical advantages come from. Lift definitions stay in the world instead of a separate registry, so world copies and rollbacks need no database synchronisation. Migration from Lift or LiftReloaded needs no structure conversion, only a config import that runs by itself on first start. One Java 8 bytecode JAR targets Minecraft 1.13 through 26.3.

Rides carry everyone standing on the departure floor at once, hold still anyone waiting on a floor the lift passes through, and restore every temporarily removed block and every changed entity property when the ride ends.

## How a lift is put together

Heights are relative to the floor block, which sits at the floor level (`floor`):

```text
floor + 3   wall sign — the control sign
floor + 2   button
floor + 1   free space, or an optional wall sign carrying the floor name
floor       base block (lowest floor) or floor block (every higher floor)
```

Rules the scanner enforces:

- A button counts as a lift button only when a sign block sits directly above it and a floor block or base block sits two blocks below it.
- The lowest floor is found by searching straight down from the button. The first `baseBlocks` material found there becomes the base; the connected same-material blocks reached horizontally from it form the lift footprint.
- Higher floors are found by walking upward through every column of that footprint. A floor exists at a height where at least one column has a valid button structure.
- Floors are numbered `1` upward in order of height. Floor `1` is the lowest.
- Any solid block in a footprint column that is not a configured floor, button, sign, or shaft block stops the scan at that height. Everything above the obstruction is invisible to the lift.
- A lift needs at least two floors. With one floor the plugin answers with the `oneFloor` message.

The control sign holds the state of the floor, in this fixed line order:

```text
line 1   Current Floor: 3      messages.currentFloor + ": " + current level
line 2   Lobby                 name of the current floor
line 3   Destination: 5        messages.destination + ": " + destination level
line 4   Roof                  name of the destination floor
```

The destination level is parsed from the text after the last `:` on line 3, so the `messages.currentFloor` and `messages.destination` labels can be translated freely. A floor's name comes from the optional wall sign one block below the button; if that sign is absent, line 2 of the control sign is used.

Right-clicking a control sign whose first line is blank initialises every uninitialised sign in the shaft and does nothing else. That is the only setup action a lift needs.

### Choosing and confirming a floor

Two selection styles are available on the same sign, decided by what the player is holding:

- **Holding an item.** Each right-click on the control sign advances the destination by one floor and writes it to the sign at once. No confirmation step.
- **Empty hand,** with `mouseScroll: true`. Right-clicking opens a scroll session. Scrolling the hotbar moves the destination one floor per notch; scrolling while sneaking moves `selection.fastStep` floors. The action bar shows the floor, its name, and the position in the floor list. The hotbar slot itself does not change while `selection.preserveHotbar` is true.

A scroll session is confirmed by pressing the swap-hand key (`selection.confirmWithSwapKey`), by right-clicking the sign again, or by pressing the lift button. It ends on its own after `selection.timeoutSeconds` seconds of inactivity, or as soon as the player moves further than 3 blocks from the sign, changes world, dies, loses `lift.change`, disconnects, or the configuration is reloaded. One sign can only be held by one player at a time; a second player gets the `scrollSelectBusy` message.

The current floor is never offered as a destination, so a lift always travels somewhere.

### What happens during a ride

Pressing the button starts the ride toward the destination shown on the sign.

- Everyone standing on the departure floor inside the shaft footprint travels together. Mobs and vehicles are included unless `liftMobs` is set to `false`.
- Anyone waiting on a floor the lift passes through is held in place and protected while that floor block is temporarily removed. Floors outside the route are never touched.
- A second call to a shaft that overlaps an active ride is refused with the `busy` message until the ride finishes; clicking the button or sign of a moving lift answers the same way.
- Floor blocks along the route, the non-sign block directly above them, and rails under a carried minecart are removed for the duration of the ride and put back when it ends.
- Gravity, invulnerability, flight permission, and flight state are captured before departure and restored afterwards.
- Leaving a moving lift is blocked while `preventLeave` is true; the rider is pushed back to the shaft centre. With `preventEntry` set to `true`, other players cannot walk into a moving shaft.
- Exiting a vehicle inside a moving lift is cancelled with the `unsafe` message.
- If the ride exceeds its expected travel time by `secondsUntilTimeout` seconds, the riders are teleported to the destination and told so with the `timeout` message.
- Disconnecting mid-ride teleports the player to the centre of the lowest floor, and the same safe position is applied again on the next join.

### Migration boundary

Verified in code and in the runtime matrix:

- `plugin.yml` declares `provides: [Lift, LiftReloaded]`, so plugins that depend on either name still resolve.
- On first start, if `plugins/SimpleLift/config.yml` does not exist, SimpleLift copies `plugins/LiftReloaded/config.yml`, or otherwise `plugins/Lift/config.yml`, and logs the imported path. If neither exists it writes the bundled defaults.
- Both `baseBlocks` layouts are read: the current list of `type`/`speed`/`music` entries and the version 1 map keyed by material.
- Command name, aliases, permission nodes, sign line layout, message keys, and material glob syntax such as `*BUTTON` are unchanged.
- Elevators that use the preserved Lift/LiftReloaded block and sign layout need no structure conversion.

Not carried over: NoteBlockAPI `.nbs` playback is not implemented. Legacy `music: true` entries are accepted and logged as a warning, and SimpleLift plays its normal movement sounds instead.

## Requirements

| Item | Requirement |
|---|---|
| Server | Bukkit, Spigot, Paper, or Purpur-based, Minecraft 1.13 through 26.3 (`api-version: '1.13'`) |
| Java | Whatever the server release requires; the JAR is Java 8 bytecode and imposes no higher floor |
| Other plugins | None. Nothing is downloaded at runtime |
| Folia | Not supported; the plugin detects Folia and disables itself |

Minecraft 1.12 and older cannot load the plugin at all, because `api-version: '1.13'` is rejected by those servers.

## Installation

1. Stop the server and back up the world and the `plugins` directory.
2. Remove the old Lift or LiftReloaded JAR if one is present. Do not run them alongside SimpleLift.
3. Copy `SimpleLift-0.3.3-SNAPSHOT.jar` into `plugins`.
4. Start the server normally and read the console. SimpleLift logs `SimpleLift <version> enabled on <server version>`, and logs the imported legacy configuration path if it imported one.
5. Run `/lift info` and ride an existing elevator.

Use a full server restart rather than a plugin-manager reload. Only `/lift reload` is supported for configuration changes.

## Five-minute quick start

This builds a two-floor lift with the bundled defaults.

1. Place an `IRON_BLOCK` on the ground. This is the base block and the whole footprint of a one-column lift.
2. Two blocks above it, put a button on an adjacent wall. One block above the button, put a wall sign. The button is at base level + 2, the sign at base level + 3.
3. Build the upper floor five or more blocks higher: put a `GLASS` block in the same column as the base block, and give it the same button and sign, again at +2 and +3.
4. Keep the column between the two floors clear. Air, ladders, torches, vines, water, rails, snow, and redstone wire are allowed; other solid blocks stop the scan.
5. Right-click either control sign once. Both signs fill in with the current and destination floors.
6. Stand on the lower floor block, press the button, and ride up. Press the button on the upper floor to come back down.

To widen the lift, add more `IRON_BLOCK`s next to the first one and repeat the floor block, button, and sign in any of those columns. The footprint may be up to `maxLiftArea` blocks — 49 by default.

## Everyday use

- **Send the lift somewhere:** right-click the control sign until the destination line shows the floor you want, then press the button.
- **Pick a floor by scrolling:** with an empty hand, right-click the sign, scroll to the floor, then press the swap-hand key or the button.
- **Skip several floors at once:** sneak while scrolling to move `selection.fastStep` floors per notch.
- **Ride as a group:** everyone standing on the departure floor inside the footprint goes along.
- **Name a floor:** put a wall sign one block below the button and write the name on its second line.
- **Call the lift with redstone:** set `redstone: true`. A rising power edge on a lift button or its support block starts a ride, and that ride carries only whoever is already on that floor.

Riding is gated by the `lift` permission; changing a destination on a sign is gated by `lift.change`.

![SimpleLift hotbar and action bar design reference](docs/assets/selection-ui-mock.png)

The image is a design reference, not an in-game screenshot.

## Commands and permissions

| Command | Effect | Permission |
|---|---|---|
| `/lift` or `/lift info` | Prints the plugin version and the supported range | `lift` |
| `/lift reload` | Validates and re-reads `config.yml` | `lift.reload` |

`/simplelift` is an alias of `/lift`. Both subcommands offer tab completion, and `reload` is only suggested to players who hold `lift.reload`.

| Permission | Grants | Default |
|---|---|---|
| `lift` | Riding a lift and using the base command | Everyone |
| `lift.change` | Changing the destination on a control sign | Everyone |
| `lift.reload` | Reloading the configuration | Operators |

`/lift reload` never leaves the server in a broken state. If the new file fails validation, the error is reported to the sender and logged, and the previously loaded settings stay active. A successful reload ends every open floor-selection session; rides already in progress finish with the settings they started with.

## Configuration

The only configuration file is `plugins/SimpleLift/config.yml`. Values missing from it fall back to the bundled defaults.

| Key | Purpose | Accepted range |
|---|---|---|
| `maxLiftArea` | Maximum number of connected base blocks in one footprint | 1–4096, default 49 |
| `minHeight`, `maxHeight` | Vertical scan bounds, further clamped by the world's own build limits | `maxHeight` must exceed `minHeight` |
| `baseBlocks` | Lowest-floor materials with their `speed`, and the legacy `music` flag | `speed` above 0 and at most 10; no duplicate materials |
| `floorBlocks`, `buttonBlocks`, `signBlocks`, `shaftBlocks` | Material names or `*` globs such as `*WALL_SIGN` | Each of the first three must match at least one real block material |
| `mouseScroll` | Enables empty-hand scroll selection | boolean |
| `selection.preserveHotbar` | Keeps the held slot unchanged while scrolling | boolean |
| `selection.actionBar` | Shows the selection on the action bar | boolean |
| `selection.timeoutSeconds` | Inactivity timeout for a selection; `0` disables expiry | 0–300, default 8 |
| `selection.fastStep` | Floors advanced per sneak-scroll notch | 1–20, default 5 |
| `selection.confirmWithSwapKey` | Lets the swap-hand key confirm | boolean |
| `secondsUntilTimeout` | Grace period added to the expected travel time before a ride is force-completed | 0–60, default 2 |
| `preventEntry`, `preventLeave` | Block entering or leaving a moving shaft | boolean |
| `liftMobs` | Carry non-player entities | boolean |
| `autoPlace` | Centre riders on the floor before departure | boolean |
| `redstone` | Allow redstone power to call a lift | boolean |
| `soundEnabled`, `soundVolume` | Movement and arrival sounds | volume 0–100 |
| `debug` | Extra scan and ride logging | boolean |
| `messages.*` | All player-facing text, with `&` colour codes | strings |

`{floor}`, `{name}`, `{index}`, and `{total}` are substituted in the selection messages such as `messages.scrollSelection` and `messages.scrollSelectConfirmed`.

Values outside these ranges, unknown material names, and invalid glob patterns are rejected with a message naming the key. At startup a rejected configuration disables the plugin; during `/lift reload` it leaves the running settings untouched.

### Language and screenshots

Set `language` to one of the built-in locale presets below, or use
`language: custom` to keep your own `messages` text in any language. The default
is `custom`, preserving existing translated configurations. A bundled locale
takes precedence over the entire `messages` section; custom mode falls back to
the bundled English text only for missing keys.

| Language | `language` value |
|---|---|
| English (US), English (UK) | `en` (`en_US`), `en_GB` |
| Bulgarian, Czech, Danish, German, Greek | `bg`, `cs`, `da`, `de`, `el` |
| Spanish, Estonian, Finnish, French, Hungarian | `es`, `et`, `fi`, `fr`, `hu` |
| Italian, Japanese, Korean, Lithuanian, Dutch, Norwegian | `it`, `ja`, `ko`, `lt`, `nl`, `no` |
| Polish, Portuguese (Portugal), Portuguese (Brazil), Romanian | `pl`, `pt` (`pt_PT`), `pt_BR`, `ro` |
| Russian, Swedish, Turkish, Ukrainian, Vietnamese | `ru`, `sv`, `tr`, `uk`, `vi` |
| Chinese (Simplified), Chinese (Hong Kong), Chinese (Traditional) | `zh` (`zh_CN`), `zh_HK`, `zh_TW` |

Locale tags are case-insensitive and may use `_` or `-`: for example, `pt-BR`,
`ja_JP`, `zh-Hant`, and `zh-MO` work. These are server-wide presets, not
per-player client locales.

For English screenshots, add `language: en` to `plugins/SimpleLift/config.yml`
and run `/lift reload` after installing this build. Interact with each elevator
to refresh its current-floor and destination labels. Existing destination
selections and user-written floor names are preserved. Rename floors yourself
if English names such as Lobby or Roof are needed. Standard messages, the action
bar, sign labels, command information, and reload responses use the chosen
language; technical diagnostic details may remain in English.

### Button-based arrival chimes

Wooden buttons play `warm`, a lower two-note bell. Stone buttons play
`crystal`, a bright three-note chime. Other materials use the original `legacy` bell.
`clear` is an optional brighter two-note bell; `silent` disables the arrival sound.
Warm and clear descend in a ding-dong pattern, with eight ticks between notes
(400 ms at 20 TPS), at the original `legacy` gain.
Explicit profile assignments in existing configurations are preserved.

Version 0.3.2 removes the extra volume reduction from warm and clear and raises
crystal's gain. `soundVolume` still controls the overall level; movement and
button-click sounds are unchanged. Built-in instruments approximate a bell,
not a recording of a real elevator.

Version 0.3.3 adds the opt-in `hotel` profile, using a low CHIME voice and a descending three-note major triad at
0/5/12 ticks (0/250/600 ms at 20 TPS). The longer final gap gives the phrase a
relaxed resolution; the second and third notes have 90% of the first's gain.

```yaml
arrivalChimes:
  default: legacy
  tags:
    "minecraft:wooden_buttons": warm
    "minecraft:stone_buttons": crystal
  materials:
    OAK_BUTTON: hotel
    STONE_BUTTON: legacy
```

An exact material wins over a tag. Tag priority is `minecraft:wooden_buttons`,
`minecraft:stone_buttons`, then `minecraft:buttons`. Unmatched materials use
`default`. Only those three vanilla block tags are accepted. Membership comes
from the server's tags, including data-pack changes, when settings load. Older
servers without `stone_buttons` use the known stone button materials instead.
An override material must exist and match `buttonBlocks` on that server.
This example gives oak buttons `hotel` while other wooden buttons keep `warm`.
Assignments apply to material types, not individual world coordinates. Overlap
between tags and materials is valid; do not repeat a material key in the same map.

An absent `tags` map receives the bundled wooden/stone assignments. An explicit
map replaces those assignments; use `tags: {}` to disable them all. Run
`/lift reload` after editing. Invalid profiles, tags, or materials reject the
reload and keep the previous settings. Existing configuration files receive
defaults in memory; they are not rewritten.

The button that starts the ride determines its chime, including redstone calls.
That profile and volume stay fixed for the ride. Each participating player hears
one arrangement at completion, including protected waiting occupants. Concurrent
lifts keep separate settings; a new chime for the same player cancels remaining
notes of the previous one. Logout, plugin shutdown, and successful configuration
reload cancel queued notes. `soundEnabled` and `soundVolume` still apply.

These short arrangements were composed for SimpleLift and use Minecraft's
built-in note-block instruments. No recordings, extracted game audio, sound
downloads, or resource pack are distributed. The arrangements are part of this
project's GPL-3.0 source; Minecraft's sound assets remain supplied by the client.
Click and movement sounds are unchanged. The new arrangements still need
in-game listening checks; the runtime matrix below covers the earlier build.

### Storage, network, and client behaviour

Established from the source, not from intent:

- **Files written:** exactly one — `plugins/SimpleLift/config.yml`, created on first start from a legacy import or from the bundled defaults. Nothing else is written to disk.
- **Persistent lift data:** none. There is no lift registry, player data file, or cache. Lift geometry is re-scanned from world blocks on every interaction, and the only persisted lift state is the text on the signs, which lives in the world save.
- **Databases:** none, local or remote. There is no JDBC, SQLite, or MySQL code and no database configuration.
- **Network:** none. The plugin contains no HTTP, socket, or `java.net` usage, no update checker, and no metrics or telemetry reporting. The Maven repositories in `build.gradle.kts` are used only when building from source.
- **Automatic dependency downloads:** none. The server API is a compile-only dependency and nothing is fetched at runtime.
- **Client software:** none required. There is no resource pack, mod, or plugin message channel. The action bar, chat messages, and sounds all use standard server-side APIs, and an action bar failure is caught and ignored rather than breaking the ride.
- **Threads:** all lift scanning and ride work runs on the server thread; a scan attempted off the main thread is rejected outright.

## Upgrading

**From Lift or LiftReloaded.** Follow the installation steps above. The first start imports the old `config.yml` into `plugins/SimpleLift/config.yml`. Elevators that use the preserved block and sign layout need no structure conversion. The old plugin folder is left in place and is not read again once SimpleLift has its own file.

**From an earlier SimpleLift build.** Stop the server, replace the JAR, start the server. Keys added by a newer release fall back to the bundled defaults, so an older `config.yml` continues to load; new keys can be copied from the defaults when you want to change them.

**Configuration edits.** Edit `plugins/SimpleLift/config.yml`, then run `/lift reload`. Check the console: a rejected file leaves the previous settings in place, so a reload that reports an error means your edit is not active.

Complete the [manual acceptance checklist](docs/manual-acceptance.md) on a disposable copy before rolling an upgrade out to production.

## Compatibility and limitations

Runtime-tested, from the [compatibility report](docs/compatibility.md):

| Server | Java | Result |
|---|---|---|
| Paper 1.13.2 build 657 | Temurin 11.0.32 | 38/38 runtime scenarios |
| Paper 1.16.5 build 794 | Temurin 17.0.17, version check overridden for the test | 38/38 runtime scenarios |
| Paper 1.20.6 build 151 | Temurin 21.0.9 | 38/38 runtime scenarios |
| Paper 1.21.8 build 60 | Temurin 21.0.9 | 38/38 runtime scenarios |
| Paper 26.2 build 112 | Temurin 25.0.4 | 38/38 runtime scenarios |

The 0.3.1-SNAPSHOT build passed 42 of 42 unit tests against the Spigot
`1.13.2-R0.1-SNAPSHOT` API on JDK 21 and the Paper `26.2.build.112-stable` API
on JDK 25, both targeting Java 8 bytecode. The runtime matrix above records
0.3.0-SNAPSHOT; it does not verify the new arrival chimes in game.

The 0.3.3-SNAPSHOT build, including the opt-in hotel chime, passed 47 of 47 unit tests in a clean
Spigot 1.13.2 API build. A separate clean build against Paper
`26.3.build.218-stable` on JDK 25 also passed all 47 tests. The existing
`0.3.3-26.3-beta.2` JAR was observed enabling normally on Paper 26.3 build 157;
its executable classes and resources match the published 0.3.3-SNAPSHOT JAR,
with differences limited to version metadata. This confirms startup, not the
full ride scenario matrix or client audio. In-game listening checks remain necessary.

Intended, but not individually runtime-tested: every other Minecraft release from 1.13 through 26.3, and the Bukkit, Spigot, and Purpur server families. The distributable compiles against the 1.13.2 API, uses no NMS or server-internal classes, and resolves the few APIs that changed — `World#getMinHeight`, plugin chunk tickets, chunk unload cancellation — reflectively. Treat untested versions as expected to work rather than verified.

Known limitations:

- **Folia is not supported.** A moving lift can cross region ownership boundaries, and region scheduling is not implemented. SimpleLift detects Folia on enable, logs the reason, and disables itself rather than running incorrectly.
- **Minecraft 1.12 and older cannot load the plugin.**
- **Hot reloads are not supported.** Use a full server restart or `/lift reload`. Unloading or replacing the plugin with a plugin manager while a lift is moving is untested, and removed floor blocks are only restored by the plugin's own shutdown path.
- **A forced process kill can strand temporary blocks.** A clean stop restores every removed floor block and every changed entity property; a kill signal, a crash, or a power loss during a ride cannot.
- **NoteBlockAPI music is not implemented.** `music: true` is accepted and warned about.
- **Scans are bounded.** A footprint larger than `maxLiftArea` is refused, and scanning cost grows with footprint area and shaft height. A 7 × 7 footprint with 95 floors spanning Y −60 to 316 was tested successfully; a 376-block ascent took 19.4 seconds at speed `1.0` and restored 4,606 temporary blocks.
- **Real-client checks remain manual.** Input handling, permissions, reconnect behaviour, and visuals are covered by the manual checklist, not by automated tests.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Plugin does not enable, console names Folia | Folia is unsupported. Use Paper, Spigot, Bukkit, or Purpur. |
| Plugin does not enable, console reports a configuration error | The message names the offending key. Fix it in `plugins/SimpleLift/config.yml` and restart. |
| `There is only one floor.` | Fewer than two floors were found. Check that upper floors have a floor block in a base column with a button two above and a sign above that, and that no unlisted solid block blocks the shaft. |
| `Floor does not exist. Check shaft for blockage` | The destination on the sign resolved back to the current floor, usually because the floor beyond it is cut off by an obstruction. |
| `Lift footprint exceeds maxLiftArea (N).` | The connected base blocks exceed `maxLiftArea`. Shrink the base or raise the limit, up to 4096. |
| Nothing happens when the button is pressed | No eligible passenger was standing on that floor inside the footprint. Stand on the floor block itself. If riding mobs or vehicles, check `liftMobs`. |
| `This lift is already moving.` | Another ride overlaps this shaft. Wait for it to finish. |
| `Another player is selecting a floor on this sign.` | Someone else holds the scroll session for that sign. |
| Scrolling does nothing | `mouseScroll` is off, the player lacks `lift.change`, the sign is more than 3 blocks away, or the session already ended. |
| The selection disappears by itself | `selection.timeoutSeconds` elapsed, the player moved away or changed world, or the configuration was reloaded. Set `selection.timeoutSeconds: 0` to disable the timeout. |
| `Can't leave elevator in use` / `Can't enter elevator in use` | `preventLeave` and `preventEntry` are doing their job. Turn them off if that is not wanted. |
| `It is unsafe to leave a vehicle in a lift!` | Dismounting inside a moving lift is blocked; wait for arrival. |
| `You have been teleported to the destination due to timeout` | The ride exceeded its expected travel time plus `secondsUntilTimeout`. Check the shaft for obstructions, or raise `secondsUntilTimeout`. |
| Sign lines look wrong or destinations do not stick | Break and replace the control sign, then right-click it once while it is blank to reinitialise the shaft. |

Set `debug: true` and reload for per-scan and per-ride logging when a problem is hard to pin down.

## Support

Report a problem with the server software and build number, the Java version, the full `plugins/SimpleLift/config.yml`, the relevant console output with `debug: true` enabled, and a description or screenshot of the elevator. The [compatibility report](docs/compatibility.md) and the [manual acceptance checklist](docs/manual-acceptance.md) list the checks that are expected to pass before a deployment.

Use the [GitHub issue tracker](https://github.com/tdbgo/simplelift-minecraft-plugin/issues) for reproducible bugs and compatibility reports.

## Building from source

Build and test against the oldest supported API:

```powershell
.\gradlew.bat clean build --console=plain
```

Cross-compile and test against the Paper 26.3 API with JDK 25:

```powershell
.\gradlew.bat clean test --console=plain `
  '-PapiGroup=io.papermc.paper' `
  '-PapiArtifact=paper-api' `
  '-PapiVersion=26.3.build.218-stable' `
  '-PjavaToolchainVersion=25' `
  '-PjavaRelease=8'
```

The build produces `SimpleLift-0.3.3-SNAPSHOT.jar` together with a sources JAR.

## License

SimpleLift is distributed under GPL-3.0-only. It includes code adapted from LiftReloaded by kikelkik, the successor to Lift by croxis, reworked and extended for SimpleLift. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
