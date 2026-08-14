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

Do not deploy when any item fails. Keep the configuration, server and Java versions, relevant logs, and elevator screenshots with the failure report.
