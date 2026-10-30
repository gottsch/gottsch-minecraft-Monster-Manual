package mod.gottsch.forge.gmm.core.entity.monster;

import mod.gottsch.forge.gmm.core.config.MobConfigHelper;
import mod.gottsch.forge.gmm.core.entity.ai.goal.CastSpellGoal;
import mod.gottsch.forge.gmm.core.entity.ai.goal.ThrowProjectileGoal;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A caster variant of {@link Orc} -- stands off at range rather than closing in, retreating if the
 * target crowds it (the same approach/retreat/hold-ground positioning {@link Orc}'s own ranged rock
 * thrower uses, via {@link ThrowProjectileGoal} with a {@code null} launcher -- positioning only, no
 * projectile), and only fights in melee, with bare fists, if cornered at point-blank. All actual
 * damage is meant to come from spells: a {@link CastSpellGoal} is layered on top (it declares no
 * {@code Goal.Flag}s, so it never fights the positioning goal for control). The shared library owns
 * no concrete spell, so the consumer supplies {@link #spellCaster} (e.g. wired to GMM's own
 * {@code SpikeGrowthSpell.cast(...)}).
 * <p>
 * Weaker than a plain {@link Orc} in both HP and melee damage, per D&D 5e's own Orc stat blocks: a
 * quarterstaff-wielding Orc Shaman's melee (avg ~4 dmg) is roughly 0.44x a martial Orc's greataxe
 * (avg ~9 dmg); the same ratio is applied here to both {@code MAX_HEALTH} and {@code ATTACK_DAMAGE}
 * rather than to melee damage alone, since (unlike the source material) this variant is meant to be
 * frailer overall, not just weaker in melee.
 * <p>
 * Hitbox: the doorway rule on {@link Orc} applies (register under 1.0 wide). Narrowing it only
 * shortens the point-blank fist range; {@link CastSpellGoal}'s minimum range is the configured
 * {@code spellMinRange} (4 blocks), which outweighs the width-derived reach at either size.
 *
 * @author Mark Gottschling on Jul 9, 2026
 */
public class OrcShaman extends Orc {
    /**
     * Consumer-supplied spell(s) this shaman casts. The shared library owns no concrete spell, so a
     * consuming mod sets this (e.g. in its common setup). Left null, the shaman never casts.
     */
    public static CastSpellGoal.SpellLauncher spellCaster;

    /** Lazily created on first use -- see {@link #getCombatGoalOverride()} for why. */
    private Goal standoffGoal;

    public OrcShaman(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        if (spellCaster != null) {
            int chargeTime = (int) MobConfigHelper.get(this).number("spellChargeTime", 120);
            // don't cast Spike Growth if the target is already this close -- the standoff goal's own
            // melee fallback (getCombatGoalOverride()) handles point-blank instead.
            double minRange = MobConfigHelper.get(this).number("spellMinRange", 4.0);
            this.goalSelector.addGoal(4, new CastSpellGoal(this, chargeTime, minRange * minRange, spellCaster));
        }
    }

    /**
     * Replaces {@link Orc}'s melee/throw switch entirely -- a Shaman never lobs rocks and never
     * chases into melee; it holds ground/retreats at range and only swings fists at point-blank.
     * Lazily built (not a field initializer) since {@link Orc}'s own constructor calls
     * {@code reassessWeaponGoal()} before this subclass's field initializers have run.
     */
    @Override
    protected Goal getCombatGoalOverride() {
        if (this.standoffGoal == null) {
            this.standoffGoal = new ThrowProjectileGoal(this, 1.0D, 40, 16F, mob -> true, null);
        }
        return this.standoffGoal;
    }

    /**
     * Never carries a weapon -- D&D's Orc Shaman leans on a quarterstaff/spells, not a martial
     * weapon, and an empty hand keeps its point-blank melee fallback honestly weak (bare fists at
     * the lowered {@link Attributes#ATTACK_DAMAGE} below, no weapon bonus stacking on top).
     */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource randomSource, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(randomSource, difficulty);
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.ATTACK_DAMAGE, 1.5D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.2D)
                .add(Attributes.ARMOR, (double) 2.0F)
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25F);
    }
}
