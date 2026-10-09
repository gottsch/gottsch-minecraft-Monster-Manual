package mod.gottsch.neo.gmm.core.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A bone-fragment projectile: behaves exactly like a vanilla arrow (physics, damage, sticking,
 * pickup) but renders as a tumbling bone shard rather than an arrow. Each shard picks one of a few
 * {@link #VARIANTS} shapes at spawn (synced) for visual variety, and tracks its own spin counter so
 * the renderer can stop tumbling once it lands. Registration-free like every gmm class — the
 * consumer registers the EntityType.
 *
 * @author Mark Gottschling on 7/3/2026
 */
public class BoneShard extends AbstractArrow {

    public static final int VARIANTS = 3;

    private static final EntityDataAccessor<Byte> DATA_VARIANT =
            SynchedEntityData.defineId(BoneShard.class, EntityDataSerializers.BYTE);
    // render scale — 1.0 = default shard. Lets a caller (e.g. Bloody Bones' flung limbs) throw chunkier
    // pieces without a separate model; the renderer multiplies by this. Does not affect physics/damage.
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(BoneShard.class, EntityDataSerializers.FLOAT);

    /** Increments only while airborne; the renderer uses it so a stuck/stopped shard freezes. */
    private int spinTicks;

    public BoneShard(EntityType<? extends BoneShard> entityType, Level level) {
        super(entityType, level);
    }

    public BoneShard(EntityType<? extends BoneShard> entityType, LivingEntity shooter, Level level) {
        super(entityType, shooter, level);
        if (!level.isClientSide) {
            this.setVariant((byte) this.random.nextInt(VARIANTS));
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, (byte) 0);
        this.entityData.define(DATA_SCALE, 1.0F);
    }

    public void setVariant(byte variant) {
        this.entityData.set(DATA_VARIANT, (byte) Math.floorMod(variant, VARIANTS));
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setScale(float scale) {
        this.entityData.set(DATA_SCALE, scale);
    }

    public float getScale() {
        return this.entityData.get(DATA_SCALE);
    }

    public int getSpinTicks() {
        return this.spinTicks;
    }

    /** True once the shard has stuck in a block (arrow-style). */
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
        return new ItemStack(Items.BONE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Variant", (byte) this.getVariant());
        tag.putFloat("Scale", this.getScale());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getByte("Variant"));
        if (tag.contains("Scale")) {
            this.setScale(tag.getFloat("Scale"));
        }
    }
}
