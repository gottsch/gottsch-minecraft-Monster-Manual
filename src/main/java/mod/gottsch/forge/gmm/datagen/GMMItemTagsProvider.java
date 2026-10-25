/*
 * This file is part of gottsch's Monster Manual.
 * Copyright (c) 2025 Mark Gottschling (gottsch)
 *
 * gottsch's Monster Manual is free software: you can redistribute it and/or modify
 * it under the terms of the Open Software Licence 3.0.
 *
 * gottsch's Monster Manual is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Open Software Licence 3.0 for more details.
 *
 * You should have received a copy of the Open Software Licence
 * along with gottsch's Monster Manual. If not, see <https://www.tldrlegal.com/license/open-software-licence-3-0>.
 */
package mod.gottsch.forge.gmm.datagen;

import mod.gottsch.forge.gmm.core.GMM;
import mod.gottsch.forge.gmm.core.tag.GMMTags;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class GMMItemTagsProvider extends ItemTagsProvider {
	public GMMItemTagsProvider(PackOutput output, CompletableFuture<Provider> lookup,
							   CompletableFuture<TagLookup<Block>> blockTagProvider, @Nullable ExistingFileHelper existingFileHelper) {
		super(output, lookup, blockTagProvider, GMM.MOD_ID, existingFileHelper);
	}

	@Override
	protected void addTags(Provider provider) {
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.ROTTEN_FLESH);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.BEEF);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.CHICKEN);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.MUTTON);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.RABBIT);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.COD);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.SALMON);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.TROPICAL_FISH);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.COOKED_BEEF);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.COOKED_CHICKEN);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.COOKED_MUTTON);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.COOKED_RABBIT);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.COOKED_COD);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.COOKED_SALMON);

		tag(GMMTags.Items.GHOUL_FOOD).add(Items.BONE);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.LEATHER);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.RABBIT_HIDE);
		tag(GMMTags.Items.GHOUL_FOOD).add(Items.FEATHER);

		// SkeletonWarrior equipment pools: gmm ships sensible weak-vanilla defaults so the mob is
		// functional standalone. gmm owns no items, so these are vanilla only (leather/gold armor,
		// low-tier weapons, some iron); consumer mods add their own items to these tags additively.
		tag(GMMTags.Items.SKELETON_WARRIOR_WEAPONS).add(
				Items.WOODEN_SWORD, Items.WOODEN_AXE,
				Items.STONE_SWORD, Items.STONE_AXE,
				Items.GOLDEN_SWORD, Items.GOLDEN_AXE,
				Items.IRON_SWORD, Items.IRON_AXE);
		tag(GMMTags.Items.SKELETON_WARRIOR_HELMETS).add(
				Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET);
		tag(GMMTags.Items.SKELETON_WARRIOR_CHESTPLATES).add(
				Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE);
		tag(GMMTags.Items.SKELETON_WARRIOR_LEGGINGS).add(
				Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS);
		tag(GMMTags.Items.SKELETON_WARRIOR_BOOTS).add(
				Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS);
		// Shield pool: gmm owns no shield items of its own, so vanilla's own shield is the only default.
		tag(GMMTags.Items.SKELETON_WARRIOR_SHIELDS).add(Items.SHIELD);

		// Orc weapon pool: ship weak-vanilla defaults so Orcs are armed standalone; consumers add more.
		tag(GMMTags.Items.ORC_WEAPONS).add(Items.STONE_SWORD, Items.STONE_AXE);

		// Wight weapon pool: a modest sword + bow default (either melee or ranged, see
		// Wight#reassessWeaponGoal) so it's armed standalone; consumers add their own additively.
		tag(GMMTags.Items.WIGHT_WEAPONS).add(Items.IRON_SWORD, Items.STONE_SWORD, Items.BOW);
		tag(GMMTags.Items.WIGHT_SHIELDS).add(Items.SHIELD);

		// Bodak gaze ward: carved pumpkin blocks Death Gaze, the same item vanilla Enderman already
		// uses to ward off its own gaze-based aggro. Consumers add their own ward items additively.
		tag(GMMTags.Items.BODAK_GAZE_WARD).add(Items.CARVED_PUMPKIN);

		// Leech burners: anything a player already carries into a dungeon that makes a flame.
		tag(GMMTags.Items.LEECH_BURNERS).add(Items.TORCH, Items.SOUL_TORCH, Items.FLINT_AND_STEEL, Items.FIRE_CHARGE);

		// Skeleton Champion weapon pool: an elite leader carries elite steel — diamond + netherite
		// swords by default. Consumers add their own (fancier) weapons additively.
		tag(GMMTags.Items.SKELETON_CHAMPION_WEAPONS).add(Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
		tag(GMMTags.Items.SKELETON_CHAMPION_SHIELDS).add(Items.SHIELD);

		// Minotaur weapon pool: deliberately EMPTY here, unlike SKELETON_CHAMPION_WEAPONS above.
		// The mob has a signature weapon -- the minotaur axe -- and the art for it lives in the
		// consumers (dungeons2, ddenizens), not in gmm. Shipping vanilla axes as a default would
		// mean a consumer that supplies the real axe still rolls a plain iron one two times in
		// three, so gmm ships nothing and lets the consumer's tag entry BE the pool. A gmm install
		// with no consumer leaves the Minotaur bare-fisted, which Minotaur#populateDefaultEquipmentSlots
		// documents as an intended look.

		// Orc Warlord weapon pool: a chief carries a real axe rather than the warband's chipped
		// stone one (ORC_WEAPONS above is stone sword + stone axe). Axes rather than swords because
		// the orc rig and its animation were built around one. Consumers add their own additively.
		tag(GMMTags.Items.ORC_WARLORD_WEAPONS).add(Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);

		// Corrosion immunity (Acid Skeleton, Gelatinous Cube, ...): diamond gear resists it (netherite
		// added too, since it's the higher tier — drop it if only diamond should be immune). Consumers
		// add more.
		tag(GMMTags.Items.CORROSION_IMMUNE).add(
				Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
				Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
				Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
				Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE);

		// Animated Armor: a modest iron-tier default so a spawned suit is fully equipped standalone;
		// consumers add their own armor sets additively.
		tag(GMMTags.Items.ANIMATED_ARMOR_HELMETS).add(Items.IRON_HELMET, Items.CHAINMAIL_HELMET);
		tag(GMMTags.Items.ANIMATED_ARMOR_CHESTPLATES).add(Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE);
		tag(GMMTags.Items.ANIMATED_ARMOR_LEGGINGS).add(Items.IRON_LEGGINGS, Items.CHAINMAIL_LEGGINGS);
		tag(GMMTags.Items.ANIMATED_ARMOR_BOOTS).add(Items.IRON_BOOTS, Items.CHAINMAIL_BOOTS);

		// Animated Weapon: swords + axes across the common tiers, so a spawned instance is armed
		// standalone; consumers add their own weapons additively.
		tag(GMMTags.Items.ANIMATED_WEAPON_WEAPONS).add(
				Items.IRON_SWORD, Items.IRON_AXE,
				Items.STONE_SWORD, Items.STONE_AXE,
				Items.DIAMOND_SWORD, Items.DIAMOND_AXE);
	}
}
