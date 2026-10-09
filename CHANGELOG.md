# Changelog for gottsch's Monster Manual (Forge 1.20.1)

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-08-20

### Added
- 🗿 **Stone Colossus** — a tall, lean armoured construct that stands as scenery until someone walks
  up to it. Slow, immovable (knockback resistance 1.0) and heavily armoured rather than
  high-health: 120 HP behind 14 armour and 6 toughness, a low attack rate, and speed under the
  Minotaur's. Deliberately the Minotaur's opposite — that mob owns the charge, and two brutes
  sharing one tell would be one mob with two models.

  **Dormancy is the mob**, not a spawn optimisation. `IDormantMob` (modelled on `IChargingMob`,
  and for the same reason — the pose is a client-side read) carries synched `dormant` and `waking`
  **flags**; `DormancyGoal` holds the mob still, wakes it when a non-creative player comes within
  8 blocks, and counts the rise out server-side.

  Two things in there are worth copying for the next ambush mob:
  - **A goal, not `setNoAi`.** NoAi stops the mob being ticked as a mob, which also stops gravity —
    a statue spawned a block above its floor would hang there for ever. The goal claims every flag
    the goal selector hands out at priority 0 instead, so the mob still falls, burns and takes
    damage while owning no behaviour. It cannot starve the *target* goals, since `Mob` keeps a
    separate `GoalSelector` for those with its own locked flags, so it clears the target each tick.
  - **A flag and a per-side clock, not a synched tick.** Two earlier cuts were wrong. Synching a
    counter and decrementing it every tick made the client see twenty discrete values a second, so
    the rise visibly stepped. Synching the server's `tickCount` and measuring it against the
    model's own `ageInTicks` was worse and subtler — `tickCount` is per side, so a colossus woken
    out of view or seen after a chunk reload read a start tick far in its own future and stayed a
    statue for ever. What works is the slam's shape: a synched boolean, the server counting the
    window in `DormancyGoal`, and each client starting its own clock the tick it first sees the
    flag. The animation is smooth at any frame rate and costs one packet instead of one per tick.

  `StoneColossusModel` extends `HumanoidModel` for its *animation* the way `MinotaurModel` does —
  seven empty vanilla parts, posed by `super.setupAnim`, read back onto the real rig. Its rig
  fields are `rig`-prefixed: same-named fields would **hide** the inherited ones rather than
  replace them, leaving two parts one keystroke apart, one of which draws nothing. The statue is a
  distinct pose (head bowed, shoulders forward, forearms drawn up) that the rise smoothsteps out
  of, and the six belt cloths are pushed by the leg on their own side, each only in the direction
  that leg can actually push it. Its chest and belt gems are a full-bright layer
  (`StoneColossusGemLayer`) **gated on dormancy** — dark while it is scenery, lit from the first
  tick of the rise, so a player can tell a statue from a boss that has already noticed them.

  Damage rules are the mob's identity too: `IS_PROJECTILE` damage ×0.5 ("arrows chip, they do not
  wound") and `minecraft:pickaxes` ×1.75 — the answer to a thing made of stone is to mine it. Any
  damage wakes it, so it can never be shot to death as a statue. Construct: immune to poison and
  wither, *not* fire.

  GMM ships the class, rig and texture; it registers nothing, so the size it is drawn at belongs to
  the consumer — Dungeons2 draws it at 7 blocks from a 2.75-block rig.

- 🎯 **`StalkGoal`** — keeps a mob pressing toward a target it *cannot reach*, standing as close
  under it as the floor allows instead of stopping wherever its path gave out. Written for the
  arena case that reads worst: a player up on a ledge or gallery plinking at a melee boss that
  stands in the middle of the room doing nothing. The stalk point is found by probing **back** from
  the target one block at a time until the navigation reports a reachable point, so it lands under
  the player rather than at the end of a path that quit early; the reach test and the probe are
  both cached on cooldowns, since each is a full path search. Ranks above the melee goal — a
  lower-priority goal can never take a flag a higher one holds — and its entry test is melee's
  exact complement, so it can never starve a mob that could be attacking instead.

