package mod.gottsch.neo.gmm.core.tag;

import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * @author by Mark Gottschling on 11/6/2025
 */
public class GMMTags {
    public static class Items {

        public static final TagKey<Item> GHOUL_FOOD = mod(GMM.MOD_ID, "ghoul/food");

        // SkeletonWarrior equipment pools (consumer-populated, one per slot)
        public static final TagKey<Item> SKELETON_WARRIOR_WEAPONS = mod(GMM.MOD_ID, "skeleton_warrior/weapons");
        public static final TagKey<Item> SKELETON_WARRIOR_HELMETS = mod(GMM.MOD_ID, "skeleton_warrior/helmets");
        public static final TagKey<Item> SKELETON_WARRIOR_CHESTPLATES = mod(GMM.MOD_ID, "skeleton_warrior/chestplates");
        public static final TagKey<Item> SKELETON_WARRIOR_LEGGINGS = mod(GMM.MOD_ID, "skeleton_warrior/leggings");
        public static final TagKey<Item> SKELETON_WARRIOR_BOOTS = mod(GMM.MOD_ID, "skeleton_warrior/boots");
        // Shield pool (see RaiseShieldGoal) -- SkeletonWarrior rolls into this at a configurable chance
        // (shieldProbability), unlike weapons/armor above which are always rolled.
        public static final TagKey<Item> SKELETON_WARRIOR_SHIELDS = mod(GMM.MOD_ID, "skeleton_warrior/shields");

        // Orc weapon pool (consumer-populated)
        public static final TagKey<Item> ORC_WEAPONS = mod(GMM.MOD_ID, "orc/weapons");

        // Minotaur weapon pool. Unlike the Orc pools there is no fallback: an empty tag leaves the
        // Minotaur bare-fisted, which reads correctly for it. gmm ships heavy two-handers as the
        // default; consumers add to it additively.
        public static final TagKey<Item> MINOTAUR_WEAPONS = mod(GMM.MOD_ID, "minotaur/weapons");

        // The war-chief's own blade, separate from the warband's so a pack carrying stone axes does
        // not arm its leader with one. gmm ships iron/diamond/netherite axes as the default;
        // consumers add to it additively. An emptied pool falls back to ORC_WEAPONS rather than to bare fists --
        // see OrcWarlord#populateDefaultEquipmentSlots.
        public static final TagKey<Item> ORC_WARLORD_WEAPONS = mod(GMM.MOD_ID, "orc_warlord/weapons");

        // Skeleton Champion weapon pool: the elite leader picks its main-hand weapon from this on spawn.
        // gmm ships diamond + netherite swords as the default (an elite carries elite steel); consumers
        // add their own weapons additively.
        public static final TagKey<Item> SKELETON_CHAMPION_WEAPONS = mod(GMM.MOD_ID, "skeleton_champion/weapons");
        // Shield pool (see RaiseShieldGoal) -- unlike SkeletonWarrior/Wight, a Champion always rolls
        // one (no probability gate); it's the pack leader, not a rank-and-file grunt.
        public static final TagKey<Item> SKELETON_CHAMPION_SHIELDS = mod(GMM.MOD_ID, "skeleton_champion/shields");

        // Equipment immune to acid/corrosion damage (Acid Skeleton, Gelatinous Cube, ...). gmm ships
        // diamond/netherite gear as the default; consumers can add their own corrosion-proof items
        // additively. Shared across mobs rather than mob-scoped since it's a material property, not a
        // per-mob equipment pool.
        public static final TagKey<Item> CORROSION_IMMUNE = mod(GMM.MOD_ID, "corrosion_immune");

        // Wight weapon pool: melee (sword) or ranged (bow) -- see Wight#reassessWeaponGoal. gmm ships
        // a modest vanilla default of each so a Wight is armed standalone; consumers add more.
        public static final TagKey<Item> WIGHT_WEAPONS = mod(GMM.MOD_ID, "wight/weapons");
        // Shield pool (see RaiseShieldGoal) -- a melee-rolled Wight only, at a configurable chance
        // (shieldProbability); a bow-rolled Wight never gets one (see Wight#populateDefaultEquipmentSlots).
        public static final TagKey<Item> WIGHT_SHIELDS = mod(GMM.MOD_ID, "wight/shields");

