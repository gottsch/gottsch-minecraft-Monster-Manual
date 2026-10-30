# Changelog for gottsch's Monster Manual (Forge 1.20.1)

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-10-30

GMM is a library mod, so new mobs and features appear in game through the mods that use it, such as Dungeon Denizens.

### New Monsters
- **Leech and Giant Leech:** Water ambushers that attach to you and stop all healing until they're gone. Burn them off, rip them off for some damage, or have another player pull them off.
- **Ember Hound:** A hound made of embers that sets targets alight and burns anyone who hits it in melee. Fire and lava heal it, while water douses its flames and slows it down.
- **Stone Colossus:** A towering construct that stands like a statue until you get close, then attacks with ground slams. Arrows barely hurt it, but pickaxes hit hard.
- **Minotaur:** A heavy brute that winds up and charges at you. Sidestep the charge, as it won't change course once it starts running.
- **Orc Warlord:** An orc chief who calls in reinforcements, buffs nearby allies and alerts them when he spots you. He also tries to stay at the edge of the fight.
- **Black Pudding:** The fastest and deadliest ooze, with a long-reach lash. Swords, axes and lightning split it, but other attacks don't.

### Changes
- **Orc doors:** Orcs can now break wooden doors to reach you, if the mod you're playing with turns this on. Iron doors still stop them.
- **Spell blocking:** Shields can now block Harm, Disintegrate, Fire Spout, Firewall Column, Spike Growth and Withering Gaze.
- **Spell damage:** Those six spells now scale with difficulty, so they hit harder on Hard.
- **Orc textures:** Orcs have new green skin, and Orc Shamans have ashen grey skin.

### Fixes
- Orcs now hold items properly in their hands, including while swinging.
- Buried Grave Zombies no longer show a head sticking out of the ground after a chunk reloads.
- Shriekers and Violet Fungi can no longer be pushed or knocked out of place.

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