- 📏 **`TallMeleeAttackGoal`** — melee reach for mobs tall enough that vanilla's test lies to them.
  Vanilla measures one 3D distance from the mob's feet to the target's feet, which for a seven-block
  construct counts its own height against it: a player standing level with its head is an arm's
  length from its face and six blocks away by that arithmetic, so it never swings and instead walks
  about below them looking for level ground (found in play, 2026-09-13). This splits the question —
  the vanilla reach horizontally, and vertically anywhere the mob's own body spans plus a block at
  each end. It does **not** widen the horizontal reach, so nothing becomes hittable that was not
  already hittable from the same floor.

  It also takes an optional **swing interval**, because vanilla's is a hard-coded 20 ticks inside
  `resetAttackCooldown()` and the field it sets is private — there is no override that changes it,
  so the goal keeps its own counter instead. A heavy mob should not swing at a light mob's rate.
  The counter deliberately survives goal restarts: melee stops and starts constantly in a real
  fight, and clearing it on entry would let a mob swing every time it re-engaged.

- 💥 **`GroundSlamGoal`** — the Stone Colossus's signature attack: wind up with both fists overhead,
  then hit everything in a radius at once. A single-target swing is beaten by circling; an area is
  beaten by *distance*, which turns the fight from a dodging problem into a positioning one. The
  wind-up is the point rather than a cost — the model raises the fists over exactly the ticks the
  goal counts before striking (`ISlammingMob` publishes that number, so the animation and the blow
  can never drift apart), so what the player sees **is** the timer. Damage falls off toward the rim
  but never to nothing, knockback lifts as well as shoves, and everything caught takes Slowness.

  Two consumer statics, unset by default: `damageSource` (a consumer with its own damage type gets
  attribution, a death message and something its gear can test for) and `blockEffect` — **whether a
  slam may break the world is a property of the world**, so a library that cannot know whose walls
  these are breaks nothing at all until told otherwise.

  Hits players and the mob's current target only, like the Annihilation Ray: a boss that knocks its
  own escort about disarms its own encounter, and stray hits turn `HurtByTargetGoal` into infighting
  the player watches instead of joining.

- 👊 **`IBurstingMob`** — the pose hook for a mob that takes a wall down with one wound-up punch,
  and the Stone Colossus's `burstPose`. Same timing contract as `ISlammingMob` (a synched flag, a
  server that counts its own ticks, a client that starts its own clock on the rising edge), because
  that contract is the fix for a real bug and not a style; **what differs is the pose, and it
  differs in the one way that matters** — a slam drives both fists into the FLOOR, a burst drives
  one fist horizontally into something at chest height. Reusing the slam's flag would have cost
  nothing to write and shown a wall falling while the mob punched the ground in front of it.

  The arm cocks back and **holds** — the hold is the tell, the same shape the slam's raise uses —
  with the striking shoulder drawn back, then comes through on the impact tick while the head
  counter-rotates and keeps watching the wall. Which fist is **synched rather than derived**: taking
  it from the animation clock would have been free and would have let two players watching the same
  blow see different arms, since each client's clock starts when the entity loaded there.

  A mob may implement both interfaces. They must not run at once, and **the model resolves that
  rather than the goals** — the two poses own the same arms, which is a fact about the rig that no
  goal can see. The slam wins: it is the attack that hurts somebody, and a wall that comes down a
  blow late costs nothing. The consumer of all this is Dungeons2's `SmashBlocksGoal`, which is where
  the decision that these walls may be broken belongs.

- 🪨 **`IHurlingMob` and `ThrowBoulderGoal`** — a heave and a throw for a mob that does NOT
  chase. `ThrowProjectileGoal` was already there and is the wrong shape for this: it is a thrower's
  whole fight (approach when out of range, back away when crowded, fists at point blank), and a slow
  brute that owns one room must not retreat and already has goals for closing and for point blank.
  What was missing is only the throw — stand still, heave, let go — so this is the narrow version.
  It **roots** the mob for the whole blow, requires **line of sight**, and has a range FLOOR as well
  as a ceiling, because a mob lobbing rocks at someone standing on its foot reads as broken.

  The pose is the third member of the slam/burst family and shares their timing contract. It reuses
  `ThrowProjectileGoal.ProjectileLauncher` rather than declaring a second identical interface, and
  the projectile stays the consumer's: whether a thrown rock may break the wall it lands on is a
  property of the world, so a library that cannot know whose walls these are throws nothing of its
  own.