        // Bodak gaze ward: a helmet-slot item that blocks Death Gaze entirely (see
        // Bodak#isLookingAtMe), mirroring vanilla Enderman's own carved-pumpkin ward. gmm ships carved
        // pumpkin as the default so a Bodak is counterable standalone with zero new items; consumers
        // can add their own dedicated ward item (blindfold/goggles/etc.) additively.
        public static final TagKey<Item> BODAK_GAZE_WARD = mod(GMM.MOD_ID, "bodak/gaze_ward");

        // Shadow spawn weapon pool (consumer-populated)
        public static final TagKey<Item> SHADOW_WEAPONS = mod(GMM.MOD_ID, "shadow/weapons");
        // Shadow-bane weapons: bypass the Shadow's metal resistance and deal bonus damage.
        // BANE = +2 (gold-tier bane), MINOR_BANE = +1. Gold swords are handled intrinsically.
        public static final TagKey<Item> SHADOW_BANE = mod(GMM.MOD_ID, "shadow/bane");
        public static final TagKey<Item> SHADOW_MINOR_BANE = mod(GMM.MOD_ID, "shadow/minor_bane");

        // Animated Armor equipment pools (consumer-populated, one per slot) -- a full suit rolls all
        // four independently, see AnimatedArmor#populateDefaultEquipmentSlots.
        public static final TagKey<Item> ANIMATED_ARMOR_HELMETS = mod(GMM.MOD_ID, "animated_armor/helmets");
        public static final TagKey<Item> ANIMATED_ARMOR_CHESTPLATES = mod(GMM.MOD_ID, "animated_armor/chestplates");
        public static final TagKey<Item> ANIMATED_ARMOR_LEGGINGS = mod(GMM.MOD_ID, "animated_armor/leggings");
        public static final TagKey<Item> ANIMATED_ARMOR_BOOTS = mod(GMM.MOD_ID, "animated_armor/boots");

        // Animated Weapon mainhand pool (consumer-populated) -- rolled once on spawn and always dropped.
        public static final TagKey<Item> ANIMATED_WEAPON_WEAPONS = mod(GMM.MOD_ID, "animated_weapon/weapons");

