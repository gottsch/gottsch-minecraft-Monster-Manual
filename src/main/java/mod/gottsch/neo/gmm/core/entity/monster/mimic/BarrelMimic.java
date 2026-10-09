package mod.gottsch.neo.gmm.core.entity.monster.mimic;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Disguised as a barrel — same Mimic (see {@link Mimic}) as {@link VanillaChestMimic}, just a
 * different container skin. In Treasure2, each skin's attributes track that container's loot rarity
 * tier (rarer chest/barrel skin = harder mimic) rather than the container type itself; GMM doesn't
 * carry a rarity system for its generic single-skin ports, so this uses the same D&D 5e Mimic-derived
 * baseline as {@link VanillaChestMimic} rather than picking one of Treasure2's per-rarity numbers.
 * See {@link VanillaChestMimic}'s class doc for the D&D-to-MC scaling math.
 *
 * @author Mark Gottschling on 7/8/2026 -- ported from Treasure2's BarrelMimic
 */
public class BarrelMimic extends Mimic {

    public BarrelMimic(EntityType<? extends Monster> entityType, Level level) {
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
