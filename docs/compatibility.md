# Compatibility

[한국어](compatibility.ko.md)

SimpleLift 0.3.0-SNAPSHOT targets Minecraft 1.13 through 26.2 with one Java 8 bytecode JAR.

The implementation uses the Spigot 1.13.2 API and a small compatibility layer. It does not use NMS or Minecraft implementation classes. Not every intermediate release was tested.

## Verified matrix

| Target | JVM or build condition | Result |
|---|---|---|
| Spigot API `1.13.2-R0.1-SNAPSHOT` | JDK 21, `--release 8` | Clean build and JUnit 19/19 |
| Paper API `26.2.build.112-stable` | JDK 25, `--release 8` | Cross-compile and JUnit 19/19 |
| Paper `1.13.2`, build 657 | Temurin 11.0.32 | Runtime scenarios 38/38 |
| Paper `1.16.5`, build 794 | Temurin 17.0.17 with Paper version check override | Runtime scenarios 38/38 |
| Paper `1.20.6`, build 151 | Temurin 21.0.9 | Runtime scenarios 38/38 |
| Paper `1.21.8`, build 60 | Temurin 21.0.9 | Runtime scenarios 38/38 |
| Paper `26.2`, build 112 | Temurin 25.0.4 | Runtime scenarios 38/38 |

Paper 1.16.5 officially supports older Java versions. The isolated test used `-DPaper.IgnoreJavaVersion=true`. Production servers should follow the Java requirements of their Paper version.

## Multiple users

Runtime scenarios verified these cases:

- Multiple users on one floor travel together.
- Users waiting at the destination are protected while the temporary floor is removed.
- Users on unrelated floors are not attached to the ride.
- Another call to the active shaft receives `busy` until the ride finishes.
- Physics and temporary floor blocks are restored after normal completion and repeated rides.
- Sign selection sessions are independent between signs and exclusive per sign.
- A legacy LiftReloaded configuration can be imported on first start.

## Height and load

Paper 26.2 was tested with a 7 × 7 footprint and 95 floors from Y=-60 to Y=316. The travel distance was 376 blocks.

- 21/21 high-rise scenarios passed.
- A 376-block ascent took 19.427 seconds at speed `1.0`.
- 4,606 temporary floor blocks were restored.
- Two riders and one user waiting at the top floor retained their physics state.

The floor count is not fixed. `minHeight`, `maxHeight`, and the world build height define the scan range. Scan cost grows with height and footprint area.

## Compatibility strategy

- Java class version 52
- Spigot 1.13.2 compile baseline
- Reflection only for APIs that do not exist on old servers, such as `World#getMinHeight`
- Version-neutral material and sound lookup
- No bundled Bukkit, Paper, or NMS classes
- `api-version: '1.13'`
- Safe disable on Folia

## Remaining limits

- Real-client input, permission, reconnect, and visual checks remain manual.
- Folia region scheduling is not implemented.
- NoteBlockAPI `.nbs` playback is not implemented.
- Forced process termination cannot guarantee restoration of temporary blocks.

Use the [manual acceptance checklist](manual-acceptance.md) before production deployment.