- 🐂 **Minotaur** — a bull-headed brute built to close distance violently. Slow, heavy, hard to
  stagger and hard to run from: high knockback resistance, an attack that throws its target, and a
  charge (below). Ships with `MinotaurModel`, `MinotaurRenderer` and the `gmm:minotaur/weapons` item
  tag.

  It swings every **30 ticks**, not vanilla's 20: twelve damage landing three times in two seconds
  reads as a flurry rather than a heavy blow. Its melee goal is `TallMeleeAttackGoal` (below), since at
  2.6 blocks a player on a one-block step is measured further away than they look. Consumers that
  replace the Minotaur's melee goal (Dungeons2 swaps it for a pursuing one) must re-add the *tall*
  goal, not the vanilla one: `TallMeleeAttackGoal` is a `MeleeAttackGoal`, so a filter that removes
  the latter takes this with it.

  The tag is **deliberately empty in gmm**, unlike every other weapon pool here. The Minotaur has a
  signature weapon — a great axe — and the art for it lives in the consumers, so a vanilla-axe
  default would mean a consumer supplying the real thing still rolled a plain one two times in three.
  A gmm install with no consumer leaves the Minotaur bare-fisted, which
  `Minotaur#populateDefaultEquipmentSlots` documents as an intended look rather than a gap.

  `MinotaurModel` extends `HumanoidModel` for its *animation*, not its geometry: the seven vanilla
  part names are registered with **no cubes**, `super.setupAnim` poses them, and the real parts read
  the rotations back out. `translateToHand` walks the real arm chain, since the inherited version
  applies only the (now empty) vanilla arm's transform and would drop a held item at the mob's feet.
  The digitigrade leg half-cancels its thigh swing at the foot so the hoof stays level through a
  stride; the head is damped and clamped rather than tracking like a biped's; the two-segment tail
  gives its lower joint the base's angle from a fixed phase earlier, which is what makes it read as a
  tail rather than a rod.

- 🏃 **`ChargeAttackGoal`** — a wind-up, a run and one heavy hit, usable by any `PathfinderMob`.
  Fires only from the middle distance, so it is a way to *close* distance rather than a second melee
  attack. It stops the mob dead for the wind-up (the telegraph), then **locks the target's position**
  at the moment the run starts — a charge that steers is a homing missile, and a dodge should be
  rewarded. Movement goes through `PathNavigation` rather than `deltaMovement`, so the charge follows
  the floor, handles a step up, and does not bury the mob in a corridor wall. The impact reuses
  `doHurtTarget`, so armour, enchantments, damage events and knockback resistance all behave
  normally.

  Its extra damage is a **flat** `ADDITION`, not a multiplier, and this matters:
  `AttributeInstance.calculateValue` applies `MULTIPLY_BASE` *after* every `ADDITION`, and a held
  weapon's damage is an `ADDITION`. A percentage bonus would therefore compound with whatever is in
  the mob's hand, so arming a charging mob better would silently make its charge better, without
  limit and without anyone choosing it.

- 🐂 **`IChargingMob`** — opt-in pose hook for a mob that wants a visible tell while
  `ChargeAttackGoal` is winding up and running. Same contract as `ICastingMob`: called only via
  `instanceof`, never a hard cast, so a charging mob that wants no pose change simply does not
  implement it.

