package mod.gottsch.neo.gmmtest;

import mod.gottsch.neo.gmm.core.config.GMMRegistries;
import mod.gottsch.neo.gmm.core.entity.ai.goal.CastSpellGoal;
import mod.gottsch.neo.gmm.core.entity.monster.Daemon;
import mod.gottsch.neo.gmm.core.entity.monster.IGMMMonster;
import mod.gottsch.neo.gmm.core.entity.monster.Orc;
import mod.gottsch.neo.gmm.core.entity.monster.OrcShaman;
import mod.gottsch.neo.gmm.core.entity.monster.OrcWarlord;
import mod.gottsch.neo.gmm.core.entity.monster.Shadowlord;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.Beholder;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.DeathTyrant;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.Gazer;
import mod.gottsch.neo.gmm.core.entity.monster.beholderkin.Spectator;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.BloodyBones;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.TaintedSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Bloater;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Bodak;
import mod.gottsch.neo.gmm.core.entity.projectile.BloaterArm;
import mod.gottsch.neo.gmm.core.entity.projectile.BoneShard;
import mod.gottsch.neo.gmm.core.entity.projectile.DisarmSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.DisintegrateSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.FireSpoutSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.GMMHurtingProjectile;
import mod.gottsch.neo.gmm.core.entity.projectile.HarmSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.ParalysisSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.Rock;
import mod.gottsch.neo.gmm.core.entity.projectile.SpikeGrowthSpell;
import mod.gottsch.neo.gmm.core.entity.projectile.WitheringGazeSpell;
import mod.gottsch.neo.gmm.core.tag.GMMVanillaTags;
import mod.gottsch.neo.gottschcore.random.WeightedCollection;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * gmm's reference consumer. gmm registers no content, so this dev-only mod (never packaged into the
 * gmm jar) registers every gmm mob and spell the way a consuming mod would: entity types, attributes,
 * spawn eggs, renderers ({@link GMMTestClient}), the static launcher/factory hooks, vanilla category
 * tags and {@code gmm:mob_config} entries.
 * <p>
 * The wiring in {@link #commonSetup} is Dungeon Denizens' CommonSetup, which is what players met on
 * Forge. Two checks run on every launch so a gap shows up in the log rather than in play: every type
 * must have a renderer (client), and every mob's {@code isUndead()} must agree with
 * {@code #minecraft:undead} (server start).
 */
@Mod(GMMTest.MOD_ID)
public class GMMTest {
    public static final String MOD_ID = "gmmtest";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    /** Spawn eggs, keyed by mob name, in {@link GMMTestEntities#MOBS} order. */
    public static final Map<String, DeferredItem<DeferredSpawnEggItem>> EGGS = new LinkedHashMap<>();

    // in-flight visuals for the spell projectiles; textures ship in gmm
    public static final DeferredItem<Item> PARALYSIS_SPELL_ITEM = ITEMS.registerSimpleItem("paralysis_spell");
    public static final DeferredItem<Item> HARM_SPELL_ITEM = ITEMS.registerSimpleItem("harm_spell");
    public static final DeferredItem<Item> DISINTEGRATE_SPELL_ITEM = ITEMS.registerSimpleItem("disintegrate_spell");
    public static final DeferredItem<Item> DISARM_SPELL_ITEM = ITEMS.registerSimpleItem("disarm_spell");
    public static final DeferredItem<Item> WITHERING_GAZE_SPELL_ITEM = ITEMS.registerSimpleItem("withering_gaze_spell");
    public static final DeferredItem<Item> ROCK_ITEM = ITEMS.registerSimpleItem("rock");
    public static final DeferredItem<SwordItem> SHADOW_BLADE = ITEMS.register("shadow_blade",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2F))));

    static {
        for (GMMTestEntities.MobEntry<?> mob : GMMTestEntities.MOBS) {
            EGGS.put(mob.name(), ITEMS.register(mob.name() + "_egg",
                    () -> new DeferredSpawnEggItem(mob.type(), mob.eggBase(), mob.eggSpots(), new Item.Properties())));
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("gmmtest", () -> CreativeModeTab.builder()
            .title(Component.literal("gmm test"))
            .icon(() -> new ItemStack(EGGS.get("orc").get()))
            .displayItems((params, output) -> {
                EGGS.values().forEach(egg -> output.accept(egg.get()));
                output.accept(SHADOW_BLADE.get());
            })
            .build());

    public GMMTest(IEventBus modEventBus, ModContainer container) {
        GMMTestEntities.ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(GMMTest::onAttributeCreate);
        modEventBus.addListener(GMMTest::onSpawnPlacements);
        modEventBus.addListener(GMMTestData::gatherData);
        modEventBus.addListener((RegisterGameTestsEvent event) -> event.register(GMMSmokeTests.class));
        NeoForge.EVENT_BUS.addListener(GMMTest::onServerStarted);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        for (GMMTestEntities.MobEntry<?> mob : GMMTestEntities.MOBS) {
            event.put(mob.type().get(), mob.attributes().get().build());
        }
    }

    /**
     * Placement matters even without natural spawning: summon goals and companion spawns check
     * {@code SpawnPlacements.isSpawnPositionOk}, which is NO_RESTRICTIONS for an unregistered type.
     */
    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (GMMTestEntities.MobEntry<?> mob : GMMTestEntities.MOBS) {
            register(event, mob.type().get(), mob.name());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Mob> void register(RegisterSpawnPlacementsEvent event, EntityType<T> type, String name) {
        switch (name) {
            case "alligator_gar" -> event.register(type, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                    (t, level, reason, pos, random) -> true, RegisterSpawnPlacementsEvent.Operation.OR);
            case "beholder", "death_tyrant", "gazer", "spectator", "winged_skeleton" -> event.register(type, SpawnPlacementTypes.NO_RESTRICTIONS,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (t, level, reason, pos, random) -> true, RegisterSpawnPlacementsEvent.Operation.OR);
            default -> event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, random) -> true, RegisterSpawnPlacementsEvent.Operation.OR);
        }
    }

    /** Dungeon Denizens' gmm wiring, with gmmtest's types and items. */
    private void commonSetup(FMLCommonSetupEvent event) {
        Orc.projectileLauncher = (shooter, target, x, y, z) -> {
            Rock rock = new Rock(GMMTestEntities.ROCK.get(), shooter.level());
            rock.setPos(x, y, z);
            rock.lobTo(shooter, target.getX(), target.getY(0.5D), target.getZ(), 0.8D);
            shooter.level().addFreshEntity(rock);
        };

        ParalysisSpell.itemSupplier = PARALYSIS_SPELL_ITEM;
        HarmSpell.itemSupplier = HARM_SPELL_ITEM;
        DisintegrateSpell.itemSupplier = DISINTEGRATE_SPELL_ITEM;
        DisarmSpell.itemSupplier = DISARM_SPELL_ITEM;
        Rock.itemSupplier = ROCK_ITEM;
        WitheringGazeSpell.itemSupplier = WITHERING_GAZE_SPELL_ITEM;

        CastSpellGoal.SpellLauncher paralysisSpell = (caster, target, x, y, z) ->
                launch(new ParalysisSpell(GMMTestEntities.PARALYSIS_SPELL.get(), caster.level()), caster, target, x, y, z);
        CastSpellGoal.SpellLauncher harmSpell = (caster, target, x, y, z) ->
                launch(new HarmSpell(GMMTestEntities.HARM_SPELL.get(), caster.level()), caster, target, x, y, z);
        CastSpellGoal.SpellLauncher disintegrateSpell = (caster, target, x, y, z) ->
                launch(new DisintegrateSpell(GMMTestEntities.DISINTEGRATE_SPELL.get(), caster.level()), caster, target, x, y, z);
        CastSpellGoal.SpellLauncher disarmSpell = (caster, target, x, y, z) ->
                launch(new DisarmSpell(GMMTestEntities.DISARM_SPELL.get(), caster.level()), caster, target, x, y, z);
        CastSpellGoal.SpellLauncher witheringGazeSpell = (caster, target, x, y, z) ->
                launch(new WitheringGazeSpell(GMMTestEntities.WITHERING_GAZE_SPELL.get(), caster.level()), caster, target, x, y, z);

        WeightedCollection<Integer, CastSpellGoal.SpellLauncher> beholderSpells = new WeightedCollection<>();
        beholderSpells.add(3, paralysisSpell);
        beholderSpells.add(2, harmSpell);
        beholderSpells.add(1, disintegrateSpell);
        beholderSpells.add(1, disarmSpell);
        Beholder.spellCaster = beholderSpells;

        WeightedCollection<Integer, CastSpellGoal.SpellLauncher> gazerSpells = new WeightedCollection<>();
        gazerSpells.add(3, paralysisSpell);
        gazerSpells.add(1, harmSpell);
        Gazer.spellCaster = gazerSpells;

        DeathTyrant.spellCaster = paralysisSpell;
        Spectator.spellCaster = paralysisSpell;

        WeightedCollection<Double, EntityType<? extends Mob>> warlordMobs = new WeightedCollection<>();
        warlordMobs.add(75D, GMMTestEntities.ORC.get());
        warlordMobs.add(25D, GMMTestEntities.ORC_SHAMAN.get());
        OrcWarlord.summonMobs = warlordMobs;

        WeightedCollection<Double, EntityType<? extends Mob>> beholderMobs = new WeightedCollection<>();
        beholderMobs.add(60D, GMMTestEntities.HEADLESS.get());
        beholderMobs.add(40D, GMMTestEntities.ORC.get());
        beholderMobs.add(20D, GMMTestEntities.SPECTATOR.get());
        beholderMobs.add(20D, EntityType.BLAZE);
        Beholder.summonMobs = beholderMobs;
        Beholder.summonDaemon = GMMTestEntities.DAEMON.get();

        WeightedCollection<Double, EntityType<? extends Mob>> gazerMobs = new WeightedCollection<>();
        gazerMobs.add(33D, GMMTestEntities.HEADLESS.get());
        gazerMobs.add(33D, GMMTestEntities.ORC.get());
        gazerMobs.add(34D, EntityType.ZOMBIE);
        gazerMobs.add(20D, EntityType.VEX);
        Gazer.summonMobs = gazerMobs;

        WeightedCollection<Double, EntityType<? extends Mob>> deathTyrantMobs = new WeightedCollection<>();
        deathTyrantMobs.add(20D, EntityType.ZOMBIE);
        deathTyrantMobs.add(20D, EntityType.HUSK);
        deathTyrantMobs.add(20D, EntityType.SKELETON);
        deathTyrantMobs.add(60D, GMMTestEntities.SKELETON_WARRIOR.get());
        DeathTyrant.summonMobs = deathTyrantMobs;
        DeathTyrant.summonDaemon = GMMTestEntities.DAEMON.get();

        Daemon.fireSpoutLauncher = (daemon, x, y, z, x2, y2, z2) -> {
            FireSpoutSpell spell = new FireSpoutSpell(GMMTestEntities.FIRESPOUT_SPELL.get(), daemon.level());
            spell.init(daemon, x, y, z, x2, y2, z2);
            daemon.level().addFreshEntity(spell);
        };

        Shadowlord.spellCaster = harmSpell;
        WeightedCollection<Double, EntityType<? extends Mob>> shadowlordMobs = new WeightedCollection<>();
        shadowlordMobs.add(70D, GMMTestEntities.SHADOW.get());
        shadowlordMobs.add(30D, GMMTestEntities.GHOUL.get());
        Shadowlord.summonMobs = shadowlordMobs;
        Shadowlord.summonDaemon = GMMTestEntities.DAEMON.get();
        Shadowlord.weapon = SHADOW_BLADE::get;

        OrcShaman.spellCaster = (caster, target, x, y, z) ->
                SpikeGrowthSpell.cast(GMMTestEntities.SPIKE_GROWTH_SPELL.get(), caster, target);
        Bodak.spellCaster = witheringGazeSpell;

        TaintedSkeleton.shardFactory = (shooter, level) -> new BoneShard(GMMTestEntities.BONE_SHARD.get(), shooter, level);
        BloodyBones.shardFactory = (shooter, level) -> new BoneShard(GMMTestEntities.BONE_SHARD.get(), shooter, level);
        Bloater.armFactory = (shooter, level) -> new BloaterArm(GMMTestEntities.BLOATER_ARM.get(), shooter, level);
    }

    private static void launch(GMMHurtingProjectile spell, LivingEntity caster, LivingEntity target, double x, double y, double z) {
        spell.init(caster, target.getX() - x, target.getY(0.5D) - y, target.getZ() - z);
        spell.setPos(x, y, z);
        caster.level().addFreshEntity(spell);
    }

    /**
     * Every mob type must construct (so it has attributes), and its class-side {@code isUndead()} must
     * match the {@code #minecraft:undead} tag built from {@link GMMVanillaTags}. A mismatch means the
     * helper's class list has drifted from the {@code isUndead()} overrides.
     */
    private static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().overworld();
        List<String> problems = new ArrayList<>();
        for (GMMTestEntities.MobEntry<?> mob : GMMTestEntities.MOBS) {
            EntityType<?> type = mob.type().get();
            Entity entity;
            try {
                entity = type.create(level);
            } catch (RuntimeException e) {
                problems.add(mob.name() + ": failed to construct (" + e + ")");
                continue;
            }
            if (entity == null) {
                problems.add(mob.name() + ": create() returned null");
                continue;
            }
            boolean undead = entity instanceof IGMMMonster gmm && gmm.isUndead();
            boolean tagged = type.is(EntityTypeTags.UNDEAD);
            if (undead != tagged) {
                problems.add(mob.name() + ": isUndead()=" + undead + " but #minecraft:undead=" + tagged);
            }
            if (!(entity instanceof Monster) && !(entity instanceof Mob)) {
                problems.add(mob.name() + ": not a Mob");
            }
            entity.discard();
        }
        // the datapack registry is gmm's one piece of loader wiring that a mob test does not exercise
        int configs = level.registryAccess().registry(GMMRegistries.MOB_CONFIG).map(r -> r.size()).orElse(-1);
        if (configs <= 0) {
            problems.add("gmm:mob_config registry " + (configs < 0 ? "missing" : "empty") + " -- datapack entries did not load");
        } else {
            LOGGER.info("gmmtest: gmm:mob_config loaded {} entries", configs);
        }
        if (problems.isEmpty()) {
            LOGGER.info("gmmtest: all {} mob types constructed; isUndead() agrees with #minecraft:undead", GMMTestEntities.MOBS.size());
        } else {
            problems.forEach(p -> LOGGER.error("gmmtest: {}", p));
        }
    }
}
