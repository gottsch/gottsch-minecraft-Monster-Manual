package mod.gottsch.neo.gmm.core.entity.monster.mimic;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Disguised as a plain vanilla chest; ported from Treasure2's {@code VanillaChestMimic}, retextured
 * as GMM's generic, dependency-free base skin (see the MobIdeasCatalog's "Chest Mimic" entry — the
 * richly-skinned chest variants stay in Treasure2).
 * <p>
 * Attributes are the D&D 5e Mimic stat block (Monstrosity, CR 2: 58 HP, AC 12, half speed 15ft,
 * Bite 1d8+3 piercing + 1d8 acid) scaled to MC using the same zombie-baseline ratio the
 * MobIdeasCatalog documents elsewhere (D&D 22 HP {@literal <->} MC 20 HP, {@literal ~}0.91x):
 * <ul>
 *     <li>MAX_HEALTH: 58 HP * 0.91 {@literal ~=} 52</li>
 *     <li>ATTACK_DAMAGE: bite (avg 7.5) + acid (avg 4.5) = 12 avg * 0.91 {@literal ~=} 11</li>
 *     <li>MOVEMENT_SPEED: D&D's 15ft is half of a standard 30ft speed</li>
 * </ul>
 * ARMOR/ARMOR_TOUGHNESS and FOLLOW_RANGE aren't tied to a D&D stat and are kept at Treasure2's
 * original values.
 *
 * @author Mark Gottschling on 7/8/2026 -- ported from Treasure2's VanillaChestMimic
 */
public class VanillaChestMimic extends Mimic {

    public VanillaChestMimic(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 52.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.12D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0D);
    }
}
