package mod.gottsch.neo.gmm.core.capability;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.monster.ghoul.AbstractGhoul;
import mod.gottsch.neo.gmm.core.entity.ownership.Ownership;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;

/**
 * The loader-specific half of the ownership portability boundary (see {@link Ownership}). On Forge
 * this was a capability attached to every non-GMM living entity; on NeoForge it is a Data
 * Attachment, created on first access by {@link Ownership#of}. GMM mobs implement
 * {@link mod.gottsch.neo.gmm.core.entity.ownership.IOwnable} themselves and never use it.
 * <p>
 * The attachment id is the old capability id, {@code gmm:ownership}. Its serializer writes nothing
 * for an entity nobody owns, so merely asking a zombie who owns it does not add save data to it.
 * <p>
 * Also holds {@link #registerGhoulInventory}: NeoForge registers entity capabilities per
 * {@link EntityType}, and gmm owns no types, so a consumer opts its ghouls in.
 */
public class GMMCapabilities {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GMM.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<OwnershipData>> OWNERSHIP =
            ATTACHMENT_TYPES.register("ownership", () -> AttachmentType.builder(OwnershipData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, OwnershipData>() {
                        @Override
                        public OwnershipData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            OwnershipData data = new OwnershipData();
                            Ownership.load(tag, data);
                            return data;
                        }

                        @Nullable
                        @Override
                        public CompoundTag write(OwnershipData data, HolderLookup.Provider provider) {
                            CompoundTag tag = new CompoundTag();
                            Ownership.save(tag, data);
                            return tag.isEmpty() ? null : tag;
                        }
                    })
                    .build());

    private GMMCapabilities() { }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    /**
     * Exposes a ghoul's carried items as {@link Capabilities.ItemHandler#ENTITY} (Forge exposed it as
     * {@code ForgeCapabilities.ITEM_HANDLER} on every ghoul). Optional: call it from your own
     * {@code RegisterCapabilitiesEvent} listener for each ghoul type you register, if something
     * (a hopper minecart, another mod) should be able to reach into the ghoul's inventory.
     */
    public static void registerGhoulInventory(RegisterCapabilitiesEvent event, EntityType<? extends AbstractGhoul> type) {
        event.registerEntity(Capabilities.ItemHandler.ENTITY, type, (ghoul, context) -> ghoul.getInventory());
    }
}
