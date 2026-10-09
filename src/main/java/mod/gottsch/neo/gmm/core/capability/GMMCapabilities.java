package mod.gottsch.neo.gmm.core.capability;

import mod.gottsch.neo.gmm.core.GMM;
import mod.gottsch.neo.gmm.core.entity.ownership.IOwnable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.common.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
import net.neoforged.bus.api.IEventBus;

/**
 * Registers the ownership {@link Capability} and attaches it to non-GMM entities so that any mob --
 * not just GMM mob bases -- can carry an owner. This is the Forge-specific half of the ownership
 * portability boundary (see {@link mod.gottsch.neo.gmm.core.entity.ownership.Ownership}); on
 * NeoForge this whole class is replaced by a Data Attachment, with no change to callers.
 *
 * @author Mark Gottschling
 */
public class GMMCapabilities {
    public static final Capability<IOwnable> OWNERSHIP = CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation OWNERSHIP_ID = ResourceLocation.fromNamespaceAndPath(GMM.MOD_ID, "ownership");

    private GMMCapabilities() { }

    /** Wires both listeners: capability registration (mod bus) and per-entity attach (forge bus). */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GMMCapabilities::onRegisterCapabilities);
        NeoForge.EVENT_BUS.addGenericListener(Entity.class, GMMCapabilities::onAttachCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IOwnable.class);
    }

    private static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        // GMM mobs implement IOwnable directly (own fields/synced data); only other living entities
        // need the capability-backed ownership.
        if (entity instanceof LivingEntity && !(entity instanceof IOwnable)) {
            OwnershipProvider provider = new OwnershipProvider();
            event.addCapability(OWNERSHIP_ID, provider);
            event.addListener(provider::invalidate);
        }
    }
}
