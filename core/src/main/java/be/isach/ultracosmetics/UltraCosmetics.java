package be.isach.ultracosmetics;

import be.isach.ultracosmetics.command.CommandManager;
import be.isach.ultracosmetics.config.AutoCommentConfiguration;
import be.isach.ultracosmetics.config.CustomConfiguration;
import be.isach.ultracosmetics.config.FunctionalConfigLoader;
import be.isach.ultracosmetics.config.ManualCommentConfiguration;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.config.TreasureManager;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.type.CosmeticType;
import be.isach.ultracosmetics.economy.EconomyHandler;
import be.isach.ultracosmetics.hook.ChestSortHook;
import be.isach.ultracosmetics.hook.DiscordSRVHook;
import be.isach.ultracosmetics.hook.PlaceholderHook;
import be.isach.ultracosmetics.hook.TownyHook;
import be.isach.ultracosmetics.listeners.Listener113;
import be.isach.ultracosmetics.listeners.MainListener;
import be.isach.ultracosmetics.listeners.PlayerListener;
import be.isach.ultracosmetics.listeners.PriorityListener;
import be.isach.ultracosmetics.listeners.UnmovableItemListener;
import be.isach.ultracosmetics.menu.CosmeticsInventoryHolder;
import be.isach.ultracosmetics.menu.Menus;
import be.isach.ultracosmetics.mysql.MySqlConnectionManager;
import be.isach.ultracosmetics.permissions.PermissionManager;
import be.isach.ultracosmetics.player.UltraPlayerManager;
import be.isach.ultracosmetics.run.FallDamageManager;
import be.isach.ultracosmetics.run.InvalidWorldChecker;
import be.isach.ultracosmetics.run.VanishChecker;
import be.isach.ultracosmetics.util.ArmorStandManager;
import be.isach.ultracosmetics.util.EntitySpawningManager;
import be.isach.ultracosmetics.util.PermissionPrinter;
import be.isach.ultracosmetics.util.PlayerUtils;
import be.isach.ultracosmetics.util.Problem;
import be.isach.ultracosmetics.util.ServerVersion;
import be.isach.ultracosmetics.util.SmartLogger;
import be.isach.ultracosmetics.util.SmartLogger.LogLevel;
import be.isach.ultracosmetics.util.UpdateManager;
import be.isach.ultracosmetics.version.VersionManager;
import be.isach.ultracosmetics.worldguard.WorldGuardManager;
import com.cryptomorin.xseries.XMaterial;
import me.libraryaddict.disguise.DisguiseConfig;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Main class of the plugin.
 *
 * @author iSach
 * @since 08-03-2015
 */
public class UltraCosmetics extends JavaPlugin {

    private final Map<String, CosmeticItem> cosmetics = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("uc").setExecutor(new CommandManager(this));
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        loadCosmetics();
    }

    private void loadCosmetics() {
        List<String> enabledEmotes = getConfig().getStringList("enabled-emotes");
        cosmetics.putAll(Emote.loadEmotes(EmoteType.getEnabledTypes(enabledEmotes)));

        List<String> enabledGadgets = getConfig().getStringList("enabled-gadgets");
        cosmetics.putAll(Gadget.loadGadgets(GadgetType.getEnabledTypes(enabledGadgets)));

        List<String> enabledHats = getConfig().getStringList("enabled-hats");
        cosmetics.putAll(Hat.loadHats(HatType.getEnabledTypes(enabledHats)));

        List<String> enabledMorphs = getConfig().getStringList("enabled-morphs");
        cosmetics.putAll(Morph.loadMorphs(MorphType.getEnabledTypes(enabledMorphs)));

        List<String> enabledMounts = getConfig().getStringList("enabled-mounts");
        cosmetics.putAll(Mount.loadMounts(MountType.getEnabledTypes(enabledMounts)));

        List<String> enabledParticleEffects = getConfig().getStringList("enabled-particle-effects");
        cosmetics.putAll(ParticleEffect.loadParticleEffects(ParticleEffectType.getEnabledTypes(enabledParticleEffects)));

        List<String> enabledPets = getConfig().getStringList("enabled-pets");
        cosmetics.putAll(Pet.loadPets(PetType.getEnabledTypes(enabledPets)));

        List<String> enabledSuits = getConfig().getStringList("enabled-suits");
        cosmetics.putAll(Suit.loadSuits(SuitType.getEnabledTypes(enabledSuits)));
    }

    public CosmeticItem getCosmeticItem(String name) {
        return cosmetics.get(name.toLowerCase());
    }

    public void addCosmeticItem(String name, CosmeticItem item) {
        cosmetics.put(name.toLowerCase(), item);
    }

    public void removeCosmeticItem(String name) {
        cosmetics.remove(name.toLowerCase());
    }

    public List<Player> getOnlinePlayersWithPermission(String permission) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission))
                .collect(Collectors.toList());
    }

    public void giveCosmeticItem(Player player, String name) {
        CosmeticItem item = getCosmeticItem(name);
        if (item != null) {
            item.giveToPlayer(player);
        }
    }

    public void removeCosmeticItem(Player player, String name) {
        CosmeticItem item = getCosmeticItem(name);
        if (item != null) {
            item.removeFromPlayer(player);
        }
    }

    public void removeCosmeticItems(Player player) {
        cosmetics.values().forEach(item -> item.removeFromPlayer(player));
    }

    public void updateCosmeticItems(Player player) {
        cosmetics.values().forEach(item -> item.update(player));
    }

    public void clearCosmeticItems() {
        cosmetics.clear();
    }
}
