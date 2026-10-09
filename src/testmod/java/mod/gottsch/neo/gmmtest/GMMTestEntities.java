package mod.gottsch.neo.gmmtest;

import mod.gottsch.neo.gmm.core.entity.monster.AlligatorGar;
import mod.gottsch.neo.gmm.core.entity.monster.BlackPudding;
import mod.gottsch.neo.gmm.core.entity.monster.Boulder;
import mod.gottsch.neo.gmm.core.entity.monster.Daemon;
import mod.gottsch.neo.gmm.core.entity.monster.GelatinousCube;
import mod.gottsch.neo.gmm.core.entity.monster.GrayOoze;
import mod.gottsch.neo.gmm.core.entity.monster.Headless;
import mod.gottsch.neo.gmm.core.entity.monster.Minotaur;
import mod.gottsch.neo.gmm.core.entity.monster.OchreJelly;
import mod.gottsch.neo.gmm.core.entity.monster.Orc;
import mod.gottsch.neo.gmm.core.entity.monster.OrcShaman;
import mod.gottsch.neo.gmm.core.entity.monster.OrcWarlord;
import mod.gottsch.neo.gmm.core.entity.monster.Rat;
import mod.gottsch.neo.gmm.core.entity.monster.Shadow;
import mod.gottsch.neo.gmm.core.entity.monster.Shadowlord;
import mod.gottsch.neo.gmm.core.entity.monster.StoneColossus;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.Beholder;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.DeathTyrant;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.Gazer;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.Spectator;
import mod.gottsch.neo.gmm.core.entity.monster.construct.AnimatedArmor;
import mod.gottsch.neo.gmm.core.entity.monster.construct.AnimatedWeapon;
import mod.gottsch.neo.gmm.core.entity.monster.construct.WoodGolem;
import mod.gottsch.neo.gmm.core.entity.monster.gargoyle.Gargoyle;
import mod.gottsch.neo.gmm.core.entity.monster.gargoyle.Margoyle;
import mod.gottsch.neo.gmm.core.entity.monster.ghoul.Ghoul;
import mod.gottsch.neo.gmm.core.entity.monster.ghoul.SewerGhoul;
import mod.gottsch.neo.gmm.core.entity.monster.mimic.BarrelMimic;
import mod.gottsch.neo.gmm.core.entity.monster.mimic.VanillaChestMimic;
import mod.gottsch.neo.gmm.core.entity.monster.plant.Shrieker;
import mod.gottsch.neo.gmm.core.entity.monster.plant.VioletFungus;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.AcidSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.BloodyBones;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.BurningSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.ElectricSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.FrostSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.IronSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.MagmaSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.SkeletonChampion;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.SkeletonWarrior;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.TaintedSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.WingedSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Bloater;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Bodak;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.GraveZombie;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Wight;
import mod.gottsch.neo.gmm.core.entity.projectile.BloaterArm;
import mod.gottsch.neo.gmm.core.entity.projectile.BoneShard;
import mod.gottsch.neo.gmm.core.entity.projectile.DisarmSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.DisintegrateSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.FireSpoutSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.FirewallColumnSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.HarmSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.ParalysisSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.Rock;
import mod.gottsch.neo.gmm.core.entity.projectile.SpikeGrowthSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.WitheringGazeSpell;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One {@link EntityType} per concrete gmm mob and projectile, under the {@code gmmtest} namespace,
 * named as Dungeon Denizens names them so DD's {@code gmm:mob_config} JSON can be copied across as-is.
 * Sizes are DD's, except Stone Colossus (DD has none), which is drawn at its rig's own size.
 * <p>
 * {@link #MOBS} drives attributes, spawn eggs, tags and the start-up checks from one list, so a mob
 * added here cannot be half-registered.
 */
public final class GMMTestEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GMMTest.MOD_ID);

    public record MobEntry<T extends Mob>(String name, DeferredHolder<EntityType<?>, EntityType<T>> type, Class<T> entityClass,
                                          Supplier<AttributeSupplier.Builder> attributes, int eggBase, int eggSpots) { }

    public static final List<MobEntry<?>> MOBS = new ArrayList<>();
    public static final List<DeferredHolder<EntityType<?>, ? extends EntityType<?>>> PROJECTILES = new ArrayList<>();

    // --- mobs ---------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<Headless>> HEADLESS = mob("headless", Headless::new, Headless.class, 0.65F, 1.5F, false, Headless::createAttributes, 0xc8b486, 0x6f5e48);
    public static final DeferredHolder<EntityType<?>, EntityType<Orc>> ORC = mob("orc", Orc::new, Orc.class, 1F, 1.99F, false, Orc::createAttributes, 0x6b8e4e, 0x3f5c2c);
    public static final DeferredHolder<EntityType<?>, EntityType<OrcShaman>> ORC_SHAMAN = mob("orc_shaman", OrcShaman::new, OrcShaman.class, 1F, 1.99F, false, OrcShaman::createAttributes, 0x6b8e4e, 0x4b2e83);
    public static final DeferredHolder<EntityType<?>, EntityType<OrcWarlord>> ORC_WARLORD = mob("orc_warlord", OrcWarlord::new, OrcWarlord.class, 1F, 1.99F, false, OrcWarlord::createAttributes, 0x5d6b4a, 0x6b1f22);
    public static final DeferredHolder<EntityType<?>, EntityType<Ghoul>> GHOUL = mob("ghoul", Ghoul::new, Ghoul.class, 0.6F, 1.68F, false, Ghoul::createAttributes, 0x9fc2b8, 0x4f5b56);
    public static final DeferredHolder<EntityType<?>, EntityType<SewerGhoul>> SEWER_GHOUL = mob("sewer_ghoul", SewerGhoul::new, SewerGhoul.class, 0.6F, 1.68F, false, SewerGhoul::createAttributes, 0x5a6650, 0x3d4636);
    public static final DeferredHolder<EntityType<?>, EntityType<Rat>> RAT = mob("rat", Rat::new, Rat.class, 0.8F, 0.25F, false, Rat::createAttributes, 0x7a5c3e, 0x4a3521);
    public static final DeferredHolder<EntityType<?>, EntityType<AlligatorGar>> ALLIGATOR_GAR = mob("alligator_gar", AlligatorGar::new, AlligatorGar.class, 0.6F, 0.4F, false, AlligatorGar::createAttributes, 0x4a5c3a, 0x8f9f6b);
    public static final DeferredHolder<EntityType<?>, EntityType<Beholder>> BEHOLDER = mob("beholder", Beholder::new, Beholder.class, 2.25F, 3.5F, true, Beholder::prepareAttributes, 0x871e00, 0xc15227);
    public static final DeferredHolder<EntityType<?>, EntityType<DeathTyrant>> DEATH_TYRANT = mob("death_tyrant", DeathTyrant::new, DeathTyrant.class, 2.25F, 3.5F, true, DeathTyrant::prepareAttributes, 0x86765a, 0xcdc3bb);
    public static final DeferredHolder<EntityType<?>, EntityType<Gazer>> GAZER = mob("gazer", Gazer::new, Gazer.class, 1.125F, 1.25F, true, Gazer::prepareAttributes, 0x7a2e2f, 0xd4af37);
    public static final DeferredHolder<EntityType<?>, EntityType<Spectator>> SPECTATOR = mob("spectator", Spectator::new, Spectator.class, 0.84375F, 1.21875F, false, Spectator::prepareAttributes, 0x344133, 0xabb685);
    public static final DeferredHolder<EntityType<?>, EntityType<Boulder>> BOULDER = mob("boulder", Boulder::new, Boulder.class, 1F, 1F, true, Boulder::createAttributes, 0x5c5c5c, 0x8f8f8f);
    public static final DeferredHolder<EntityType<?>, EntityType<Shadow>> SHADOW = mob("shadow", Shadow::new, Shadow.class, 0.75F, 1.95F, false, Shadow::createAttributes, 0x000000, 0x2b2b2b);
    public static final DeferredHolder<EntityType<?>, EntityType<Shadowlord>> SHADOWLORD = mob("shadowlord", Shadowlord::new, Shadowlord.class, 1.25F, 2.8125F, false, Shadowlord::createAttributes, 0x000000, 0x6c6c6c);
    public static final DeferredHolder<EntityType<?>, EntityType<Daemon>> DAEMON = mob("daemon", Daemon::new, Daemon.class, 1F, 3F, true, Daemon::createAttributes, 0xff0000, 0xff8c00);
    public static final DeferredHolder<EntityType<?>, EntityType<SkeletonWarrior>> SKELETON_WARRIOR = mob("skeleton_warrior", SkeletonWarrior::new, SkeletonWarrior.class, 0.6F, 1.95F, false, SkeletonWarrior::createAttributes, 0xf5f6d2, 0xcdc3bb);
    public static final DeferredHolder<EntityType<?>, EntityType<WingedSkeleton>> WINGED_SKELETON = mob("winged_skeleton", WingedSkeleton::new, WingedSkeleton.class, 0.75F, 1.95F, false, WingedSkeleton::createAttributes, 0xe2ded0, 0x8a8478);
    public static final DeferredHolder<EntityType<?>, EntityType<IronSkeleton>> IRON_SKELETON = mob("iron_skeleton", IronSkeleton::new, IronSkeleton.class, 0.63F, 2.1F, false, IronSkeleton::createAttributes, 0xc8c8d0, 0x6e6e78);
    public static final DeferredHolder<EntityType<?>, EntityType<MagmaSkeleton>> MAGMA_SKELETON = mob("magma_skeleton", MagmaSkeleton::new, MagmaSkeleton.class, 0.63F, 2.1F, true, MagmaSkeleton::createAttributes, 0x4b0000, 0xff7900);
    public static final DeferredHolder<EntityType<?>, EntityType<FrostSkeleton>> FROST_SKELETON = mob("frost_skeleton", FrostSkeleton::new, FrostSkeleton.class, 0.6F, 1.99F, false, FrostSkeleton::createAttributes, 0x3d5878, 0xd6f4ff);
    public static final DeferredHolder<EntityType<?>, EntityType<TaintedSkeleton>> TAINTED_SKELETON = mob("tainted_skeleton", TaintedSkeleton::new, TaintedSkeleton.class, 0.7F, 1.99F, false, TaintedSkeleton::createAttributes, 0xa8a596, 0x8a1420);
    public static final DeferredHolder<EntityType<?>, EntityType<AcidSkeleton>> ACID_SKELETON = mob("acid_skeleton", AcidSkeleton::new, AcidSkeleton.class, 0.6F, 1.99F, false, AcidSkeleton::createAttributes, 0xb2e278, 0x2c4e1c);
    public static final DeferredHolder<EntityType<?>, EntityType<ElectricSkeleton>> ELECTRIC_SKELETON = mob("electric_skeleton", ElectricSkeleton::new, ElectricSkeleton.class, 0.6F, 1.99F, false, ElectricSkeleton::createAttributes, 0x2d3a6e, 0xf5e642);
    public static final DeferredHolder<EntityType<?>, EntityType<BurningSkeleton>> BURNING_SKELETON = mob("burning_skeleton", BurningSkeleton::new, BurningSkeleton.class, 0.6F, 1.99F, true, BurningSkeleton::createAttributes, 0x1c1512, 0xffb02e);
    public static final DeferredHolder<EntityType<?>, EntityType<BloodyBones>> BLOODY_BONES = mob("bloody_bones", BloodyBones::new, BloodyBones.class, 0.6F, 1.99F, false, BloodyBones::createAttributes, 0xe8e0d0, 0x8a1420);
    public static final DeferredHolder<EntityType<?>, EntityType<Bloater>> BLOATER = mob("bloater", Bloater::new, Bloater.class, 0.7F, 2.1F, false, Bloater::createAttributes, 0x6c8a2c, 0x3a4a1e);
    public static final DeferredHolder<EntityType<?>, EntityType<GraveZombie>> GRAVE_ZOMBIE = mob("grave_zombie", GraveZombie::new, GraveZombie.class, 0.6F, 1.95F, false, GraveZombie::createAttributes, 0x8b5a2b, 0x3e2711);
    public static final DeferredHolder<EntityType<?>, EntityType<Wight>> WIGHT = mob("wight", Wight::new, Wight.class, 0.6F, 1.95F, false, Wight::createAttributes, 0xced1d6, 0x2a2c33);
    public static final DeferredHolder<EntityType<?>, EntityType<Bodak>> BODAK = mob("bodak", Bodak::new, Bodak.class, 0.6F, 1.95F, false, Bodak::createAttributes, 0x6b6a63, 0x2b2a26);
    public static final DeferredHolder<EntityType<?>, EntityType<Shrieker>> SHRIEKER = mob("shrieker", Shrieker::new, Shrieker.class, 1.3F, 0.85F, false, Shrieker::createAttributes, 0x8f4f8f, 0xc9a24a);
    public static final DeferredHolder<EntityType<?>, EntityType<VioletFungus>> VIOLET_FUNGUS = mob("violet_fungus", VioletFungus::new, VioletFungus.class, 1.3F, 0.85F, false, VioletFungus::createAttributes, 0x6e2878, 0x231528);
    public static final DeferredHolder<EntityType<?>, EntityType<GelatinousCube>> GELATINOUS_CUBE = mob("gelatinous_cube", GelatinousCube::new, GelatinousCube.class, 1.1F, 1.1F, false, GelatinousCube::createAttributes, 0x9adfc7, 0x5fae95);
    public static final DeferredHolder<EntityType<?>, EntityType<OchreJelly>> OCHRE_JELLY = mob("ochre_jelly", OchreJelly::new, OchreJelly.class, 0.85F, 0.85F, false, OchreJelly::createAttributes, 0xadb83d, 0x5f6b1f);
    public static final DeferredHolder<EntityType<?>, EntityType<GrayOoze>> GRAY_OOZE = mob("gray_ooze", GrayOoze::new, GrayOoze.class, 0.9F, 0.9F, false, GrayOoze::createAttributes, 0x7d8a8f, 0x4a5459);
    public static final DeferredHolder<EntityType<?>, EntityType<BlackPudding>> BLACK_PUDDING = mob("black_pudding", BlackPudding::new, BlackPudding.class, 0.9F, 0.9F, false, BlackPudding::createAttributes, 0x2b1d33, 0x5e456b);
    public static final DeferredHolder<EntityType<?>, EntityType<VanillaChestMimic>> VANILLA_CHEST_MIMIC = mob("vanilla_chest_mimic", VanillaChestMimic::new, VanillaChestMimic.class, 1F, 1F, false, VanillaChestMimic::createAttributes, 0x8f691d, 0x2f3a24);
    public static final DeferredHolder<EntityType<?>, EntityType<BarrelMimic>> BARREL_MIMIC = mob("barrel_mimic", BarrelMimic::new, BarrelMimic.class, 1F, 1F, false, BarrelMimic::createAttributes, 0x9f854d, 0x54452c);
    public static final DeferredHolder<EntityType<?>, EntityType<AnimatedArmor>> ANIMATED_ARMOR = mob("animated_armor", AnimatedArmor::new, AnimatedArmor.class, 0.6F, 1.99F, false, AnimatedArmor::createAttributes, 0xc0c0c8, 0x4a4a52);
    public static final DeferredHolder<EntityType<?>, EntityType<AnimatedWeapon>> ANIMATED_WEAPON = mob("animated_weapon", AnimatedWeapon::new, AnimatedWeapon.class, 0.75F, 0.75F, false, AnimatedWeapon::createAttributes, 0xa8adb5, 0xc9a227);
    public static final DeferredHolder<EntityType<?>, EntityType<WoodGolem>> WOOD_GOLEM = mob("wood_golem", WoodGolem::new, WoodGolem.class, 0.6F, 1.95F, false, WoodGolem::createAttributes, 0xc9a66b, 0x3e2b18);
    public static final DeferredHolder<EntityType<?>, EntityType<SkeletonChampion>> SKELETON_CHAMPION = mob("skeleton_champion", SkeletonChampion::new, SkeletonChampion.class, 0.72F, 2.39F, false, SkeletonChampion::createAttributes, 0xc9c9c9, 0x17171a);
    public static final DeferredHolder<EntityType<?>, EntityType<Minotaur>> MINOTAUR = mob("minotaur", Minotaur::new, Minotaur.class, 1.0F, 2.2F, false, Minotaur::createAttributes, 0x8a4a34, 0xe8e2d2);
    public static final DeferredHolder<EntityType<?>, EntityType<Gargoyle>> GARGOYLE = mob("gargoyle", Gargoyle::new, Gargoyle.class, 0.75F, 1.75F, false, Gargoyle::createAttributes, 0x6d6d81, 0x373b41);
    public static final DeferredHolder<EntityType<?>, EntityType<Margoyle>> MARGOYLE = mob("margoyle", Margoyle::new, Margoyle.class, 0.9F, 2.1F, false, Margoyle::createAttributes, 0x7f7f7f, 0x5a6d41);
    public static final DeferredHolder<EntityType<?>, EntityType<StoneColossus>> STONE_COLOSSUS = mob("stone_colossus", StoneColossus::new, StoneColossus.class, 1.4F, 2.75F, false, StoneColossus::createAttributes, 0x8a8a80, 0x3fbfd0);

    // --- projectiles --------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<ParalysisSpell>> PARALYSIS_SPELL = projectile("slow", EntityType.Builder.of(ParalysisSpell::new, MobCategory.MISC).sized(1F, 1F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<HarmSpell>> HARM_SPELL = projectile("harm", EntityType.Builder.of(HarmSpell::new, MobCategory.MISC).sized(1F, 1F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<WitheringGazeSpell>> WITHERING_GAZE_SPELL = projectile("withering_gaze", EntityType.Builder.of(WitheringGazeSpell::new, MobCategory.MISC).sized(1F, 1F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<DisarmSpell>> DISARM_SPELL = projectile("disarm", EntityType.Builder.of(DisarmSpell::new, MobCategory.MISC).sized(1F, 1F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<DisintegrateSpell>> DISINTEGRATE_SPELL = projectile("disintegrate", EntityType.Builder.of(DisintegrateSpell::new, MobCategory.MISC).sized(1F, 1F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<FireSpoutSpell>> FIRESPOUT_SPELL = projectile("firespout", EntityType.Builder.of(FireSpoutSpell::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<FirewallColumnSpell>> FIREWALL_COLUMN_SPELL = projectile("firewall_column", EntityType.Builder.of(FirewallColumnSpell::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<SpikeGrowthSpell>> SPIKE_GROWTH_SPELL = projectile("spike_growth", EntityType.Builder.of(SpikeGrowthSpell::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<Rock>> ROCK = projectile("rock", EntityType.Builder.of(Rock::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<BoneShard>> BONE_SHARD = projectile("bone_shard", EntityType.Builder.<BoneShard>of(BoneShard::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20));
    public static final DeferredHolder<EntityType<?>, EntityType<BloaterArm>> BLOATER_ARM = projectile("bloater_arm", EntityType.Builder.<BloaterArm>of(BloaterArm::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20));

    private GMMTestEntities() { }

    private static <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> mob(String name, EntityType.EntityFactory<T> factory, Class<T> entityClass,
                                                                                    float width, float height, boolean fireImmune,
                                                                                    Supplier<AttributeSupplier.Builder> attributes, int eggBase, int eggSpots) {
        DeferredHolder<EntityType<?>, EntityType<T>> holder = ENTITIES.register(name, () -> {
            EntityType.Builder<T> builder = EntityType.Builder.of(factory, name.equals("boulder") ? MobCategory.AMBIENT : MobCategory.MONSTER)
                    .sized(width, height)
                    .clientTrackingRange(12);
            if (fireImmune) {
                builder.fireImmune();
            }
            return builder.build(name);
        });
        MOBS.add(new MobEntry<>(name, holder, entityClass, attributes, eggBase, eggSpots));
        return holder;
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> projectile(String name, EntityType.Builder<T> builder) {
        DeferredHolder<EntityType<?>, EntityType<T>> holder = ENTITIES.register(name, () -> builder.build(name));
        PROJECTILES.add(holder);
        return holder;
    }
}