- 🗡️ **Orc Warlord** — the war-chief of an orc band: an `Orc` that leads rather than swings
  first. Three abilities that are one fight rather than three buttons. He **calls** reinforcements,
  he **rallies** what arrives (a periodic Strength + Speed refresh over a data-driven ally set), and
  he **alerts** that same set the moment he acquires a target — so spotting the chief is being
  spotted by the whole warband. He also keeps to the *edge* of his own troops (`AvoidCrowdGoal`), so
  a player's sweep or AoE catches grunts instead of him.

  **Extends `Orc`, not `GMMMonster`, and that is load-bearing:** `OrcModel.setupAnim` casts its
  entity to `Orc` to read the shoulder-pad/hair/bracer bits, so a warlord that were not an Orc would
  crash the renderer on first sight of one. It also inherits the cosmetic roll, the data-driven
  weapon and the ranged-throw option, which is why he reads as *his own warband's* leader rather
  than a different monster wearing red. The two ally-alerting target goals **replace** the pair `Orc`
  installs rather than sitting above them — both share `Goal.Flag.TARGET`, so a leftover pair would
  never run and would read as intent to whoever came next.

  Rally cadence and crowd-spacing are `SkeletonChampion`'s deliberately, down to the same
  `gmm:mob_config` keys — two pack leaders behaving by different rules would be two things to learn
  for no gain. The **summon is four times the rally's cadence, not twice**: the rally refreshes a
  buff and can afford to be frequent, while the horn adds *bodies*, and bodies do not expire.

  **Two consumer hooks, and both fail silently** — the same trap as `Orc.projectileLauncher`, so
  they are documented on the class rather than left to be discovered:
  `OrcWarlord.summonMobs` (unset, the horn goal is never added and the chief never calls anyone) and
  the `gmm:orc_warlord/rally_allies` entity-type tag, which gmm ships **empty** because gmm registers
  no entities (unpopulated, both the rally and the alert reach nothing). A warlord with neither is a
  slightly tough orc that looks like a boss.

  Ships with `OrcWarlordRenderer` (a visual-only 1.2× scale over the rank and file — consumers should
  size the hitbox as a plain Orc, or the chief wedges in the doorways his own warband walks through)
  and the `gmm:orc_warlord/weapons` item tag, defaulting to iron/diamond/netherite axes so a chief
  carries a real weapon rather than the warband's chipped stone one. An emptied pool falls back to
  `gmm:orc/weapons` rather than to bare fists.

  He always spawns in his **full kit** — both shoulder pads, the hair and the bracers — because the
  kit is what tells a player at a glance which orc is the chief. `Orc#finalizeSpawn` rolls each bit
  as an independent coin flip, so `OrcWarlord#finalizeSpawn` sets all four *after* calling `super`.
  The bits are named by `Orc.ALL_APPAREL`, composed from the four position constants (now
  `protected`) rather than written as `0b1111`, so a fifth model part is worn by whoever asks for the
  full set. `ALL_APPAREL` includes HAIR, which is not armour: the byte is one model-part field.

- ⚓ **Anchoring, for every GMM mob** — a consumer can post any GMM mob to guard a position by
  calling vanilla's own `Mob#restrictTo(BlockPos, int)` on the instance it just placed. An anchored
  mob keeps to its post while idle and never despawns; an unanchored one is untouched, so this is
  opt-in per *instance*, not per mob type. New `Anchor` helper plus four members on `GMMMonster` and
  `GMMFlyingMonster`.

  Vanilla leaves two holes that make `restrictTo` alone insufficient, and both are now closed in one
  place: `Mob#checkDespawn` never consults the restriction, so a posted guardian wandered off the
  despawn cliff like any natural spawn; and the restrict centre/radius are persisted by nothing at
  all (`Entity#saveWithoutId` writes `Glowing`, `Silent`, `NoGravity` and `Invulnerable` — vanilla
  only ever sets a restriction on villagers, whose brain re-derives a home from a bed), so a
  guardian came back from a chunk unload free to wander and free to despawn.

  Anchoring is deliberately **not a leash**. The radius gates despawning and the positions the
  wander goals pick — `WaterAvoidingRandomStrollGoal` runs its candidates through
  `isWithinRestriction`, which is what keeps an idle guardian in its room with no extra goal — and
  it is suspended while the mob is engaged. Left unqualified it does something worse than leash: a
  guardian that chased an intruder out of its room spends the rest of its life trying to wander
  back, mid-fight included. `isAnchorSuspended()` is the overridable hook, defaulting to "has a
  target"; a mob with a dormant/active split of its own overrides it. No goals are installed — a
  consumer wanting a mob to actively walk back to its post adds `MoveTowardsRestrictionGoal`.

