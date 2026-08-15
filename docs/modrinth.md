# Modrinth submission package

Internal working document for publishing SimpleLift on Modrinth. The copy blocks below are ready to paste into the matching Modrinth fields; the checklist at the end is for the project owner and is not published anywhere.

---

## Project identity

| Field | Value |
|---|---|
| Project title | `SimpleLift` |
| Public byline | `SimpleLift by PLAYCITY BLOCK` |
| Project type | Plugin |
| Slug | Owner decision — `simplelift` if available |

## Summary field

Paste into Modrinth's short summary (keep within the field's character limit):

```text
Block-built elevators for Bukkit, Spigot, Paper, and Purpur servers. Lift/LiftReloaded compatible, with no database and no conversion step.
```

Alternate, shorter:

```text
Lift/LiftReloaded-compatible block elevators. No database, no structure conversion, one JAR for Minecraft 1.13–26.2.
```

## Full description

Paste into the Modrinth description editor. This block is reusable public copy and deliberately contains no production-process attribution.

---

**SimpleLift by PLAYCITY BLOCK**

SimpleLift is an elevator plugin for Bukkit-family Minecraft servers. It moves players, mobs, and vehicles between the floors of a shaft built entirely out of ordinary blocks. It preserves the configuration keys, command name, permission nodes, and sign layout used by Lift and LiftReloaded, so compatible existing elevators need no structure conversion.

### The elevator is the build

A lift is defined by a base block at the lowest floor, a floor block in the same column at every higher floor, and a button with a wall sign directly above it on each floor:

```text
floor + 3   wall sign — the control sign
floor + 2   button
floor + 1   free space, or an optional wall sign carrying the floor name
floor       base block (lowest floor) or floor block (every higher floor)
```

There is no lift database and no registration command. Every ride re-reads the structure from the world, so breaking a floor block or moving a sign changes the lift immediately. Floors are numbered from `1` at the lowest floor, and the connected base blocks define the shaft footprint.

### Why that matters

Lift definitions stay in the world instead of a separate registry, so world copies and rollbacks need no database synchronisation. Migration from Lift or LiftReloaded needs no structure conversion — only a config import that runs by itself on first start. One Java 8 bytecode JAR targets Minecraft 1.13 through 26.2.

### Picking a floor

The control sign shows the current floor, its name, the destination floor, and its name on four fixed lines. Right-clicking a blank control sign initialises every sign in the shaft — that is the only setup a lift needs.

Two selection styles work on the same sign:

- **Holding an item** — each right-click advances the destination by one floor and writes it to the sign immediately.
- **Empty hand** — right-click to open a scroll session, then scroll the hotbar to pick a floor, with the choice shown on the action bar. Sneak-scroll jumps several floors at a time, and the held hotbar slot stays put. Confirm with the swap-hand key, a second sign click, or the button.

A scroll session belongs to one player at a time and ends on its own after a period of inactivity, or when the player walks away, changes world, or disconnects.

### Riding

- Everyone standing on the departure floor inside the footprint travels together. Mobs and vehicles come along unless you turn that off.
- Anyone waiting on a floor the lift passes through is held in place and protected while that floor block is temporarily removed. Floors outside the route are never touched.
- Temporarily removed floor blocks, the block above them, and rails under a carried minecart are restored when the ride ends, as are rider gravity, invulnerability, and flight state.
- A call to a shaft that is already moving is refused until the ride finishes.
- Optional settings block entering or leaving a moving shaft, and optional redstone control lets a powered button call the lift.

### Commands and permissions

| Command | Effect | Permission |
|---|---|---|
| `/lift` or `/lift info` | Plugin version and supported range | `lift` |
| `/lift reload` | Validate and re-read the configuration | `lift.reload` |

`/simplelift` is an alias. Permissions are `lift` (ride and use the base command, default everyone), `lift.change` (change a sign destination, default everyone), and `lift.reload` (default operators). A configuration that fails validation is rejected with the offending key named, and the last known-good settings stay active.

### Configuration, storage, and network

The only file written is `plugins/SimpleLift/config.yml`. There is no lift registry, player data file, or cache — lift geometry is re-scanned from world blocks on every interaction, and the only persisted lift state is the sign text in the world save.

SimpleLift makes no network connections. There is no update checker, no telemetry, no local or remote database, and no runtime dependency downloading. No client-side mod or resource pack is required.

Configurable behaviour includes floor/base/button/sign/shaft materials (plain names or `*` globs), per-material travel speed, scan bounds, scroll-selection controls, ride restrictions, redstone calls, sounds, and all player-facing messages.

### Compatibility

Runtime-tested on Paper 1.13.2, 1.16.5, 1.20.6, 1.21.8, and 26.2. Not every intermediate Minecraft release was individually tested — treat untested versions as expected to work rather than verified.

