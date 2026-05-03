package me.ryanhamshire.GriefPrevention;

import me.ryanhamshire.GriefPrevention.events.ClaimExpirationEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

class CleanupUnusedClaimTask implements Runnable {
    private final Claim claim;
    private final PlayerData ownerData;
    private final OfflinePlayer ownerInfo;

    CleanupUnusedClaimTask(Claim claim, PlayerData ownerData, OfflinePlayer ownerInfo) {
        this.claim = claim;
        this.ownerData = ownerData;
        this.ownerInfo = ownerInfo;
    }

    @Override
    public void run() {
        GriefPrevention plugin = GriefPrevention.instance;
        long lastPlayed = ownerInfo.getLastPlayed();
        long currentTime = System.currentTimeMillis();

        // Is this just a default new-player chest claim?
        int chestDays = plugin.config_claims_chestClaimExpirationDays;
        if (chestDays > 0 && ownerData.getClaims().size() == 1) {

            int radius = plugin.config_claims_automaticClaimsForNewPlayersRadius;
            int areaOfDefaultClaim = radius >= 0 ? (int) Math.pow(radius * 2 + 1, 2) : 0;

            if (claim.getArea() <= areaOfDefaultClaim) {
                long cutoffTime = currentTime - (chestDays * 86400000L); // Days to milliseconds

                if (lastPlayed < cutoffTime) {
                    if (expireEventCanceled()) return;

                    plugin.dataStore.deleteClaim(claim, true, true);
                    GriefPrevention.AddLogEntry(claim.getOwnerName() + "'s new player claim expired.", CustomLogEntryTypes.AdminActivity);
                    return;
                }
            }
        }

        int expirationDays = plugin.config_claims_expirationDays;
        if (expirationDays > 0) {
            long cutoffTime = currentTime - (expirationDays * 86400000L);

            if (lastPlayed < cutoffTime) {
                if (expireEventCanceled()) return;

                plugin.dataStore.deleteClaimsForPlayer(claim.ownerID, true);
                GriefPrevention.AddLogEntry("All of " + claim.getOwnerName() + "'s claims have expired.", CustomLogEntryTypes.AdminActivity);
            }
        }
    }

    private boolean expireEventCanceled() {
        ClaimExpirationEvent event = new ClaimExpirationEvent(this.claim);
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}