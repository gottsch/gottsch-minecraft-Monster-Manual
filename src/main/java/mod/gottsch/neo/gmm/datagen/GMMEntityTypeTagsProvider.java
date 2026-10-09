package mod.gottsch.neo.gmm.datagen;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.tag.GMMTags;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class GMMEntityTypeTagsProvider extends EntityTypeTagsProvider {
	public GMMEntityTypeTagsProvider(PackOutput output, CompletableFuture<Provider> lookup, @Nullable ExistingFileHelper existingFileHelper) {
		super(output, lookup, GMM.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(Provider provider) {
		// Beholder Enthrall candidates: common overworld/dungeon hostiles as a functional-standalone
		// default. Consumers add their own mobs additively (see GMMTags.EntityTypes for the reasoning
		// behind scoping this per-caster rather than sharing one tag across every future enthraller).
		tag(GMMTags.EntityTypes.BEHOLDER_ENTHRALL_CANDIDATES).add(
				EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED,
				EntityType.SKELETON, EntityType.STRAY,
				EntityType.SPIDER, EntityType.CAVE_SPIDER,
				EntityType.CREEPER, EntityType.WITCH,
				EntityType.PILLAGER, EntityType.VINDICATOR,
				EntityType.BLAZE, EntityType.PIGLIN);

		// Wight thrall-raising pools (see Wight#tryRaiseThrall): gmm ships a zombie default for both
		// so a Wight is functional standalone; consumers add their own zombie-family mobs additively.
		tag(GMMTags.EntityTypes.WIGHT_SUMMON_ALLIES).add(EntityType.ZOMBIE);
		tag(GMMTags.EntityTypes.WIGHT_ENTHRALL_CANDIDATES).add(EntityType.ZOMBIE);

		// Category: hostile monsters (cross-cutting, see GMMTags.EntityTypes doc) -- same roster as
		// BEHOLDER_ENTHRALL_CANDIDATES, since "common overworld/dungeon hostile" is the same real-world
		// set either way. First consumer is Wood Golem's protector targeting.
		tag(GMMTags.EntityTypes.CATEGORY_HOSTILE_MONSTERS).add(
				EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED,
				EntityType.SKELETON, EntityType.STRAY,
				EntityType.SPIDER, EntityType.CAVE_SPIDER,
				EntityType.CREEPER, EntityType.WITCH,
				EntityType.PILLAGER, EntityType.VINDICATOR,
				EntityType.BLAZE, EntityType.PIGLIN);
	}
}
