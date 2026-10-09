
package mod.gottsch.neo.gmm.datagen;

import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.concurrent.CompletableFuture;

/**
 * 
 * @author Mark Gottschling on 11/6/2025
 *
 */
@EventBusSubscriber(modid = GMM.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        
        if (event.includeServer()) {
        	GMMBlockTagsProvider blockTags = new GMMBlockTagsProvider(output, lookupProvider, event.getExistingFileHelper());
            generator.addProvider(true, blockTags);
            generator.addProvider(true, new GMMItemTagsProvider(output, lookupProvider, blockTags.contentsGetter(), event.getExistingFileHelper()));
            generator.addProvider(true, new GMMEntityTypeTagsProvider(output, lookupProvider, event.getExistingFileHelper()));
        }
    }
}