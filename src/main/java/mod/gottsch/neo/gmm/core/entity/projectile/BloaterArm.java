package mod.gottsch.neo.gmm.core.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A severed zombie-arm projectile: behaves like a vanilla arrow (physics, sticking, pickup) but
 * renders as a fixed, standard zombie-arm cuboid using the Bloater's own texture — the "arms" flung
 * loose by {@link mod.gottsch.neo.gmm.core.entity.monster.zombie.Bloater}'s death rupture. Tracks
 * its own spin counter so the renderer can freeze the tumble once it lands — same idiom as
 * {@link BoneShard}, but a single fixed shape rather than several bone-splinter variants.
 * Registration-free like every gmm class; the consumer registers the EntityType.
 *
 * @author Mark Gottschling on 7/14/2026
 */
public class BloaterArm extends AbstractArrow {

    /** Increments only while airborne; the renderer uses it so a stuck/stopped arm freezes. */
    private int spinTicks;

    public BloaterArm(EntityType<? extends BloaterArm> entityType, Level level) {
        super(entityType, level);
    }

    public BloaterArm(EntityType<? extends BloaterArm> entityType, LivingEntity shooter, Level level) {
        super(entityType, shooter, level);
    }

    public int getSpinTicks() {
        return this.spinTicks;
    }

    /** True once the arm has stuck in a block (arrow-style). */
    public boolean isStuck() {
        return this.inGround;
    }

    @Override
    public void tick() {
        super.tick();
        // keep tumbling only while actually flying; stops on contact / when velocity dies
        if (!this.inGround && this.getDeltaMovement().lengthSqr() > 1.0E-6D) {
            this.spinTicks++;
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(Items.ROTTEN_FLESH);
    }
}
