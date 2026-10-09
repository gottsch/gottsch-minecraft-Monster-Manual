package mod.gottsch.neo.gmm.core.tag;

import mod.gottsch.neo.gmm.core.entity.monster.Shadow;
import mod.gottsch.neo.gmm.core.entity.monster.Shadowlord;
import mod.gottsch.neo.gmm.core.entity.monster.ghoul.AbstractGhoul;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.AcidSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.BloodyBones;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.BowSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.BurningSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.ElectricSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.SkeletonChampion;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.SkeletonWarrior;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.TaintedSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.skeleton.WingedSkeleton;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Bloater;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Bodak;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.GraveZombie;
import mod.gottsch.neo.gmm.core.entity.monster.zombie.Wight;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The vanilla entity-type tags a gmm mob class belongs in, for a consumer's datagen.
 * <p>
 * 1.20.5 replaced {@code MobType} with tags, and gmm registers no entity types, so it cannot tag
 * its own mobs. {@link mod.gottsch.neo.gmm.core.entity.monster.IGMMMonster#isUndead} keeps the potion
 * behaviour inside the class; Smite, the Wither leaving it alone and vanilla mobs that hunt undead
 * all read {@code #minecraft:undead}, which only the consumer can fill. From an
 * {@code EntityTypeTagsProvider}:
 * <pre>{@code
 * GMMVanillaTags.addTo(this::tag, MyEntities.WIGHT.get(), Wight.class);
 * }</pre>
 * The class list below must agree with every {@code isUndead()} override. The gmmtest reference
 * consumer checks that on server start for every type it registers.
 */
public final class GMMVanillaTags {

    /** Every gmm class whose 1.20.1 {@code getMobType()} returned {@code UNDEAD}; subclasses included. */
    private static final Set<Class<?>> UNDEAD = Set.of(
            AbstractGhoul.class, Shadow.class, Shadowlord.class,
            AcidSkeleton.class, BloodyBones.class, BowSkeleton.class, BurningSkeleton.class,
            ElectricSkeleton.class, SkeletonChampion.class, SkeletonWarrior.class, TaintedSkeleton.class,
            WingedSkeleton.class,
            Bloater.class, Bodak.class, GraveZombie.class, Wight.class);

    private GMMVanillaTags() { }

    /** The vanilla tags an entity of {@code entityClass} should be in. Empty for most gmm mobs. */
    public static List<TagKey<EntityType<?>>> tagsFor(Class<? extends Entity> entityClass) {
        List<TagKey<EntityType<?>>> tags = new ArrayList<>();
        if (UNDEAD.stream().anyMatch(c -> c.isAssignableFrom(entityClass))) {
            tags.add(EntityTypeTags.UNDEAD);
        }
        return tags;
    }

    /**
     * Adds {@code type} to each tag in {@link #tagsFor}.
     *
     * @param tagger {@code this::tag} from an {@code EntityTypeTagsProvider}
     */
    public static void addTo(Function<TagKey<EntityType<?>>, IntrinsicHolderTagsProvider.IntrinsicTagAppender<EntityType<?>>> tagger,
                             EntityType<?> type, Class<? extends Entity> entityClass) {
        for (TagKey<EntityType<?>> tag : tagsFor(entityClass)) {
            tagger.apply(tag).add(type);
        }
    }
}
