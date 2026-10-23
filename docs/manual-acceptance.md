# Manual acceptance checklist

[한국어](manual-acceptance.ko.md)

Run this checklist on a disposable server copy with the same permissions and plugin set as production.

## Prepare

1. Remove the Lift or LiftReloaded JAR and install only SimpleLift.
2. Copy an existing elevator and configuration into the test world.
3. Start the server normally and check the console for errors.
4. Run `/lift info` and `/lift reload`.

## Verify

- [ ] Right-clicking an empty control sign initializes every floor sign.
- [ ] Item right-click and empty-hand scrolling cycle the correct destinations.
- [ ] Sneak-scroll uses `selection.fastStep` and the swap-hand key confirms.
- [ ] The action bar shows the selected floor, name, and current floor.
- [ ] Selection ends after timeout, distance, damage, world change, or reload.
- [ ] Two users cannot control the same sign at the same time.
- [ ] Upward and downward rides restore gravity, flight, invulnerability, and collision state.
- [ ] Boats, minecarts, and allowed mobs travel without duplication.
- [ ] `lift`, `lift.change`, and `lift.reload` deny actions when missing.
- [ ] Logout or reconnect during travel ends at a safe location.
- [ ] `preventEntry` and `preventLeave` block entry and exit during travel.
- [ ] Redstone calls start only once per power change.
- [ ] Floors, rails, signs, and buttons are restored after each direction.
- [ ] Multiple riders from one floor arrive together.
- [ ] Another-floor calls receive `busy` and work after the active ride ends.
- [ ] Users on floors along the route remain in place and keep their physics state.

## Arrival chimes (0.3.3)

- [ ] Assign `hotel` to a button material, then to a tag or the default. Confirm the low descending three-note chime at 0/5/12 ticks and that material overrides take priority.

- [ ] Warm and clear each play two descending bell notes, eight ticks apart. Compare with legacy at the same global/client volume; both notes should be clearly audible without distortion. Warm should sound lower than clear.

- [ ] Switch `language` between `en`, `de`, `ja`, `pt_BR`, `zh_TW`, and `custom`, then reload. Verify chat, action bar, command output, and sign labels after interaction; floor names and destinations stay intact.

- [ ] Wooden and stone buttons play distinct arrangements at arrival; unassigned materials use `default`.
- [ ] An exact material override wins over tags; `silent`, `soundEnabled: false`, and volume zero mute arrivals.
- [ ] Two riders hear one sequence each; simultaneous shafts keep their own calling-button profile.
- [ ] Mixed-button floors and redstone calls use the initiating button, not a destination button.
- [ ] Logout, successful reload, and shutdown cancel trailing notes; invalid reload preserves settings.
- [ ] Listen on the oldest and newest supported clients for clipping, volume, timing, and missing instruments.

Do not deploy when any item fails. Keep the configuration, server and Java versions, relevant logs, and elevator screenshots with the failure report.
