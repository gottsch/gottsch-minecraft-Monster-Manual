package mod.gottsch.forge.gmm.core.event;

import mod.gottsch.forge.gmm.core.GMM;
import mod.gottsch.forge.gmm.core.damagesource.GMMDamageTypes;
import mod.gottsch.forge.gmm.core.effect.GMMMobEffects;
import mod.gottsch.forge.gmm.core.entity.monster.Leech;
import mod.gottsch.forge.gmm.core.tag.GMMTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Everything about a {@link Leech} that happens to its victim rather than to the leech: the heal block
 * and the ways to get one off.
 *
 * <table>
 *   <caption>Removal</caption>
 *   <tr><th>Out</th><th>How</th><th>Cost</th></tr>
 *   <tr><td>Fire</td><td>catch fire or take any fire damage</td><td>the burn; clears every leech</td></tr>
 *   <tr><td>Burn one</td><td>sneak + use a {@code gmm:leech/burners} item (either hand, air or block)</td>
 *       <td>flint and steel loses 1 durability, a fire charge is used up, a torch is free</td></tr>
 *   <tr><td>Rip one</td><td>sneak + use an empty main hand <em>on a block</em></td>
 *       <td>the leech's {@code ripDamage}, through armour</td></tr>
 *   <tr><td>Helped</td><td>another player right-clicks the victim</td><td>none</td></tr>
 * </table>
 *
 * <p>Ripping needs a block because Forge's empty-hand {@code RightClickEmpty} event fires on the client
 * only: an empty-handed click on air never reaches the server, and GMM has no network channel. Clicking
 * a block does reach it. The interact events are cancelled on <em>both</em> sides, so a sneak-click
 * with a torch removes a leech instead of placing a torch, with no ghost block; only the server removes
 * anything.</p>
 *
 * <p>Each self-removal or helping pull has a 1 s cooldown per acting player, so clearing six leeches
 * still means standing still for six seconds.</p>
 *
 * @author Mark Gottschling on 10/25/2026
 */
@Mod.EventBusSubscriber(modid = GMM.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LeechEvents {
    private static final String COOLDOWN_KEY = GMM.MOD_ID + ":leechRemovalTime";
    private static final int REMOVAL_COOLDOWN_TICKS = 20;

    private enum Action { BURN, RIP }

    /** No healing of any kind while a leech is on you. */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(GMMMobEffects.LEECHED.get())) {
            event.setCanceled(true);
        }
    }

    /** Fire burns every leech off at once. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || !event.getSource().is(DamageTypeTags.IS_FIRE)) {
            return;
        }
        for (Leech leech : Leech.attachedTo(event.getEntity())) {
            leech.detach(Leech.DetachReason.BURNED);
        }
    }

    /** The victim cannot punch its own leech off; another player can. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getTarget() instanceof Leech leech && leech.isAttachedTo(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Action action = selfAction(event.getEntity(), true);
        if (action != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            perform(event.getEntity(), action);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Action action = selfAction(event.getEntity(), false);
        if (action != null) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            perform(event.getEntity(), action);
        }
    }

    /** Another player right-clicking a leeched player pulls one off, free. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getTarget() instanceof Player victim)) {
            return;
        }
        List<Leech> leeches = Leech.attachedTo(victim);
        if (leeches.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        Player helper = event.getEntity();
        if (!helper.level().isClientSide && offCooldown(helper)) {
            leeches.get(0).detach(Leech.DetachReason.PULLED);
        }
    }

    /**
     * What a sneak-use by a leeched player means: burn if either hand holds a burner, rip if the main
     * hand is empty and a block was clicked, otherwise nothing (the click behaves normally).
     */
    private static Action selfAction(Player player, boolean onBlock) {
        if (!player.isShiftKeyDown() || Leech.attachedTo(player).isEmpty()) {
            return null;
        }
        if (burner(player) != null) {
            return Action.BURN;
        }
        if (onBlock && player.getMainHandItem().isEmpty()) {
            return Action.RIP;
        }
        return null;
    }

    private static InteractionHand burner(Player player) {
        if (player.getMainHandItem().is(GMMTags.Items.LEECH_BURNERS)) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(GMMTags.Items.LEECH_BURNERS)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static void perform(Player player, Action action) {
        if (player.level().isClientSide || !offCooldown(player)) {
            return;
        }
        List<Leech> leeches = Leech.attachedTo(player);
        if (leeches.isEmpty()) {
            return;
        }
        Leech leech = leeches.get(0);
        if (action == Action.BURN) {
            InteractionHand hand = burner(player);
            if (hand == null) {
                return;
            }
            ItemStack stack = player.getItemInHand(hand);
            leech.detach(Leech.DetachReason.BURNED);
            if (!player.getAbilities().instabuild) {
                if (stack.isDamageableItem()) {
                    stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                } else if (stack.is(Items.FIRE_CHARGE)) {
                    stack.shrink(1);
                }
            }
        } else {
            float damage = leech.ripDamage();
            leech.detach(Leech.DetachReason.RIPPED);
            if (damage > 0.0F) {
                player.hurt(GMMDamageTypes.source(player.level(), GMMDamageTypes.LEECH_RIP), damage);
            }
        }
    }

    /** True, and starts the cooldown, if the player has not removed a leech in the last second. */
    private static boolean offCooldown(Player player) {
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLong(COOLDOWN_KEY);
        if (now - last < REMOVAL_COOLDOWN_TICKS && now >= last) {
            return false;
        }
        player.getPersistentData().putLong(COOLDOWN_KEY, now);
        return true;
    }
}
