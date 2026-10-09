package mod.gottsch.neo.gmmtest;

import mod.gottsch.neo.gmm.core.client.model.*;
import mod.gottsch.neo.gmm.core.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Layers and renderers for every gmmtest type, as Dungeon Denizens' ClientSetup registers them.
 * <p>
 * An entity type with no renderer crashes the client the first time one comes into view, and a
 * dedicated server never notices (Dungeons2 shipped five spell types without one). So after
 * registering, every type in {@link GMMTestEntities} is checked for a renderer and the client refuses
 * to start if any is missing.
 */
@EventBusSubscriber(modid = GMMTest.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GMMTestClient {

    private GMMTestClient() { }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BlackPuddingModel.LAYER_LOCATION, BlackPuddingModel::createBodyLayer);
        event.registerLayerDefinition(HeadlessModel.LAYER_LOCATION, HeadlessModel::createBodyLayer);
        event.registerLayerDefinition(OrcModel.LAYER_LOCATION, OrcModel::createBodyLayer);
        event.registerLayerDefinition(OrcShamanModel.LAYER_LOCATION, OrcShamanModel::createBodyLayer);
        event.registerLayerDefinition(GhoulModel.LAYER_LOCATION, GhoulModel::createBodyLayer);
        event.registerLayerDefinition(SewerGhoulModel.LAYER_LOCATION, SewerGhoulModel::createBodyLayer);
        event.registerLayerDefinition(RatModel.LAYER_LOCATION, RatModel::createBodyLayer);
        event.registerLayerDefinition(AlligatorGarModel.LAYER_LOCATION, AlligatorGarModel::createBodyLayer);
        event.registerLayerDefinition(BodakModel.LAYER_LOCATION, BodakModel::createBodyLayer);
        event.registerLayerDefinition(BloaterZombieModel.LAYER_LOCATION, BloaterZombieModel::createBodyLayer);
        event.registerLayerDefinition(BeholderModel.LAYER_LOCATION, BeholderModel::createBodyLayer);
        event.registerLayerDefinition(DeathTyrantModel.LAYER_LOCATION, DeathTyrantModel::createBodyLayer);
        event.registerLayerDefinition(GazerModel.LAYER_LOCATION, GazerModel::createBodyLayer);
        event.registerLayerDefinition(SpectatorModel.LAYER_LOCATION, SpectatorModel::createBodyLayer);
        event.registerLayerDefinition(BoulderModel.LAYER_LOCATION, BoulderModel::createBodyLayer);
        event.registerLayerDefinition(ShadowModel.LAYER_LOCATION, ShadowModel::createBodyLayer);
        event.registerLayerDefinition(ShadowlordModel.LAYER_LOCATION, ShadowlordModel::createBodyLayer);
        event.registerLayerDefinition(DaemonModel.LAYER_LOCATION, DaemonModel::createBodyLayer);
        event.registerLayerDefinition(SkeletonWarriorModel.LAYER_LOCATION, SkeletonWarriorModel::createBodyLayer);
        event.registerLayerDefinition(WingedSkeletonModel.LAYER_LOCATION, WingedSkeletonModel::createBodyLayer);
        event.registerLayerDefinition(IronSkeletonModel.LAYER_LOCATION, IronSkeletonModel::createBodyLayer);
        event.registerLayerDefinition(MagmaSkeletonModel.LAYER_LOCATION, MagmaSkeletonModel::createBodyLayer);
        event.registerLayerDefinition(FrostSkeletonModel.LAYER_LOCATION, FrostSkeletonModel::createBodyLayer);
        event.registerLayerDefinition(TaintedSkeletonModel.LAYER_LOCATION, TaintedSkeletonModel::createBodyLayer);
        event.registerLayerDefinition(SkeletonChampionModel.LAYER_LOCATION, SkeletonChampionModel::createBodyLayer);
        event.registerLayerDefinition(MinotaurModel.LAYER_LOCATION, MinotaurModel::createBodyLayer);
        event.registerLayerDefinition(ElectricSkeletonModel.LAYER_LOCATION, ElectricSkeletonModel::createBodyLayer);
        event.registerLayerDefinition(ShriekerModel.LAYER_LOCATION, ShriekerModel::createBodyLayer);
        event.registerLayerDefinition(VioletFungusModel.LAYER_LOCATION, VioletFungusModel::createBodyLayer);
        for (int v = 0; v < BoneShardModel.LAYERS.length; v++) {
            final int variant = v;
            event.registerLayerDefinition(BoneShardModel.LAYERS[variant], () -> BoneShardModel.createBodyLayer(variant));
        }
        event.registerLayerDefinition(BloaterArmModel.LAYER_LOCATION, BloaterArmModel::createBodyLayer);
        event.registerLayerDefinition(GargoyleModel.LAYER_LOCATION, GargoyleModel::createBodyLayer);
        event.registerLayerDefinition(MargoyleModel.LAYER_LOCATION, MargoyleModel::createBodyLayer);
        event.registerLayerDefinition(VanillaChestMimicModel.LAYER_LOCATION, VanillaChestMimicModel::createBodyLayer);
        event.registerLayerDefinition(BarrelMimicModel.LAYER_LOCATION, BarrelMimicModel::createBodyLayer);
        event.registerLayerDefinition(AnimatedWeaponModel.LAYER_LOCATION, AnimatedWeaponModel::createBodyLayer);
        event.registerLayerDefinition(WoodGolemModel.LAYER_LOCATION, WoodGolemModel::createBodyLayer);
        event.registerLayerDefinition(StoneColossusModel.LAYER_LOCATION, StoneColossusModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        Set<EntityType<?>> registered = new HashSet<>();
        Registrar r = new Registrar(event, registered);

        r.add(GMMTestEntities.HEADLESS.get(), HeadlessRenderer::new);
        r.add(GMMTestEntities.ORC.get(), OrcRenderer::new);
        r.add(GMMTestEntities.ORC_SHAMAN.get(), OrcShamanRenderer::new);
        r.add(GMMTestEntities.ORC_WARLORD.get(), OrcWarlordRenderer::new);
        r.add(GMMTestEntities.GHOUL.get(), GhoulRenderer::new);
        r.add(GMMTestEntities.SEWER_GHOUL.get(), SewerGhoulRenderer::new);
        r.add(GMMTestEntities.RAT.get(), RatRenderer::new);
        r.add(GMMTestEntities.ALLIGATOR_GAR.get(), AlligatorGarRenderer::new);
        r.add(GMMTestEntities.BEHOLDER.get(), BeholderRenderer::new);
        r.add(GMMTestEntities.DEATH_TYRANT.get(), DeathTyrantRenderer::new);
        r.add(GMMTestEntities.GAZER.get(), GazerRenderer::new);
        r.add(GMMTestEntities.SPECTATOR.get(), SpectatorRenderer::new);
        r.add(GMMTestEntities.BOULDER.get(), BoulderRenderer::new);
        r.add(GMMTestEntities.SHADOW.get(), ShadowRenderer::new);
        r.add(GMMTestEntities.SHADOWLORD.get(), ShadowlordRenderer::new);
        r.add(GMMTestEntities.DAEMON.get(), DaemonRenderer::new);
        r.add(GMMTestEntities.SKELETON_WARRIOR.get(), SkeletonWarriorRenderer::new);
        r.add(GMMTestEntities.WINGED_SKELETON.get(), WingedSkeletonRenderer::new);
        r.add(GMMTestEntities.IRON_SKELETON.get(), IronSkeletonRenderer::new);
        r.add(GMMTestEntities.MAGMA_SKELETON.get(), MagmaSkeletonRenderer::new);
        r.add(GMMTestEntities.FROST_SKELETON.get(), FrostSkeletonRenderer::new);
        r.add(GMMTestEntities.TAINTED_SKELETON.get(), TaintedSkeletonRenderer::new);
        r.add(GMMTestEntities.ACID_SKELETON.get(), AcidSkeletonRenderer::new);
        r.add(GMMTestEntities.ELECTRIC_SKELETON.get(), ElectricSkeletonRenderer::new);
        r.add(GMMTestEntities.BURNING_SKELETON.get(), BurningSkeletonRenderer::new);
        r.add(GMMTestEntities.BLOODY_BONES.get(), BloodyBonesRenderer::new);
        r.add(GMMTestEntities.BLOATER.get(), BloaterRenderer::new);
        r.add(GMMTestEntities.GRAVE_ZOMBIE.get(), GraveZombieRenderer::new);
        r.add(GMMTestEntities.WIGHT.get(), WightRenderer::new);
        r.add(GMMTestEntities.BODAK.get(), BodakRenderer::new);
        r.add(GMMTestEntities.SKELETON_CHAMPION.get(), SkeletonChampionRenderer::new);
        r.add(GMMTestEntities.MINOTAUR.get(), MinotaurRenderer::new);
        r.add(GMMTestEntities.SHRIEKER.get(), ShriekerRenderer::new);
        r.add(GMMTestEntities.VIOLET_FUNGUS.get(), VioletFungusRenderer::new);
        r.add(GMMTestEntities.GELATINOUS_CUBE.get(), GelatinousCubeRenderer::new);
        r.add(GMMTestEntities.OCHRE_JELLY.get(), OchreJellyRenderer::new);
        r.add(GMMTestEntities.GRAY_OOZE.get(), GrayOozeRenderer::new);
        r.add(GMMTestEntities.BLACK_PUDDING.get(), BlackPuddingRenderer::new);
        r.add(GMMTestEntities.VANILLA_CHEST_MIMIC.get(), VanillaChestMimicRenderer::new);
        r.add(GMMTestEntities.BARREL_MIMIC.get(), BarrelMimicRenderer::new);
        r.add(GMMTestEntities.GARGOYLE.get(), GargoyleRenderer::new);
        r.add(GMMTestEntities.MARGOYLE.get(), MargoyleRenderer::new);
        r.add(GMMTestEntities.ANIMATED_ARMOR.get(), AnimatedArmorRenderer::new);
        r.add(GMMTestEntities.ANIMATED_WEAPON.get(), AnimatedWeaponRenderer::new);
        r.add(GMMTestEntities.WOOD_GOLEM.get(), WoodGolemRenderer::new);
        r.add(GMMTestEntities.STONE_COLOSSUS.get(), StoneColossusRenderer::new);

        // spells: thrown items, full bright (scale, fullBright) -- DD's values
        r.add(GMMTestEntities.PARALYSIS_SPELL.get(), c -> new ThrownItemRenderer<>(c, 1.25F, true));
        r.add(GMMTestEntities.HARM_SPELL.get(), c -> new ThrownItemRenderer<>(c, 1.25F, true));
        r.add(GMMTestEntities.WITHERING_GAZE_SPELL.get(), c -> new ThrownItemRenderer<>(c, 0.6F, true));
        r.add(GMMTestEntities.DISINTEGRATE_SPELL.get(), c -> new ThrownItemRenderer<>(c, 1.25F, true));
        r.add(GMMTestEntities.DISARM_SPELL.get(), c -> new ThrownItemRenderer<>(c, 1.25F, true));
        r.add(GMMTestEntities.FIRESPOUT_SPELL.get(), c -> new ThrownItemRenderer<>(c, 1.5F, true));
        r.add(GMMTestEntities.FIREWALL_COLUMN_SPELL.get(), c -> new ThrownItemRenderer<>(c, 1.0F, true));
        r.add(GMMTestEntities.ROCK.get(), c -> new ThrownItemRenderer<>(c, 0.5F, true));
        r.add(GMMTestEntities.BONE_SHARD.get(), BoneShardRenderer::new);
        r.add(GMMTestEntities.BLOATER_ARM.get(), BloaterArmRenderer::new);
        r.add(GMMTestEntities.SPIKE_GROWTH_SPELL.get(), SpikeGrowthSpellRenderer::new);

        List<String> missing = Stream.concat(
                        GMMTestEntities.MOBS.stream().map(m -> (EntityType<?>) m.type().get()),
                        GMMTestEntities.PROJECTILES.stream().map(h -> (EntityType<?>) h.get()))
                .filter(type -> !registered.contains(type))
                .map(type -> EntityType.getKey(type).toString())
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("gmmtest: no renderer registered for " + missing);
        }
    }

    private record Registrar(EntityRenderersEvent.RegisterRenderers event, Set<EntityType<?>> registered) {
        <T extends Entity> void add(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
            event.registerEntityRenderer(type, provider);
            registered.add(type);
        }
    }
}
