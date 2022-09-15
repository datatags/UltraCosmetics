package be.isach.ultracosmetics.worldguard;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.util.Problem;
import be.isach.ultracosmetics.util.ServerVersion;
import be.isach.ultracosmetics.util.SmartLogger;
import be.isach.ultracosmetics.util.SmartLogger.LogLevel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

public class WorldGuardManager {

    private final UltraCosmetics ultraCosmetics;
    private IFlagManager flagManager;

    public WorldGuardManager(UltraCosmetics ultraCosmetics) {
        this.ultraCosmetics = ultraCosmetics;
    }

    public void register() {
        String path = "be.isach.ultracosmetics.worldguard.";
        if (!UltraCosmeticsData.get().getServerVersion().isAtLeast(ServerVersion.v1_13)) {
            path += "legacy.";
        }
        try {
            flagManager = (IFlagManager) Class.forName(path + "FlagManager").getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
            return;
        } catch (NoClassDefFoundError | NoSuchMethodError | NoSuchMethodException | ClassNotFoundException e) {
            ultraCosmetics.getSmartLogger().write(LogLevel.WARNING, "Couldn't find required classes for WorldGuard integration.");
            ultraCosmetics.getSmartLogger().write(LogLevel.WARNING, "Please make sure you are using the latest version of WorldGuard");
            ultraCosmetics.getSmartLogger().write(LogLevel.WARNING, "for your version of Minecraft. Debug info:");
            e.printStackTrace();
            ultraCosmetics.getSmartLogger().write("WorldGuard support is disabled.");
            ultraCosmetics.addProblem(Problem.WORLDGUARD_HOOK_FAILURE);
            return;
        }
        flagManager.register();
    }

    public void registerPhase2() {
        if (flagManager == null) return;
        SmartLogger log = ultraCosmetics.getSmartLogger();
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            log.write(LogLevel.ERROR, "WorldGuard is not enabled yet! Is WorldGuard up to date? Is another plugin interfering with the load order?");
            log.write(LogLevel.ERROR, "WorldGuard support will be disabled.");
            UltraCosmeticsData.get().getPlugin().addProblem(Problem.WORLDGUARD_HOOK_FAILURE);
            return;
        }
        flagManager.registerPhase2();
        log.write();
        log.write("WorldGuard custom flags enabled");
    }

    public boolean areCosmeticsAllowedHere(Player player, Category category) {
        return allowedCosmeticsState(player, category) == CosmeticRegionState.ALLOWED;
    }

    public CosmeticRegionState allowedCosmeticsState(Player player, Category category) {
        if (flagManager == null) return CosmeticRegionState.ALLOWED;

        if (!flagManager.flagCheck(UCFlag.COSMETICS, player)) {
            return CosmeticRegionState.BLOCKED_ALL;
        }
        if (category != null && !categoryFlagCheck(player, category)) {
            return CosmeticRegionState.BLOCKED_CATEGORY;
        }
        return CosmeticRegionState.ALLOWED;
    }

    public boolean canAffectPlayersHere(Player player) {
        if (flagManager == null) return true;
        return flagManager.flagCheck(UCFlag.COSMETICS, player) && flagManager.flagCheck(UCFlag.AFFECT_PLAYERS, player);
    }

    public boolean areChestsAllowedHere(Player player) {
        if (flagManager == null) return true;
        return flagManager.flagCheck(UCFlag.TREASURE, player);
    }

    public void doCosmeticCheck(Player player, UltraCosmetics uc) {
        if (flagManager == null) return;
        if (!flagManager.flagCheck(UCFlag.COSMETICS, player) && uc.getPlayerManager().getUltraPlayer(player).clear()) {
            player.sendMessage(MessageManager.getMessage("Region-Disabled"));
            return;
        }
        Set<Category> blockedCategories = flagManager.categoryFlagCheck(player);
        if (blockedCategories == null) return;
        for (Category category : blockedCategories) {
            if (blockedCategories.contains(category) && uc.getPlayerManager().getUltraPlayer(player).removeCosmetic(category)) {
                player.sendMessage(MessageManager.getMessage("Region-Disabled-Category")
                        .replace("%category%", ChatColor.stripColor(MessageManager.getMessage("Menu." + category.getMessagesName() + ".Title"))));
            }
        }
    }

    protected boolean categoryFlagCheck(Player player, Category category) {
        Set<Category> categories = flagManager.categoryFlagCheck(player);
        return categories == null || !categories.contains(category);
    }

}
