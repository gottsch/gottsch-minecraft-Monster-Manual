package mod.gottsch.neo.gmm.core.entity.monster.skeleton;








import javax.annotation.Nullable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import mod.gottsch.neo.gottschcore.random.RandomHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A cold-counterpart to the Magma Skeleton. Where Magma is a fire/Nether skeleton that floats on
 * lava, the Frost Skeleton is an ice creature: it is immune to freezing, applies Slowness (and a
 * freeze shiver) on melee, fires Slowness arrows when it carries a bow, and freezes water into ice
 * as it walks (Frost-Walker style) rather than sinking. Intended for snowy/icy biomes.
 *
 * @author Mark Gottschling on 7/2/2026
 */
public class FrostSkeleton extends BowSkeleton {

    public FrostSkeleton(EntityType<? extends BowSkeleton> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BowSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 20D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    /**
     * Optionally carry a bow (fires Slowness arrows via {@link #getArrow}); otherwise fight in melee
     * (Slowness + freeze applied in {@link #doHurtTarget}). No armor.
     */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (RandomHelper.checkProbability(random, 60)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
    }

    /**
     * Frost arrows: tag the fired arrow with Slowness so ranged hits chill the target.
     */
    @Override
    protected AbstractArrow getArrow(ItemStack itemStack, float power, @Nullable ItemStack weapon) {
        AbstractArrow arrow = super.getArrow(itemStack, power, weapon);
        if (arrow instanceof Arrow tippable) {
            tippable.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 1));
        }
        return arrow;
    }

    /**
     * Melee chill: Slowness + a freeze shiver (the vanilla powder-snow freeze visual) on a landed hit.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            living.setTicksFrozen(living.getTicksFrozen() + 140);
        }
        return hurt;
    }

    /**
     * Immune to freezing / powder snow — it's made of frost.
     */
    @Override
    public boolean canFreeze() {
        return false;
    }

    /**
     * Freezes surrounding water into (frosted) ice as it moves — the thematic "walk on water" for a
     * frost creature (it walks on the ice it makes, rather than floating on the water). Reuses vanilla
     * Frost Walker logic (radius 3, requires being on the ground; the ice melts over time on its own).
     */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.isAlive()) {
            freezeWaterAround(this, this.level(), this.blockPosition(), 1);
        }
    }

    /**
     * Frost Walker I as a mob ability. 1.20.1 called {@code FrostWalkerEnchantment.onEntityMoved}; in 1.21
     * Frost Walker is a data-driven enchantment effect with no static entry point, so this is that
     * method's body: freeze still water one block down within {@code 2 + level} blocks while on the ground.
     */
    private static void freezeWaterAround(LivingEntity entity, Level level, BlockPos pos, int enchantLevel) {
        if (!entity.onGround()) {
            return;
        }
        BlockState frostedIce = Blocks.FROSTED_ICE.defaultBlockState();
        int radius = Math.min(16, 2 + enchantLevel);
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, -1, radius))) {
            if (!target.closerToCenterThan(entity.position(), radius)) {
                continue;
            }
            above.set(target.getX(), target.getY() + 1, target.getZ());
            if (!level.getBlockState(above).isAir()) {
                continue;
            }
            if (level.getBlockState(target) == FrostedIceBlock.meltsInto()
                    && frostedIce.canSurvive(level, target)
                    && level.isUnobstructed(frostedIce, target, CollisionContext.empty())) {
                level.setBlockAndUpdate(target, frostedIce);
                level.scheduleTick(target, Blocks.FROSTED_ICE, Mth.nextInt(entity.getRandom(), 60, 120));
            }
        }
    }
}