Built against the Spigot 1.13.2 API and shipped as Java 8 bytecode, using no NMS or server-internal classes. APIs that changed between releases are reached reflectively and degrade quietly when absent.

**Folia is not supported.** A moving lift can cross region ownership boundaries and region scheduling is not implemented, so SimpleLift detects Folia on enable and disables itself. Minecraft 1.12 and older cannot load the plugin.

### Installing

1. Stop the server and back up the world and `plugins`.
2. Remove any old Lift or LiftReloaded JAR — do not run them alongside SimpleLift.
3. Drop the SimpleLift JAR into `plugins` and start the server.
4. Run `/lift info` and ride an existing elevator.

On first start, if `plugins/SimpleLift/config.yml` is absent, SimpleLift imports `plugins/LiftReloaded/config.yml`, or otherwise `plugins/Lift/config.yml`, and logs which one it used. Use a full server restart rather than a plugin-manager reload.

### Known limitations

- Folia is unsupported.
- NoteBlockAPI `.nbs` playback is not implemented; legacy `music: true` entries load with a warning and standard movement sounds are used.
- Plugin-manager hot reloads are unsupported.
- A forced process termination during a ride can leave temporarily removed blocks unrestored.

### License and credits

GNU General Public License v3.0 only. SimpleLift is a compatibility successor to Lift by croxis and LiftReloaded by kikelkik, preserving their attribution, configuration keys, permission nodes, command names, and sign layout.

---

## Version submission

| Field | Value |
|---|---|
| Version title | `SimpleLift 0.3.0-SNAPSHOT` |
| Version number | `0.3.0-SNAPSHOT` |
| Release channel | **Beta** |
| Featured | Owner decision; suggest no while the channel is Beta |
| Version file | `SimpleLift-0.3.0-SNAPSHOT.jar` from `.\gradlew.bat clean build` |

**Why Beta.** The version carries a `-SNAPSHOT` suffix and is not a tagged release, and the manual acceptance checklist covering real-client input, permission, reconnect, and visual behaviour has not been signed off in this repository. Do not publish it in the Release channel until that changes.

**Owner decision before upload:** whether to publish a snapshot version number at all, or to cut a non-snapshot version first. Publishing `0.3.0-SNAPSHOT` as-is means the uploaded file name, the plugin's self-reported version, and the Modrinth version number all agree, which is the reason to keep it; the counter-argument is that a `-SNAPSHOT` suffix reads as a build artifact to users browsing the page.

### Version changelog draft

Paste into the version changelog field:

```markdown
Development snapshot — not a tagged release.

First published state of SimpleLift.

**What it does**
- Elevators defined entirely by blocks: a base block at the lowest floor, a floor block per column above, and a button with a wall sign above it on every floor. No lift database and no registration step — the structure is read from the world on every ride.
- Four-line control signs showing current and destination floors, initialised by a single right-click on a blank sign.
- Two ways to pick a floor: right-click cycling with an item in hand, or empty-hand scroll selection with action-bar feedback, sneak-scroll fast stepping, and confirmation via the swap-hand key, a second click, or the button.
- Group rides: everyone on the departure floor travels together, occupants waiting on floors along the route are held in place and protected, and floors outside the route are untouched.
- Temporarily removed blocks and rider gravity, invulnerability, and flight state are restored when a ride ends. Calls to a shaft that is already moving are refused until it finishes.
- Optional redstone calls, entry/exit restrictions during travel, configurable materials and speeds, and fully configurable messages.
- `/lift info` and `/lift reload`, with `lift`, `lift.change`, and `lift.reload` permissions. A configuration that fails validation is rejected and the last known-good settings stay active.

**Lift and LiftReloaded compatibility**
- Declares `provides: [Lift, LiftReloaded]`.
- Command name and alias, permission nodes, configuration keys, message keys, material glob syntax, and sign line layout are preserved.
- On first start, an existing LiftReloaded or Lift `config.yml` is imported automatically. Existing elevators need no conversion.

**Compatibility**
- Minecraft 1.13 through 26.2; 1.12 and older cannot load the plugin.
- Runtime-tested on Paper 1.13.2, 1.16.5, 1.20.6, 1.21.8, and 26.2. Other releases in the range were not individually tested.
- One Java 8 bytecode JAR, built against the Spigot 1.13.2 API, with no NMS usage.
- Folia is detected and the plugin disables itself.

**Known limitations**
- Folia unsupported; NoteBlockAPI `.nbs` playback not implemented; plugin-manager hot reloads unsupported; a forced process kill during a ride can leave temporarily removed blocks unrestored.
```

## Metadata

