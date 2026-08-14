# SimpleLift

**SimpleLift by PLAYCITY BLOCK**

[한국어](README.ko.md)

SimpleLift is a lightweight elevator plugin compatible with existing Lift and LiftReloaded structures. It detects floors from blocks, buttons, and signs. Existing elevators do not need a database conversion.

## Compatibility

- Minecraft 1.13 through 26.2
- Bukkit, Spigot, Paper, and Purpur-based servers
- One Java 8 bytecode JAR for all supported versions
- Runtime-tested on Paper 1.13.2, 1.16.5, 1.20.6, 1.21.8, and 26.2
- Folia is detected and rejected because region scheduling is not implemented

Not every intermediate Minecraft release was tested. See the [compatibility report](docs/compatibility.md) for the tested versions and limits.

## Install or replace Lift

1. Stop the server and back up the world and `plugins` directory.
2. Remove only the old Lift or LiftReloaded JAR.
3. Copy `SimpleLift-0.3.0-SNAPSHOT.jar` into `plugins`.
4. Start the server normally.
5. Run `/lift info` and test an existing elevator.

When `plugins/SimpleLift/config.yml` does not exist, SimpleLift imports the first available file below:

1. `plugins/LiftReloaded/config.yml`
2. `plugins/Lift/config.yml`
3. The bundled default configuration

Do not load SimpleLift together with Lift or LiftReloaded. Use a full server restart. Hot reload tools are not supported.

## Elevator layout

Each floor uses the same elevator footprint.

```text
floor + 3  [wall sign: current floor and destination]
floor + 2  [wall button]
floor + 1  [open space or optional floor-name wall sign]
floor      [baseBlocks on the lowest floor, floorBlocks elsewhere]
```

Place the control sign directly above the button. The lowest floor defines the elevator footprint with connected `baseBlocks`.

Right-click an empty control sign once to initialize all floor signs in the shaft.

## Use

- Hold an item and right-click the control sign to cycle the destination.
- With an empty hand, right-click the sign and scroll within three blocks to select a floor.
- Sneak while scrolling to move by `selection.fastStep` floors.
- Press the swap-hand key, right-click again, or press the elevator button to confirm.
- Press the button to travel to the selected floor.
- Riders inside the same elevator footprint travel together.
- Calls from another floor return `busy` until the active ride finishes.
- Set `redstone: true` to allow a powered button or its support block to call the elevator.

The action bar shows the selected floor, floor name, and current floor. Selection ends after eight seconds by default.

![SimpleLift hotbar and action bar design reference](docs/assets/selection-ui-mock.png)

The image is a design reference, not an in-game screenshot.

## Commands and permissions

| Command or permission | Purpose | Default |
|---|---|---|
| `/lift info` | Show the plugin version and supported range | `lift` |
| `/lift reload` | Validate and reload the configuration | OP |
| `lift` | Ride elevators and use the base command | Everyone |
| `lift.change` | Change a sign destination | Everyone |
| `lift.reload` | Reload the configuration | OP |

## Configuration

Important settings:

- `baseBlocks`: lowest-floor materials and movement speeds
- `floorBlocks`, `buttonBlocks`, `signBlocks`, `shaftBlocks`: material names or glob patterns
- `maxLiftArea`, `minHeight`, `maxHeight`: bounded scan limits
- `mouseScroll`, `selection.*`: floor-selection controls
- `preventEntry`, `preventLeave`, `liftMobs`, `redstone`: ride behavior
- `soundEnabled`, `soundVolume`: movement sounds
- `messages`: user-facing text and color codes

Legacy `music: true` entries remain valid. SimpleLift currently uses normal movement sounds instead of NoteBlockAPI `.nbs` playback.

## Build

Build and test against the oldest supported API:

```powershell
.\gradlew.bat clean build --console=plain
```

Cross-compile against Paper 26.2 with JDK 25:

```powershell
.\gradlew.bat clean test --console=plain `
  '-PapiGroup=io.papermc.paper' `
  '-PapiArtifact=paper-api' `
  '-PapiVersion=26.2.build.112-stable' `
  '-PjavaToolchainVersion=25' `
  '-PjavaRelease=8'
```

## Limits

- Minecraft 1.12 and older cannot load the plugin because `api-version` is `1.13`.
- Folia is not supported.
- A forced process termination can interrupt temporary floor restoration during a ride.
- Complete the [manual acceptance checklist](docs/manual-acceptance.md) before production use.

## License

SimpleLift is distributed under GNU GPL version 3. It is a compatibility successor to Lift by croxis and LiftReloaded by kikelkik. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
