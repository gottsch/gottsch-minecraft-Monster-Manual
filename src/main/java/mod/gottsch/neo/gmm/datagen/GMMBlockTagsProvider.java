package mod.gottsch.neo.gmm.datagen;

import mod.gottsch.neo.gmm.core.GMM;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * 
 * @author Mark Gottschling on 11/6/2025
 *
 */
public class GMMBlockTagsProvider extends BlockTagsProvider {

    public GMMBlockTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider,
                                ExistingFileHelper existingFileHelper) {
    	super(output, lookupProvider, GMM.MOD_ID, existingFileHelper);
	}

	@Override
    protected void addTags(Provider provider) {
 	}

}