        public static TagKey<Item> mod(String domain, String path) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(domain, path));
        }
    }

    /**
     * Entity-type tags used by gmm's reusable AI goals. gmm owns the tag KEYS but
     * deliberately leaves them empty; CONSUMER mods populate them (e.g. a mod adds
     * its own mobs to HEADLESS_HURT_ALLIES via data/gmm/tags/entity_types/headless/hurt_allies.json)
     * so the shared goals never need to reference consumer mob classes.
     */
    public static class EntityTypes {

        // mobs a Headless alerts when it is hurt
        public static final TagKey<EntityType<?>> HEADLESS_HURT_ALLIES = mod(GMM.MOD_ID, "headless/hurt_allies");
        // mobs a Headless alerts when it acquires a target
        public static final TagKey<EntityType<?>> HEADLESS_TARGET_ALLIES = mod(GMM.MOD_ID, "headless/target_allies");

        // mobs a Skeleton Champion rallies (buffs) while alive — consumers add their skeleton types
        public static final TagKey<EntityType<?>> SKELETON_CHAMPION_RALLY_ALLIES = mod(GMM.MOD_ID, "skeleton_champion/rally_allies");

        // mobs an Orc Warlord rallies (buffs) while alive. Its own key rather than sharing the
        // Champion's: the two lead different things, and a shared tag would have a skeleton
        // champion buffing orcs the moment a consumer populated it for either one.
        public static final TagKey<EntityType<?>> ORC_WARLORD_RALLY_ALLIES = mod(GMM.MOD_ID, "orc_warlord/rally_allies");

        // mobs a Beholder can Enthrall (EnthrallGoal). Scoped per-caster rather than shared: a
        // Beholder's "high IQ, psychic powers" flavor lets it dominate stronger mobs than, say, a
        // Shadowlord would be able to (which would get its own gmm:shadowlord/enthrall_candidates tag
        // once/if it ever wires EnthrallGoal). gmm ships a default of common overworld/dungeon
        // hostiles; consumers add their own mobs additively.
        public static final TagKey<EntityType<?>> BEHOLDER_ENTHRALL_CANDIDATES = mod(GMM.MOD_ID, "beholder/enthrall_candidates");

        // A Wight that lands a killing blow may raise a fresh thrall instead of leaving a plain kill
        // (see Wight#tryRaiseThrall) via one of two mechanisms, each its own tag so a consumer can tune
        // them independently: SUMMON_ALLIES spawns a brand-new mob (then stamps it a permanent thrall
        // the same way Ownership#enthrall does); ENTHRALL_CANDIDATES instead dominates an existing
        // nearby live mob. gmm ships a zombie default for both; consumers add their own zombie-family
        // mobs additively.
        public static final TagKey<EntityType<?>> WIGHT_SUMMON_ALLIES = mod(GMM.MOD_ID, "wight/summon_allies");
        public static final TagKey<EntityType<?>> WIGHT_ENTHRALL_CANDIDATES = mod(GMM.MOD_ID, "wight/enthrall_candidates");

        // mobs that may spawn as a Wight's escort (see GMMMonster#getCompanionPool). Distinct from
        // the two tags above (which are combat-time thrall-raising pools, not spawn-time escorts) --
        // a lone Wight is an easy-to-kite caster, so a small guard at spawn makes the encounter read
        // like an actual undead leader rather than a solitary target.
        public static final TagKey<EntityType<?>> WIGHT_COMPANIONS = mod(GMM.MOD_ID, "wight/companions");

        // mobs a Shrieker alerts (via AllyAlertUtil) when it pulses on a nearby player -- Shrieker never
        // fights, so this is a proximity broadcast, not a combat-acquired-target one.
        public static final TagKey<EntityType<?>> SHRIEKER_ALLIES = mod(GMM.MOD_ID, "shrieker/allies");

        // mobs the Burning Skeleton's flame (aura pulse, melee bite, death burst) won't ignite --
        // MobType/isAlliedTo alone don't cover "don't burn fellow undead standing next to you", since
        // independently-spawned hostile mobs are never on a scoreboard team by default. Explicit tag
        // instead of a getMobType() comparison so consumers control exactly who's ignite-immune rather
        // than gmm silently exempting every MobType.UNDEAD mob in existence.
        public static final TagKey<EntityType<?>> BURNING_SKELETON_IGNITE_IMMUNE = mod(GMM.MOD_ID, "burning_skeleton/ignite_immune");

        // Mobs that may spawn as a Beholder's escort (see GMMMonster#getCompanionPool /
        // GMMMonster#finalizeSpawn). Scoped per-mob rather than shared, same reasoning as every other
        // tag in this class: a future companion-spawner with a different theme gets its own
        // gmm:<mob>/companions tag rather than sharing one pool. gmm ships no default (there's no
        // sensible vanilla stand-in for a Beholder-kin minion); consumers populate it entirely.
        public static final TagKey<EntityType<?>> BEHOLDER_COMPANIONS = mod(GMM.MOD_ID, "beholder/companions");

        // --- category tags -----------------------------------------------------------------------
        // Cross-cutting, not scoped to one mob -- a `gmm:category/*` tag every consumer of "who counts
        // as X" logic can share, instead of every mob re-enumerating its own roster (the every-tag-is-
        // per-mob pattern above works well for a bespoke pool like "who a Beholder enthralls," but
        // breaks down for something as broad as "every hostile monster," which nobody wants to list out
        // per mob). First consumer: Wood Golem's "protector" targeting (see WoodGolem#registerGoals) --
        // a golem defends its post against whatever's in this tag without the mapmaker/consumer having
        // to enumerate a monster roster themselves. gmm ships the same "common overworld/dungeon
        // hostiles" default as BEHOLDER_ENTHRALL_CANDIDATES; consumers add their own additively.
        public static final TagKey<EntityType<?>> CATEGORY_HOSTILE_MONSTERS = mod(GMM.MOD_ID, "category/hostile_monsters");

        public static TagKey<EntityType<?>> mod(String domain, String path) {
            return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(domain, path));
        }
    }

    /**
     * Damage-type tags. gmm owns the KEYS; the JSON is optional — a missing tag simply matches
     * nothing (so no datagen provider is required to ship a sensible default).
     */
    public static class DamageTypes {

        // Damage types that "kill the bones right" — a true, un-resurrectable death for Bloody Bones.
        // Fire/lava is handled intrinsically in code; drop entries here (e.g. a pack's holy/smite
        // damage type) to add more with no code change.
        public static final TagKey<net.minecraft.world.damagesource.DamageType> BLOODY_BONES_TRUE_KILL =
                mod(GMM.MOD_ID, "bloody_bones/true_kill");

        public static TagKey<net.minecraft.world.damagesource.DamageType> mod(String domain, String path) {
            return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(domain, path));
        }
    }
}
