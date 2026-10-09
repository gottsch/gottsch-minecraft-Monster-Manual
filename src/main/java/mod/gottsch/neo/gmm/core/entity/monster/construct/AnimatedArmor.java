package mod.gottsch.neo.gmm.core.entity.monster.construct;

import mod.gottsch.neo.gmm.core.entity.ai.goal.GatedGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.ProximityActivateGoal;
import mod.gottsch.neo.gmm.core.entity.ai.goal.target.SummonedOwnerTargetGoal;
import mod.gottsch.neo.gmm.core.entity.monster.GMMMonster;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * D&D 5e's "Animated Armor" construct: a suit of equipped vanilla armor with no wearer, disguised as
 * dungeon dressing (a stand of loot/decoration) until a player wanders close, at which point it
 * animates and attacks. Rendered as a fully invisible {@code HumanoidModel} plus the standard
 * {@code HumanoidArmorLayer} — see {@code AnimatedArmorRenderer} — so the equipped armor pieces
 * <em>are</em> the entity's entire visual identity, no dedicated rig needed. The chestplate slot in
 * particular is never allowed to roll empty (see {@link #selectRandomEquipment}) — it's the one piece
 * that makes the arm sleeves visible, and without it a swinging attack would be completely invisible
 * rather than reading as an animated suit.
 * <p>
 * Ambush shape is a direct sibling of {@code Mimic}'s: every goal is wrapped in {@link GatedGoal} so
 * nothing runs while dormant, and a new {@link ProximityActivateGoal} flips {@link #isActive()}
 * permanently true the instant a player wanders within range — Mimic only ever activates on a hit or
 * an interact, this is the proximity-triggered sibling case {@code Mimic}'s own class doc already
 * flagged as the next consumer of the gating idiom. Deliberately never natural-spawns (no
 * {@code ModEntities.ALL_MOBS} entry, no {@code biome_modifier}) — same treatment as
 * {@code VanillaChestMimic}/{@code BarrelMimic}, a hand/egg-placed prop, not a roaming mob.
 * <p>
 * A mapmaker can anchor a specific instance with vanilla's own {@code Mob#restrictTo(BlockPos, int)}
 * (the same mechanism villagers/iron golems use, and the same idea {@code GraveZombie} applies to its
 * own hand-placed graves) so a posed armory guardian never despawns. As with {@code GraveZombie}, the
 * restriction isn't persisted by vanilla {@code Mob}, so {@link #addAdditionalSaveData} saves/restores
 * it here. Unlike {@code GraveZombie}'s grave (which never moves regardless), the restriction here is
 * despawn-only — {@link #isWithinRestriction} stops enforcing the radius the moment the suit activates,
 * so a provoked guardian can actually chase instead of leashing back to its post mid-fight.
 *
 * @author Mark Gottschling on 7/13/2026
 */
public class AnimatedArmor extends GMMMonster {

    private static final double DEFAULT_ACTIVATE_RANGE = 4.0D;

    private static final EntityDataAccessor<Boolean> DATA_ACTIVE =
            SynchedEntityData.defineId(AnimatedArmor.class, EntityDataSerializers.BOOLEAN);

    public AnimatedArmor(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        // priority 0, no Flags claimed -- runs every tick regardless of what else is gated off
        this.goalSelector.addGoal(0, new ProximityActivateGoal(this, DEFAULT_ACTIVATE_RANGE, this::activate));
        this.goalSelector.addGoal(0, gated(new FloatGoal(this)));
        this.goalSelector.addGoal(1, gated(new MeleeAttackGoal(this, 1.0D, false)));
        this.goalSelector.addGoal(5, gated(new WaterAvoidingRandomStrollGoal(this, 1.0D)));
        this.goalSelector.addGoal(6, gated(new RandomLookAroundGoal(this)));

        this.targetSelector.addGoal(1, gated(new SummonedOwnerTargetGoal(this)));
        this.targetSelector.addGoal(2, gated(new HurtByTargetGoal(this)));
        this.targetSelector.addGoal(2, gated(new NearestAttackableTargetGoal<>(this, Player.class, true, playerNotOwner)));
    }

    /** Same convenience wrapper Mimic uses -- see {@link GatedGoal}. */
    private Goal gated(Goal delegate) {
        return new GatedGoal(delegate, this::isActive);
    }

    @Override
    public void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ACTIVE, false);
    }

    /** Whether this suit has blown its cover -- inert/immobile while false. */
    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }

    public void activate() {
        this.entityData.set(DATA_ACTIVE, true);
    }

    /** A punch/attack always blows its cover -- same "getting struck should never feel unresponsive"
     * philosophy as Mimic. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            activate();
        }
        return hurt;
    }

    /**
     * An unactivated suit is furniture and must not drift off its post, so the anchor holds until it
     * activates rather than until it has a target. See {@code Anchor}.
     */
    @Override
    protected boolean isAnchorSuspended() {
        return isActive() || super.isAnchorSuspended();
    }

    private static final String TAG_ACTIVE = "AnimatedArmorActive";

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_ACTIVE, isActive());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(TAG_ACTIVE) && tag.getBoolean(TAG_ACTIVE)) {
            activate();
        }
    }

    /** A hardcoded last-resort so the chestplate slot is never empty -- see {@link #selectRandomEquipment}. */
    private static final Item FALLBACK_CHESTPLATE = Items.IRON_CHESTPLATE;

    /** A full suit, always -- this is meant to read as a complete standing set of armor, not a
     * partially-equipped skeleton. Each slot rolls independently from its own gmm-owned tag. */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            this.armorDropChances[slot.getIndex()] = 0.15F; // vanilla-standard drop-on-death chance
            this.setItemSlot(slot, selectRandomEquipment(slot));
        }
    }

    /**
     * The chestplate is never allowed to come back empty, even if a consumer's datapack clears
     * {@code gmm:animated_armor/chestplates} down to nothing — vanilla's own {@code HumanoidArmorLayer}
     * only makes the arm sleeves visible when a CHEST-slot piece is worn ({@code setPartVisibility}'s
     * {@code case CHEST} flips {@code body}/{@code rightArm}/{@code leftArm} visible together), and
     * those arm sleeves are the only thing that sells "arms swinging" on an otherwise fully invisible
     * body. Helmet/legs/boots have no such requirement and stay simple empty-tag-means-nothing rolls.
     */
    private ItemStack selectRandomEquipment(EquipmentSlot slot) {
        Optional<Item> equipment = ForgeRegistries.ITEMS.tags()
                .getTag(tagForSlot(slot)).getRandomElement(this.random);
        if (equipment.isPresent()) {
            return new ItemStack(equipment.get(), 1);
        }
        return slot == EquipmentSlot.CHEST ? new ItemStack(FALLBACK_CHESTPLATE, 1) : ItemStack.EMPTY;
    }

    private static TagKey<Item> tagForSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> GMMTags.Items.ANIMATED_ARMOR_HELMETS;
            case CHEST -> GMMTags.Items.ANIMATED_ARMOR_CHESTPLATES;
            case LEGS -> GMMTags.Items.ANIMATED_ARMOR_LEGGINGS;
            default -> GMMTags.Items.ANIMATED_ARMOR_BOOTS;
        };
    }

    /** Poses like decoration -- block-centered and cardinal-facing, same idea as {@code Mimic}'s
     * {@code snapToBlockPose}, so it reads as a placed statue/stand rather than a mob that happens to
     * be standing still. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData,
                                         @Nullable CompoundTag tag) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        // no shared base class calls this for us -- vanilla's own leaf mobs (Zombie, AbstractSkeleton,
        // ...) each call it explicitly from their own finalizeSpawn, same idiom SkeletonWarrior/Wight
        // already follow here; without this line populateDefaultEquipmentSlots below is dead code.
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        BlockPos pos = this.blockPosition();
        this.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        float cardinal = Math.round(this.getYRot() / 90.0F) * 90.0F;
        this.setYRot(cardinal);
        this.setYBodyRot(cardinal);
        this.setYHeadRot(cardinal);
        this.yRotO = cardinal;
        this.yBodyRotO = cardinal;
        this.yHeadRotO = cardinal;
        return spawnGroupData;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ITEM_BREAK;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public void playAmbientSound() {
        // silent while dormant -- a "decoration" shouldn't announce itself
    }
}
