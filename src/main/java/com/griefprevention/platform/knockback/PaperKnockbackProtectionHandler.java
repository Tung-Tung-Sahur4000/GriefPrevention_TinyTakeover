package com.griefprevention.platform.knockback;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Paper implementation of knockback protection handling.
 * Uses Paper's {@link EntityKnockbackByEntityEvent} and {@link EntityPushedByEntityAttackEvent}.
 * <p>
 * Handles all player-caused knockback including melee attacks (spears),
 * projectiles (wind charges), mace smash AoE, and other mechanisms (shield blocks).
 * <p>
 * Paper resolves projectiles to their shooter, so {@code getHitBy()} returns
 * the player directly for both direct attacks and projectile-caused knockback.
 * <p>
 * This event is preferred over Bukkit's version on Paper servers because it fires
 * first and is not deprecated on Paper.
 * <p>
 * <b>Wind Burst mitigation:</b> Cancelling {@link EntityPushedByEntityAttackEvent} does not on
 * its own stop Wind Burst enchantment explosion knockback, due to a Paper bug where the
 * cancellation flag is ignored for that effect. See
 * <a href="https://github.com/PaperMC/Paper/issues/13079">Paper #13079</a>. As a workaround, when
 * a push is blocked its acceleration vector is also zeroed via
 * {@link EntityPushedByEntityAttackEvent#setAcceleration(Vector)}, which neutralizes the knockback
 * even when the cancellation is disregarded. This is a no-op when cancellation works normally, as a
 * zeroed acceleration is simply not applied.
 */
public class PaperKnockbackProtectionHandler extends KnockbackProtectionHandler
{

    public PaperKnockbackProtectionHandler(@NotNull DataStore dataStore, @NotNull GriefPrevention plugin)
    {
        super(dataStore, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityKnockbackByEntity(@NotNull EntityKnockbackByEntityEvent event)
    {
        if (!(event.getHitBy() instanceof Player attacker)) return;

        if (event.getEntity() instanceof Player defender)
        {
            handleKnockbackPlayer(event, attacker, defender);
        }
        else
        {
            handleKnockbackEntity(event, attacker, event.getEntity());
        }
    }

    /**
     * Handle push events from AoE attacks like mace smash.
     * This is Paper-specific and handles knockback that doesn't go through
     * the normal {@link EntityKnockbackByEntityEvent}.
     * <p>
     * Note: Wind Burst enchantment knockback ignores this event's cancellation on Paper
     * (bug #13079), so a blocked push additionally zeroes the acceleration vector below.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityPushedByEntityAttack(@NotNull EntityPushedByEntityAttackEvent event)
    {
        Entity pusher = event.getPushedBy();

        Player attacker;
        if (pusher instanceof Player player)
        {
            attacker = player;
        }
        else if (pusher instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter)
        {
            attacker = shooter;
        }
        else
        {
            return;
        }

        if (event.getEntity() instanceof Player defender)
        {
            handleKnockbackPlayer(event, attacker, defender);
        }
        else
        {
            handleKnockbackEntity(event, attacker, event.getEntity());
        }

        // Workaround for Paper #13079: cancelling this event does not stop Wind Burst explosion
        // knockback on its own. Zeroing the acceleration vector neutralizes the push directly, so
        // protected players and entities are not launched even when the cancellation is ignored.
        if (event.isCancelled())
        {
            event.setAcceleration(new Vector(0, 0, 0));
        }
    }

}