- 🎨 **Art for the Paralysis, Harm, Disintegrate and Disarm spells** —
  `gmm:item/paralysis_spell`, `harm_spell`, `disintegrate_spell` and `disarm_spell`, animated
  (each with its `.mcmeta`), from Dungeon Denizens (where Paralysis's is `slow.png`). gmm still
  registers no items: a consumer registers an art-only item whose model points at the texture and sets
  the spell's `itemSupplier`, exactly as for `rock` and `withering_gaze_spell`. Left unset, the
  spells keep drawing as a fire charge — which reads as the wrong spell, since Fire Spout is the one
  that really is fire.

- 🎯 **`GMMDamageTypes.source(level, key, direct, causing)`** — the entity-carrying form, for
  anything thrown. The entity-less `source(level, key)` has no position, and vanilla's shield test
  (`LivingEntity#isDamageSourceBlocked`) answers "not blocked" the moment the source position is
  null, because it cannot tell whether the hit came from in front.

- 🟪 **Black Pudding** — the ooze family's fastest and deadliest member. Where the Gelatinous Cube
  is a slow trap and the Gray Ooze a stationary ambusher, this one actively runs you down: 30 HP,
  7 attack damage, and 0.22 movement speed — the family's fastest by a clear margin (Ochre Jelly's
  0.18 was the previous high), but deliberately still under a vanilla Zombie's 0.23, so it outpaces
  the other oozes without outpacing the player. Walking away from it stays a viable option.
- Two mechanics on top of the stat line, each deliberately bounded so they don't cancel the other's
  counterplay:
  - **Tendril lash** — an adult strikes from `reachBonus` blocks (default 1.0) beyond normal melee
    range, so backpedaling doesn't buy the usual safety margin. Note
    that vanilla's base range for this mob is already 1.96 blocks centre-to-centre, so the bonus
    lands the strike at ~2.96 blocks — about 2.2 blocks of visible air.
    The strike is animated by a **pseudopod** drawn procedurally by the new shared `TendrilRenderer`:
    a tapered tube generated at runtime between the body's front face and the victim, whipping out
    along an arc that bows mid-strike and straightens as it lands, then retracting. Not cuboid
    geometry — a stretched `ModelPart` can only ever be a straight line, and cannot arc at all.
    No wind-up — damage lands first and the strike follows through, the way a zombie's swing *is* its
    hit, rather than telegraphing like the Bodak's death gaze. Crucially it
    fires off a **landed-hit counter, not vanilla swing state**: `MeleeAttackGoal` swings before
    `doHurtTarget` and the cooldown gate rejects roughly half those attempts, so animating off the
    swing would show a strike on blows dealing no damage.
  - **Split on slashing/lightning** — the D&D-canonical trigger, translated to Minecraft as "struck
    by a sword or an axe, or by lightning" — Minecraft has no slashing/blunt damage typing, so the
    trigger reads the attacker's mainhand item against `ItemTags.SWORDS`/`ItemTags.AXES` rather than a
    damage category. Unlike Ochre Jelly, which splits on *any* melee hit, this is narrow on purpose:
    a pickaxe, a shovel, a bare fist, arrows, or fire all kill it without splitting, and that
    asymmetry is the mechanic's entire counterplay.
- Split children are bounded and strictly weaker in kind, not just in numbers: each pudding has its
  own split budget, `maxSplits` (default 1, saved as `SplitsRemaining` so a chunk reload can't refill
  it), `maxGeneration` (default 3) caps the chain, each generation halves max health, and **only a
  generation-0 adult can lash** — so only the adult has the extended reach, and the strike itself is the tell for which one
  out-reaches you. Without that, a corridor of puddings would be an unbroken wall of long-reach
  attackers with no gap to retreat through. One adult yields at most `s + s² + … + s^G` descendants
  (`s = maxSplits`, `G = maxGeneration`), which is 3 at the defaults.
- **`TendrilRenderer`** — shared, library-level geometry for "a limb that reaches far beyond its
  body": a tapered, arcing tube generated between two points, with length, taper and curvature all
  continuous parameters. Endpoints are resolved per frame rather than baked, so a caller can either
  fire and forget (Black Pudding's strike) or track a live, moving target (the planned Roper/Reaper
  grapple-and-reel tentacle). Uses the caller's own `VertexConsumer`, so the tube inherits its
  parent's texture, render type, lighting and hurt-flash for free — deliberately not the beacon-beam
  render type, which is full-bright and translucent and would make a wet physical limb glow.
- **`ExtendedReachMeleeAttackGoal`** — a `MeleeAttackGoal` whose reach takes a supplied bonus, so a
  mob can vary it live or return zero. Needed because overriding `Mob#getMeleeAttackRangeSqr` looks
  like the hook but does nothing for melee: in 1.20.1 `MeleeAttackGoal` never calls it, and its own
  `getAttackReachSqr` recomputes the range inline from `mob.getBbWidth()`. Black Pudding uses it; the
  planned Roper/Reaper tentacle will need the same.
