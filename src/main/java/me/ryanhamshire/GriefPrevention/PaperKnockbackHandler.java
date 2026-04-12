package me.ryanhamshire.GriefPrevention;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class PaperKnockbackHandler implements Listener {

    private final @NotNull DataStore dataStore;
    private final @NotNull GriefPrevention instance;

    public PaperKnockbackHandler(@NotNull DataStore dataStore, @NotNull GriefPrevention plugin) {
        this.dataStore = dataStore;
        this.instance = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityPushedByEntityAttack(@NotNull io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent event) {
        handleKnockback(event.getEntity(), event.getPushedBy(), event);
    }

    private void handleKnockback(@NotNull Entity entity, Entity source, @NotNull io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent event) {
        if (!(entity instanceof Player defender))
            return;

        if (source == null)
            return;

        String sourceTypeName = source.getType().name();
        String causeName = event.getCause().name();

        boolean isWindChargeKnockback = causeName.equals("EXPLOSION") && source instanceof Player;

        if (!isWindChargeKnockback && !sourceTypeName.contains("WIND_CHARGE") && !sourceTypeName.equals("BREEZE_WIND_CHARGE")) {
            return;
        }

        Player attacker = null;
        if (source instanceof Player player) {
            attacker = player;
        } else if (source instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }

        if (attacker == null || attacker == defender) {
            return;
        }

        if (!instance.pvpRulesApply(defender.getWorld())) {
            return;
        }

        PlayerData defenderData = dataStore.getPlayerData(defender.getUniqueId());
        Claim claim = dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
        if (claim != null && instance.claimIsPvPSafeZone(claim)) {
            defenderData.lastClaim = claim;
            event.setCancelled(true);
            GriefPrevention.sendRateLimitedErrorMessage(attacker, Messages.CantFightWhileImmune);
        } else {
        }
    }

    public static boolean isPaperEventAvailable() {
        try {
            Class.forName("io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
