package mod.gottsch.neo.gmm.core.entity.monster.ghoul;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * @author by Mark Gottschling on 11/7/2025
 */
public class Ghoul extends AbstractGhoul {
    public Ghoul(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    protected void registerGoals() {
        super.registerGoals();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.ATTACK_KNOCKBACK)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MAX_HEALTH)
                .add(Attributes.FOLLOW_RANGE, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3F);  // faster than zombie
    }
}