- `BlackPuddingModel` — a single body cube at vanilla slime's exact dimensions, so the family keeps
  one silhouette. It needs its own model class purely to host the procedural lash; there is no other
  geometry. Decorative cuboids were tried in three passes (four corner tendrils, then trailing slime
  shapes, then a blob on the leading face) and all were cut after in-game review — the mob is
  deliberately a plain black cube distinguished by what it does, not what it grows. No translucent
  outer layer: a black pudding is opaque.

### Changed
- 🛡️ **Six spells now carry their caster: Harm, Disintegrate, Fire Spout, Firewall Column, Spike
  Growth and Withering Gaze.** Each hurts through the new `source(level, key, this, owner)`. Until
  now **no shield could block any of them**, whichever way the player faced — see the `source`
  entry above. Carrying the entities also gives the kill credit, the retaliation target and the
  "… by X" death message their attacker.

  **Balance note:** these damage types are `when_caused_by_living_non_player`, which only scales
  with difficulty when a living non-player caused the damage — so until now none of them scaled, and
  now they do, as arrows always have. On Hard, Harm's 6 becomes 9. Paralysis and Rock already carried
  their entities and are unchanged. **Bodak's Death Gaze is deliberately not changed** (it builds the
  same entity-less source): making the Bodak's near-lethal stare shield-blockable is a balance call
  about the Bodak, not a fix.

- 🗡️ **`OrcModel` and `OrcShamanModel` no longer use the hidden-cubes hack.** Both registered the
  seven vanilla parts as *real cubes* and hid them with `visible = false`, then placed a held item by
  hand-tuning the hidden `rightArm` to `(-6.5, 4.0)` so the inherited `translateToHand` happened to
  land near the fist. The vanilla names are now registered with **no cubes** and `translateToHand`
  walks the real arm chain (`orcBody > orc*Arm > orc*LowerArm`), which is the same pattern
  `MinotaurModel` uses.

  The hack drifted with the swing, which is exactly what the model's original comment complained
  about ("can't get the held item to rotate/swing properly with the arm") — a single rotation about
  the hidden arm's pivot cannot reproduce a two-bone arm with an elbow bend. Measured distance from
  the item origin to the forearm's fist point: **0.64 units at rest, 3.03 at ±60° of swing**; the chain
  version is 0.00 at every angle. Right-hand placement at rest is unchanged to within 0.1 units, so
  nothing visibly moves for an existing orc. The hidden cubes also overlapped real orc UV regions
  (`texOffs(0,0)` is the torso), harmless only because they never rendered.

- ⚓ **Grave Zombie, Animated Armor and Wood Golem now share the one anchor implementation.** All
  three had grown their own copy of the same `checkDespawn` override, the same `isWithinRestriction`
  exemption and the same `HomePosX/Y/Z` + `HomeRadius` save-data block. Behaviour is unchanged and
  the tag names are kept verbatim, so guardians already standing in an existing world stay anchored;
  each mob now only states the part that was ever specific to it — the grave zombie's anchor holds
  until it rises (its active *phase*, not merely having a target) and the armour's until it
  activates, both now expressed as one-line `isAnchorSuspended()` overrides.

- **Orc and Orc Shaman skin retextured.** The skin was a cool steel blue (`#37627A`) painted as a
  smooth gradient — the orc's body panel alone carried 164 unique colours, its head another 136,
  which reads as an airbrushed model rather than a Minecraft mob. Skin is now quantised to four flat
  shades chosen from a fixed lookup table, with a low-amplitude deterministic speckle so large flat
  panels do not read as vinyl. Orc is green (`#2A3822` / `#3B4E2C` / `#4C6438` / `#5C7744`); Orc
  Shaman is ashen grey (`#2E332E` / `#414841` / `#555D55` / `#677067`). Clothing, belt, staff, eyes
  and tusks are untouched — the recolour is hue-masked to skin only.
- The two textures needed different luminance band cut-offs (orc 58/76/90, shaman 150/163/172)
  because the shaman's source blue was much lighter, but both map onto the same four palette values
  so the two mobs read as one species. The shaman is therefore noticeably darker-skinned than it was.

