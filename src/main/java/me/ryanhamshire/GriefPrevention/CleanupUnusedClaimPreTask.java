package me.ryanhamshire.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import net.milkbowl.vault.permission.Permission;
import me.ryanhamshire.GriefPrevention.util.SchedulerUtil;

import java.util.UUID;

// Asynchronously loads player data without caching it in the datastore, then
// passes those data to a claim cleanup task which might decide to delete a claim for inactivity
class CleanupUnusedClaimPreTask implements Runnable {

    private final UUID ownerID;

    CleanupUnusedClaimPreTask(UUID uuid) {
        this.ownerID = uuid;
    }

    @Override
    public void run() {
        OfflinePlayer ownerInfo = Bukkit.getServer().getOfflinePlayer(ownerID);

        if (ownerInfo.isOnline() || ownerInfo.getLastPlayed() <= 0) {
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp != null) {
                Permission vaultPerms = rsp.getProvider();
                if (vaultPerms.playerHas((String) null, ownerInfo, "griefprevention.dontexpire")) {
                    GriefPrevention.AddLogEntry("Skipping expiration for " + ownerInfo.getName() + " due to griefprevention.dontexpire permission.", CustomLogEntryTypes.Debug, true);
                    return;
                }
            }
        }

        PlayerData ownerData = GriefPrevention.instance.dataStore.getPlayerDataFromStorage(ownerID);
        if (ownerData == null || ownerData.getClaims().isEmpty()) {
            return;
        }

        int bonusBlocks = ownerData.getBonusClaimBlocks();
        if (bonusBlocks >= GriefPrevention.instance.config_claims_expirationExemptionBonusBlocks ||
                (bonusBlocks + ownerData.getAccruedClaimBlocks()) >= GriefPrevention.instance.config_claims_expirationExemptionTotalBlocks) {
            GriefPrevention.AddLogEntry("Player " + ownerInfo.getName() + " is exempt from claim expiration based on block counts.", CustomLogEntryTypes.Debug, true);
            return;
        }

        Claim claimToExpire = ownerData.getClaims().get(0);

        SchedulerUtil.runLaterGlobal(GriefPrevention.instance, new CleanupUnusedClaimTask(claimToExpire, ownerData, ownerInfo), 1L);
    }
}