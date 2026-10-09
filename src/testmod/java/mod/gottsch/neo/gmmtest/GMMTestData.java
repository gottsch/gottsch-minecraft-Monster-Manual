package mod.gottsch.neo.gmmtest;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.tag.GMMVanillaTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * gmmtest's datagen ({@code runTestmodData}): the vanilla category tags, through
 * {@link GMMVanillaTags} exactly as a consumer would use it, plus egg/item models and names.
 */
public final class GMMTestData {

    private GMMTestData() { }

    public static void gatherData(GatherDataEvent event) {
        if (!event.getModContainer().getModId().equals(GMMTest.MOD_ID)) {
            return;
        }
        PackOutput output = event.getGenerator().getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        event.getGenerator().addProvider(event.includeServer(), new Tags(output, event.getLookupProvider(), files));
        event.getGenerator().addProvider(event.includeClient(), new Models(output, files));
        event.getGenerator().addProvider(event.includeClient(), new Lang(output));
    }

    private static final class Tags extends EntityTypeTagsProvider {
        Tags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
            super(output, lookup, GMMTest.MOD_ID, files);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            for (GMMTestEntities.MobEntry<?> mob : GMMTestEntities.MOBS) {
                GMMVanillaTags.addTo(this::tag, mob.type().get(), mob.entityClass());
            }
        }
    }

    private static final class Models extends ItemModelProvider {
        Models(PackOutput output, ExistingFileHelper files) {
            super(output, GMMTest.MOD_ID, files);
        }

        @Override
        protected void registerModels() {
            GMMTest.EGGS.keySet().forEach(name -> withExistingParent(name + "_egg", mcLoc("item/template_spawn_egg")));
            for (String spell : new String[]{"paralysis_spell", "harm_spell", "disintegrate_spell", "disarm_spell", "withering_gaze_spell", "rock"}) {
                singleTexture(spell, mcLoc("item/generated"), "layer0", ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "item/" + spell));
            }
            withExistingParent("shadow_blade", mcLoc("item/iron_sword"));
        }
    }

    private static final class Lang extends LanguageProvider {
        Lang(PackOutput output) {
            super(output, GMMTest.MOD_ID, "en_us");
        }

        @Override
        protected void addTranslations() {
            for (GMMTestEntities.MobEntry<?> mob : GMMTestEntities.MOBS) {
                String title = title(mob.name());
                add(mob.type().get(), title);
                add(GMMTest.EGGS.get(mob.name()).get(), title + " Spawn Egg");
            }
            GMMTestEntities.PROJECTILES.forEach(p -> add(p.get(), title(p.getId().getPath())));
            add(GMMTest.SHADOW_BLADE.get(), "Shadow Blade");
            add(GMMTest.PARALYSIS_SPELL_ITEM.get(), "Paralysis Spell");
            add(GMMTest.HARM_SPELL_ITEM.get(), "Harm Spell");
            add(GMMTest.DISINTEGRATE_SPELL_ITEM.get(), "Disintegrate Spell");
            add(GMMTest.DISARM_SPELL_ITEM.get(), "Disarm Spell");
            add(GMMTest.WITHERING_GAZE_SPELL_ITEM.get(), "Withering Gaze Spell");
            add(GMMTest.ROCK_ITEM.get(), "Rock");
        }

        private static String title(String id) {
            StringBuilder b = new StringBuilder();
            for (String word : id.split("_")) {
                b.append(b.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            return b.toString();
        }
    }
}