Originals and the generator live in `art/` (`art/reskin_orc.py`, `art/textures/entity/`), outside
`src/main/resources` so none of it ships in the jar. A third unused variant (`orc_jade.png`) is kept
there for a future orc variant.

### Fixed
- 🗡️ **Orc / Orc Shaman offhand items rendered in the wrong place.** Only the *right* hidden
  vanilla arm was ever hand-tuned; `left_arm` kept vanilla's `(5.0, 2.0)`, so an item in an Orc's
  offhand drew **1.5 units across and 2 up** from the actual left fist. Latent because the Orc equips
  an empty offhand by default — it would have appeared the moment a consumer gave one a shield.
  Fixed by the `translateToHand` refactor above, which derives both hands from the real geometry.
- 🧟 **Grave Zombie** — a dormant zombie came back from a chunk unload/reload **visible**, showing as
  a zombie head stuck in the floor rather than as a hidden grave. Being burrowed is two things — AI
  off and invisible — and only the first survived a save: `Mob` persists `NoAi`, but nothing at all
  persists the `Invisible` shared-flag bit (`Entity#saveWithoutId` writes `Glowing`, `Silent`,
  `NoGravity` and `Invulnerable`, and vanilla otherwise only ever gets invisibility from a potion
  effect, which is persisted in its own right). The head showed because `GraveZombieModel` sinks the
  rig 26 model units and deliberately stops short of burying it, on the assumption that a fully
  buried zombie is invisible anyway. `readAdditionalSaveData` now restores the flag alongside the
  phase it already restored, so the state can no longer come back half-applied. Nothing else changes:
  a zombie caught mid-reburrow still finishes sinking and sets the flag itself.
- 🧟 **Grave Zombie** — the buried pose now sinks the rig *fully* under the ground plane
  (`SINK_DEPTH` 26 → 34 model units). The humanoid rig spans model y -8 to 24, so 32 is the exact
  depth that drops the crown of the head to ground level and the rest is margin for the hat and armor
  layers. The old 26 was chosen deliberately, on the reasoning that a buried zombie is invisible and
  the sink only had to read as "emerging" mid-transition — so the invisibility flag was the only
  thing hiding the last ~5 units. It isn't any more. Visible effect while surfacing: the zombie now
  climbs half a block further out of the ground over the same telegraph.

- **Shrieker and Violet Fungus can no longer be pushed out of place.** Both are rooted plants, but `setNoAi(true)` and `MOVEMENT_SPEED 0` only stop them moving *themselves* — neither touches physics, so entity collision (`EntitySelector.pushableBy` → `Entity.isPushable`) let a player walking into one slide it across the floor. Both now override `isPushable()` to `false`, as vanilla's `ArmorStand` does for the same reason. Deliberately code rather than datapack: a pushable plant is a bug in any pack.
- Shrieker and Violet Fungus now ship `KNOCKBACK_RESISTANCE 1.0`, so a *hit* no longer slides them either — the same visual bug arriving by the other route, and the likelier one since they are monsters and get attacked. Unlike `isPushable` this stays a knob: `GMMMonster#applyConfigAttributes` overwrites it from a `mob_config`'s `knockbackResistance` when a pack sets one.

Both were found from Dungeons2, which grows these two as floor decoration and so is the first
consumer to have a lot of them standing about in one place. Dungeon Denizens has the same mobs and
had the same behaviour.

## [1.0.1] - 2026- 07-17

### Added
- mod logo

### Fixed
- Shield-holding mobs (Skeleton Warrior, Skeleton Champion, Wight) no longer block and attack
  simultaneously. `RaiseShieldGoal` now lowers the shield for the duration of a mainhand attack
  swing and applies the existing block cooldown before the shield can be raised again.

## [1.0.0] - 2026- 07-17

gottsch's Monster Manual (GMM) is a Minecraft Forge library mod for the 1.20.1 "Dungeon Denizens"
family of mods. It provides shared, dependency-free infrastructure for building D&D-flavored
monsters — base entity classes, AI goals, an ownership/thrall system, a spell/projectile framework,
config-driven tuning (`gmm:mob_config`), and shared sounds/particles/tags — plus a growing roster of
ready-to-use monsters (oozes, zombies, undead, beholder-kin, orcs, and more) that consumer mods
register and skin. GMM itself registers no `EntityType`s or items; it's infrastructure other mods
build on top of.