| Field | Value |
|---|---|
| Loaders | `Bukkit`, `Spigot`, `Paper`, `Purpur` |
| Explicitly **not** tagged | `Folia` — the plugin disables itself on Folia |
| Game versions | `1.13` through `26.2` |
| Environment — client | **Unsupported** — no client mod or resource pack is involved |
| Environment — server | **Required** |
| License | `GPL-3.0-only` |
| Source code | https://github.com/tdbgo/simplelift-minecraft-plugin |
| Issue tracker | https://github.com/tdbgo/simplelift-minecraft-plugin/issues |
| Wiki / Discord | Owner decision; omit the field if none exists rather than pointing it somewhere unmaintained |

**Game version caveat — read before selecting versions.** Runtime testing covered Paper 1.13.2, 1.16.5, 1.20.6, 1.21.8, and 26.2 only. The plugin declares `api-version: '1.13'` and is compiled against the Spigot 1.13.2 API as Java 8 bytecode, so the intermediate releases are expected to work but were not individually verified. If you select the full 1.13–26.2 range in the version form, keep the "not every intermediate release was tested" sentence in the description so the claim on the page matches what was actually verified. Selecting only the tested versions is the conservative alternative.

### Categories

Verify against Modrinth's current taxonomy at submission time; the tag set changes.

- Primary: `Transportation`
- Secondary: `Utility`
- Consider: `Game Mechanics`

Do not tag `Optimization` — no performance work is claimed or measured for that purpose.

---

## Owner submission checklist

Internal only. Do not paste any part of this section into the Modrinth page.

### Disclosures — mandatory

- [ ] **Enable the Modrinth "Contains AI-generated content" disclosure on the project.** The project description and publishing copy were prepared with generative assistance, which Modrinth Content Rules §6.1 requires be disclosed. This is not optional and must be set before the project is submitted for review.
- [ ] **Use no AI-generated or AI-derived image anywhere on the project page** — icon, gallery, screenshots, banners, or images embedded in the description. Modrinth Content Rules §6.2 prohibits AI-generated imagery on project pages. Every image must be an original screenshot, an original hand-made asset, or a properly licensed third-party asset with attribution.
- [ ] Confirm the public description and version changelog contain no production-process attribution. That disclosure belongs in the Modrinth disclosure setting and in this checklist, not in the reusable copy.

### Icon

- [ ] Provide a square icon meeting Modrinth's size and format requirements.
- [ ] Confirm it is not AI-generated or AI-derived (§6.2).
- [ ] Confirm the rights to use it, and that it does not reuse Lift or LiftReloaded branding.

### Gallery

- [ ] Capture in-game screenshots on a server running the current build. Suggested shots: a two-floor lift with its control sign, the four-line sign text, the action bar during scroll selection, a wide shot of a multi-floor shaft.
- [ ] Do not upload `docs/assets/selection-ui-mock.png` as a gallery image without labelling it. It is a design reference, not an in-game screenshot, and presenting it as gameplay would misrepresent the plugin.
- [ ] Write a caption for each image.
- [ ] Confirm no gallery image is AI-generated or AI-derived (§6.2).

### Links

- [ ] Source: https://github.com/tdbgo/simplelift-minecraft-plugin — confirm the repository is public and its default branch matches the uploaded build.
- [ ] Issues: https://github.com/tdbgo/simplelift-minecraft-plugin/issues — confirm issues are enabled.
- [ ] Confirm both URLs resolve before submitting. Leave optional link fields empty rather than filling them with placeholders.

### Metadata

- [ ] Title `SimpleLift`; confirm the byline `SimpleLift by PLAYCITY BLOCK` appears in the description.
- [ ] Summary set, within the character limit.
- [ ] Loaders: Bukkit, Spigot, Paper, Purpur. Folia not selected.
- [ ] Game versions selected, and the description's testing caveat matches the range chosen.
- [ ] Categories confirmed against the current taxonomy.
- [ ] Client: unsupported. Server: required.
- [ ] License set to `GPL-3.0-only`, matching `LICENSE` in the repository.
- [ ] Confirm the credits to Lift (croxis) and LiftReloaded (kikelkik) survive any description edits, consistent with `THIRD_PARTY_NOTICES.md`.

### Version file

- [ ] Build with `.\gradlew.bat clean build --console=plain` and upload `SimpleLift-0.3.0-SNAPSHOT.jar`.
- [ ] Confirm the uploaded file's `plugin.yml` version matches the Modrinth version number.
- [ ] Do not upload the sources JAR as the primary file.
- [ ] Version title, number, and Beta channel set as above.
- [ ] Version changelog pasted.
- [ ] Resolve the snapshot-version-number decision recorded above before uploading.

### Pre-review state

- [ ] Complete `docs/manual-acceptance.md` on a disposable server copy and keep the results.
- [ ] Verify the description's claims still match the build being uploaded, in particular the compatibility list, the storage and network statements, and the known limitations.
- [ ] Confirm no support, performance, or security guarantee has crept into the copy. The description states what the plugin does, not what it promises.
- [ ] Submit for review only after every disclosure item above is done.
