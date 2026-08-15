# Changelog

All notable changes to SimpleLift are recorded here. Versions follow the value in `build.gradle.kts`.

This file currently covers one entry. No earlier SimpleLift release exists, so no history precedes it.

## 0.3.0-SNAPSHOT — unreleased

Development snapshot. The version carries the `-SNAPSHOT` suffix, is not a published release, and may change before it is tagged. No release date is set.

First tracked state of the plugin: a Lift/LiftReloaded-compatible elevator that derives every lift from world blocks at interaction time, with no plugin-side lift storage.

### Capabilities

- Lifts defined entirely by blocks: a base block at the lowest floor, a floor block in the same column at each higher floor, and a button with a wall sign directly above it on every floor. The footprint is the set of connected same-material base blocks, bounded by `maxLiftArea`.
- Floors discovered by scanning upward through the footprint columns and numbered from `1` at the lowest floor. Scanning stops at any solid block that is not a configured floor, button, sign, or shaft material, and is further bounded by `minHeight`, `maxHeight`, and the world's own build limits.
- Control signs carrying current floor, current floor name, destination floor, and destination floor name on four fixed lines. The destination level is parsed after the last `:`, so the label text remains translatable. Right-clicking a blank control sign initialises every uninitialised sign in the shaft.
- Two destination-selection modes on the same sign: right-click cycling while holding an item, and empty-hand scroll selection with action-bar feedback, sneak-scroll stepping by `selection.fastStep`, and optional hotbar-slot preservation. Selection is confirmed by the swap-hand key, a second sign click, or the lift button, and expires on inactivity, distance, world change, death, permission loss, disconnect, or configuration reload. One sign is held exclusively by one player.
- Group rides: every eligible entity standing on the departure floor inside the footprint travels together, with mobs and vehicles included unless `liftMobs` is disabled. Occupants waiting on floors along the route are held in place and protected while those floor blocks are temporarily removed; floors outside the route are untouched.
- Ride safety and restoration: entity gravity, invulnerability, flight permission, and flight state are captured before departure and restored afterwards; temporarily removed floor blocks, the non-sign block above them, and rails under carried minecarts are restored when the ride ends; overlapping calls to an active shaft are refused as busy; vehicle exit during a ride is cancelled; `preventEntry` and `preventLeave` gate movement into and out of a moving shaft; a ride exceeding its expected travel time plus `secondsUntilTimeout` completes by teleporting riders to the destination; disconnecting mid-ride places the player safely and reapplies that position on the next join.
- Optional redstone calls on a rising power edge at a lift button or its support block, gated by `redstone`.
- `/lift` with the `simplelift` alias, an `info` subcommand reporting version and supported range, a `reload` subcommand that validates before applying, and tab completion filtered by permission. Permissions are `lift`, `lift.change`, and `lift.reload`.
- Configuration validated on load with explicit bounds, rejecting unknown materials, duplicate base materials, invalid glob patterns, and out-of-range numbers. A failed `/lift reload` reports the offending key and keeps the last known-good settings active.

### Compatibility

- Targets Minecraft 1.13 through 26.2 via `api-version: '1.13'`; 1.12 and older cannot load the plugin.
- Compiled against the Spigot `1.13.2-R0.1-SNAPSHOT` API and emitted as Java 8 bytecode, so one artifact serves the whole supported range. No NMS or server-internal classes are used; APIs that differ across releases are reached reflectively and degrade quietly when absent.
- Runtime-verified on Paper 1.13.2, 1.16.5, 1.20.6, 1.21.8, and 26.2. Intermediate releases and the Bukkit, Spigot, and Purpur families are intended targets but were not individually verified. See the [compatibility report](docs/compatibility.md) for the matrix, the high-rise load results, and the remaining manual checks.
- Folia is detected on enable and the plugin disables itself, because a moving lift can cross region ownership boundaries and region scheduling is not implemented.
- No network access, no local or remote database, no runtime dependency downloads, and no client-side software. The only file written is `plugins/SimpleLift/config.yml`.

### Compatibility with Lift and LiftReloaded

- `plugin.yml` declares `provides: [Lift, LiftReloaded]`, so plugins depending on either name still resolve.
- Command name and alias, permission nodes, configuration keys, message keys, material glob syntax, and the sign line layout are preserved.
- On first start, when `plugins/SimpleLift/config.yml` is absent, an existing `plugins/LiftReloaded/config.yml` is imported, or otherwise `plugins/Lift/config.yml`; the imported path is logged. With neither present, the bundled defaults are written.
- Both `baseBlocks` layouts are accepted: the current list of `type`/`speed`/`music` entries and the version 1 map keyed by material.
- Elevators that use the preserved Lift/LiftReloaded block and sign layout need no structure conversion.

### Known limitations

- Folia is unsupported.
- NoteBlockAPI `.nbs` playback is not implemented. Legacy `music: true` entries load and emit a warning, and standard movement sounds are used instead.
- Plugin-manager hot reloads are unsupported; use a full server restart or `/lift reload`.
- A forced process termination during a ride can leave temporarily removed blocks unrestored, since restoration runs on the plugin's own shutdown path.
- Real-client input, permission, reconnect, and visual checks remain manual. See the [manual acceptance checklist](docs/manual-acceptance.md).
