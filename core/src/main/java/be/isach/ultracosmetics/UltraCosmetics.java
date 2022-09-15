package be.isach.ultracosmetics;

import be.isach.ultracosmetics.command.CommandManager;
import be.isach.ultracosmetics.config.AutoCommentConfiguration;
import be.isach.ultracosmetics.config.CustomConfiguration;
import be.isach.ultracosmetics.config.ManualCommentConfiguration;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.config.TreasureManager;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.economy.EconomyHandler;
import be.isach.ultracosmetics.listeners.Listener113;
import be.isach.ultracosmetics.listeners.Listener19;
import be.isach.ultracosmetics.listeners.MainListener;
import be.isach.ultracosmetics.listeners.PlayerListener;
import be.isach.ultracosmetics.listeners.PriorityListener;
import be.isach.ultracosmetics.menu.Menus;
import be.isach.ultracosmetics.mysql.MySqlConnectionManager;
import be.isach.ultracosmetics.permissions.LuckPermsHook;
import be.isach.ultracosmetics.permissions.PermissionCommand;
import be.isach.ultracosmetics.permissions.PermissionProvider;
import be.isach.ultracosmetics.placeholderapi.PlaceholderHook;
import be.isach.ultracosmetics.player.UltraPlayerManager;
import be.isach.ultracosmetics.run.FallDamageManager;
import be.isach.ultracosmetics.run.InvalidWorldChecker;
import be.isach.ultracosmetics.run.MovingChecker;
import be.isach.ultracosmetics.treasurechests.TreasureChestManager;
import be.isach.ultracosmetics.util.ArmorStandManager;
import be.isach.ultracosmetics.util.EntitySpawningManager;
import be.isach.ultracosmetics.util.PermissionPrinter;
import be.isach.ultracosmetics.util.Problem;
import be.isach.ultracosmetics.util.ServerVersion;
import be.isach.ultracosmetics.util.SmartLogger;
import be.isach.ultracosmetics.util.SmartLogger.LogLevel;
import be.isach.ultracosmetics.util.UpdateManager;
import be.isach.ultracosmetics.version.VersionManager;
import be.isach.ultracosmetics.worldguard.CosmeticRegionState;
import be.isach.ultracosmetics.worldguard.WorldGuardManager;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.cryptomorin.xseries.XMaterial;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
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

import me.libraryaddict.disguise.DisguiseConfig;

/**
 * Main class of the plugin.
 *
 * @author iSach
 * @since 08-03-2015
 */
public class UltraCosmetics extends JavaPlugin {
    /**
     * Manages sub commands.
     */
    private CommandManager commandManager;

    /**
     * The Configuration. (config.yml)
     */
    private CustomConfiguration config;

    /**
     * Config File.
     */
    private File file;

    /**
     * Player Manager instance.
     */
    private UltraPlayerManager playerManager;

    /**
     * Smart Logger Instance.
     */
    private SmartLogger smartLogger;

    /**
     * MySql Manager.
     */
    private MySqlConnectionManager mySqlConnectionManager;

    /**
     * Update Manager.
     */
    private UpdateManager updateChecker;

    /**
     * Treasure Chests Manager;
     */
    private TreasureChestManager treasureChestManager;

    /**
     * Menus.
     */
    private Menus menus;

    /**
     * Manages armor stands.
     */
    private ArmorStandManager armorStandManager;

    private EconomyHandler economyHandler;

    private PermissionProvider permissionProvider;

    /**
     * Manages WorldGuard flags.
     */
    private WorldGuardManager worldGuardManager = new WorldGuardManager(this);

    private boolean legacyMessagePrinted = false;
    private boolean enableFinished = false;

    /**
     * Problems with the configuration of UC, severe (aka fatal) or otherwise
     */
    private Set<Problem> activeProblems = new HashSet<>();

    /**
     * Called when plugin is loaded. Used for registering WorldGuard flags as recommended in API documentation.
     */
    @Override
    public void onLoad() {
        // moved to onLoad so it's ready for WorldGuard support
        this.smartLogger = new SmartLogger(getLogger());

        UltraCosmeticsData.init(this);

        Problem problem = UltraCosmeticsData.get().checkServerVersion();
        if (problem != null) {
            activeProblems.add(problem);
            return;
        }

        // Use super.getConfig() because CustomConfiguration doesn't load until onEnable
        boolean worldGuardIntegration = super.getConfig().getBoolean("WorldGuard-Integration", true);
        // Not using isPluginEnabled() because WorldGuard should be
        // loaded but not yet enabled when registering flags
        if (worldGuardIntegration && getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardManager.register();
        }
    }

    /**
     * Called when plugin is enabled.
     */
    @Override
    public void onEnable() {
        // Enable command manager as early as possible
        // so we can print helpful error messages about
        // why the plugin didn't start correctly.
        commandManager = new CommandManager(this);

        // Also register early so we can send a message about issues.
        getServer().getPluginManager().registerEvents(new PriorityListener(this), this);
        // Set up config.
        if (!setUpConfig()) {
            getSmartLogger().write(LogLevel.ERROR, "Failed to load config.yml, shutting down to protect data.");
            getSmartLogger().write(LogLevel.ERROR, "Please run config.yml through a YAML checker site.");
            activeProblems.add(Problem.BAD_CONFIG);
            return;
        }

        // Start update checker ASAP so if there's a problem that can be
        // resolved by updating, the user knows there's an update.
        // (We can't start it before the config loader because we need config settings.)
        if (SettingsManager.getConfig().getBoolean("Check-For-Updates")) {
            getSmartLogger().write("Checking for update...");
            updateChecker = new UpdateManager(this);
            updateChecker.runTaskAsynchronously(this);
        }

        // if early loading failed...
        if (UltraCosmeticsData.get().getServerVersion() == null) {
            getSmartLogger().write(LogLevel.ERROR, "Plugin load has failed, please check earlier in the log for details.");
            return;
        }

        UltraCosmeticsData.get().initConfigFields();

        String langFileName = "messages_" + UltraCosmeticsData.get().getLanguage() + ".yml";
        File langFile = new File(getDataFolder(), langFileName);
        if (!langFile.exists()) {
            try {
                // Copied from JavaPlugin#saveResource
                InputStream in = getResource("messages/" + langFileName);
                OutputStream out = new FileOutputStream(langFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } catch (IOException e) {
                getSmartLogger().write(LogLevel.ERROR, "Failed to copy " + langFileName);
            }
        }

        // Create UltraPlayer Manager.
        this.playerManager = new UltraPlayerManager(this);

        // Beginning of boot log. basic informations.
        getSmartLogger().write("-------------------------------------------------------------------");
        getSmartLogger().write("UltraCosmetics v" + getDescription().getVersion() + " is loading... (server: " + UltraCosmeticsData.get().getServerVersion().getName() + ")");
        getSmartLogger().write("Thanks for downloading it!");
        getSmartLogger().write("Plugin by iSach.");
        getSmartLogger().write("Link: http://bit.ly/UltraCosmetics");

        // Initialize NMS Module
        if (!UltraCosmeticsData.get().initModule()) {
            // This method sets its own problems
            return;
        }

        // Init Message manager.
        if (!MessageManager.success()) {
            getSmartLogger().write(LogLevel.ERROR, "Failed to load messages.yml, shutting down to protect data.");
            getSmartLogger().write(LogLevel.ERROR, "Please run messages.yml through a YAML checker site.");
            activeProblems.add(Problem.BAD_MESSAGES);
            return;
        }

        // reward.yml & design.yml
        new TreasureManager(this);

        // Register Listeners.
        registerListeners();

        // Set up Cosmetics config.
        new CosmeticManager(this).setupCosmeticsConfigs();

        // Can't use Category.MORPHS.isEnabled() here because it checks whether LibsDisguises is enabled on its own
        if (SettingsManager.getConfig().getBoolean("Categories-Enabled." + Category.MORPHS.getConfigPath())) {
            if (!Bukkit.getPluginManager().isPluginEnabled("LibsDisguises")) {
                getSmartLogger().write();
                getSmartLogger().write(LogLevel.WARNING, "Morphs require Lib's Disguises, but it is not installed. Morphs will be disabled.");
                // TODO: make this a Problem?
            } else {
                try {
                    // Option is not present on older versions of LibsDisguises, added in commit af492c2
                    if (!DisguiseConfig.isTallSelfDisguises()) {
                        getSmartLogger().write();
                        getSmartLogger().write(LogLevel.WARNING, "You have TallSelfDisguises disabled in LibsDisguises's players.yml. Self view of morphs may not work as expected.");
                        activeProblems.add(Problem.TALL_DISGUISES_DISABLED);
                    }
                } catch (NoSuchMethodError | NoClassDefFoundError ignored) {
                }
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getSmartLogger().write();
            new PlaceholderHook(this).register();
            getSmartLogger().write("Hooked into PlaceholderAPI");
        }

        // Set up WorldGuard if needed.
        worldGuardManager.registerPhase2();

        // Set up economy if needed.
        setupEconomy();

        setupPermissionProvider();

        if (!UltraCosmeticsData.get().usingFileStorage()) {
            getSmartLogger().write();
            getSmartLogger().write("Connecting to MySQL database...");

            // Start MySQL. May forcefully switch to file storage if it fails to connect.
            mySqlConnectionManager = new MySqlConnectionManager(this);
            if (mySqlConnectionManager.success()) {
                getSmartLogger().write("Connected to MySQL database.");
            } else {
                getSmartLogger().write("File storage will be used instead.");
                activeProblems.add(Problem.SQL_INIT_FAILURE);
            }
        }
        playerManager.initPlayers();

        // Start the Fall Damage and Invalid World Check Runnables.

        new FallDamageManager().runTaskTimerAsynchronously(this, 0, 1);
        new MovingChecker(this).runTaskTimerAsynchronously(this, 0, 1);
        // No need to worry about the invalid world checker if all worlds are allowed
        if (!config.getStringList("Enabled-Worlds").contains("*")) {
            new InvalidWorldChecker(this).runTaskTimerAsynchronously(this, 0, 5);
        }
        armorStandManager = new ArmorStandManager(this);

        // Start up bStats
        setupMetrics();

        reload();

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        PermissionPrinter.printPermissions(this);

        // Ended well :v
        getSmartLogger().write();
        getSmartLogger().write("UltraCosmetics successfully finished loading and is now enabled!");
        getSmartLogger().write("-------------------------------------------------------------------");
        enableFinished = true;
    }

    /**
     * Called on startup and when things need to be reloaded.
     * Currently only some parts of the plugin are reloaded.
     */
    public void reload() {
        this.menus = new Menus(this);
    }

    /**
     * Called when plugin disables.
     */
    @Override
    public void onDisable() {
        // when the plugin is disabled from onEnable, skip cleanup
        if (!enableFinished) return;

        if (mySqlConnectionManager != null && mySqlConnectionManager.success()) {
            mySqlConnectionManager.shutdown();
        }

        playerManager.dispose();

        UltraCosmeticsData.get().getVersionManager().getModule().disable();
    }

    /**
     * Registers Listeners.
     */
    private void registerListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        pluginManager.registerEvents(new PlayerListener(this), this);
        pluginManager.registerEvents(new MainListener(), this);
        pluginManager.registerEvents(new EntitySpawningManager(), this);

        if (UltraCosmeticsData.get().getServerVersion().offhandAvailable()) {
            pluginManager.registerEvents(new Listener19(this), this);
            if (VersionManager.IS_VERSION_1_13) {
                pluginManager.registerEvents(new Listener113(), this);
            }
        }
    }

    /**
     * Sets the economy up.
     */
    private void setupEconomy() {
        economyHandler = new EconomyHandler(this, getConfig().getString("Economy"));
        UltraCosmeticsData.get().checkTreasureChests();
    }

    private void setupPermissionProvider() {
        CustomConfiguration config = SettingsManager.getConfig();
        if (config.getString("TreasureChests.Permission-Add-Command", "").startsWith("!lp-api")) {
            if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                permissionProvider = new LuckPermsHook(this);
                return;
            }
            getSmartLogger().write(LogLevel.WARNING, "Permission-Add-Command was set to '!lp-api' but LuckPerms is not present. Please change it manually.");
            config.set("TreasureChests.Permission-Add-Command", "say Please set Permission-Add-Command in UC config.yml");
        }
        if (config.getBoolean("TreasureChests.Enabled") && config.getString("TreasureChests.Permission-Add-Command", "say ").startsWith("say ")) {
            activeProblems.add(Problem.PERMISSION_COMMAND_NOT_SET);
        }
        permissionProvider = new PermissionCommand();
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 2629);
        String nms;
        ServerVersion sv = UltraCosmeticsData.get().getServerVersion();
        if (sv.isNmsSupported()) {
            nms = sv.getNmsVersion();
        } else {
            nms = "NMS-less " + (sv.isAtLeast(ServerVersion.v1_13) ? "flattening" : "legacy");
        }
        String version = getDescription().getVersion();
        metrics.addCustomChart(new DrilldownPie("uc_by_mc", () -> {
            Map<String,Map<String,Integer>> map = new HashMap<>();
            Map<String,Integer> entry = new HashMap<>();
            entry.put(version, 1);
            map.put(nms, entry);
            return map;
        }));
        metrics.addCustomChart(new DrilldownPie("mc_by_uc", () -> {
            Map<String,Map<String,Integer>> map = new HashMap<>();
            Map<String,Integer> entry = new HashMap<>();
            entry.put(nms, 1);
            map.put(version, entry);
            return map;
        }));
        metrics.addCustomChart(new SimplePie("mysql_enabled", () -> {
            return getConfig().getBoolean("MySQL.Enabled") ? "true" : "false";
        }));
    }

    private boolean setUpConfig() {
        file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveResource("config.yml", false);
        }
        if (!loadConfiguration(file)) return false;

        List<String> disabledCommands = new ArrayList<>();
        disabledCommands.add("hat");
        config.addDefault("Disabled-Commands", disabledCommands, "List of commands that won't work when cosmetics are equipped.", "Command arguments are ignored, commands are blocked when base command matches.");

        List<String> enabledWorlds = new ArrayList<>();
        enabledWorlds.add("*");
        config.addDefault("Enabled-Worlds", enabledWorlds, "List of the worlds where cosmetics are enabled!", "If list contains '*', all worlds will be allowed.");

        config.set("Disabled-Items", null);
        config.addDefault("Economy", "Vault");

        // getInt() defaults to 0 if not found
        if (config.getInt("TreasureChests.Count") < 1 || config.getInt("TreasureChests.Count") > 4) {
            config.set("TreasureChests.Count", 4, "How many treasure chests should be opened per key? Min 1, max 4");
        }
        String mode = config.getString("TreasureChests.Mode", "");
        if (!mode.equalsIgnoreCase("structure") && !mode.equalsIgnoreCase("simple") && !mode.equalsIgnoreCase("both")) {
            config.set("TreasureChests.Mode", "structure", "The treasure chest mode. Options:", "- structure: places blocks and chests (default)", "- simple: only gives <Count> cosmetics, no blocks are placed", "- both: players can choose either mode through the GUI");
        }
        // Add default values people may not have because of an old version of UC.
        if (config.isConfigurationSection("TreasureChests.Location")) {
            config.set("TreasureChests.Locations.Enabled", config.getBoolean("TreasureChests.Location.Enabled"));
            config.set("TreasureChests.Location.Enabled", null);
            ConfigurationSection section = config.getConfigurationSection("TreasureChests.Location");
            config.set("TreasureChests.Location", null);
            config.set("TreasureChests.Locations.default", section);
        }
        if (!config.isConfigurationSection("TreasureChests.Locations")) {
            ConfigurationSection section = config.createSection("TreasureChests.Locations.default");
            config.set("TreasureChests.Locations.default.Enabled", false, "Whether players should be moved to a certain", "location before opening a treasure chest.", "Does not override /uc treasure with position args.");
            config.set("TreasureChests.Location.default.X", 0, "The location players should be moved to.", "Block coordinates only, like 104, not 103.63", "To use the world the player is in, set World to 'none'");
            section.set("Y", 63);
            section.set("Z", 0);
            section.set("World", Bukkit.getWorlds().get(0).getName());
        }

        config.addDefault("TreasureChests.Locations.default.World", Bukkit.getWorlds().get(0).getName());

        if (!config.isInt("TreasureChests.Loots.Money.Min")) {
            int min = 15;
            int max = config.getInt("TreasureChests.Loots.Money.Max");
            if (max < 5) {
                min = 0;
            } else if (max < 15) {
                min = 5;
            }
            config.set("TreasureChests.Loots.Money.Min", min);
        }

        if (!config.isConfigurationSection("TreasureChests.Loots.Gadgets")) {
            ConfigurationSection section = config.createSection("TreasureChests.Loots.Gadgets", "Chance of getting a GADGET", "This is different from ammo!");
            section.set("Enabled", true);
            section.set("Chance", 20);
            section.set("Message.enabled", false);
            section.set("Message.message", "%prefix% &6&l%name% found gadget %gadget%");
        }

        if (!config.isConfigurationSection("TreasureChests.Loots.Suits")) {
            config.createSection("TreasureChests.Loots.Suits");
            config.set("TreasureChests.Loots.Suits.Enabled", true);
            config.set("TreasureChests.Loots.Suits.Chance", 10);
            config.set("TreasureChests.Loots.Suits.Message.enabled", false);
            config.set("TreasureChests.Loots.Suits.Message.message", "%prefix% &6&l%name% found suit part: %suitw%");
        }

        if (!config.isConfigurationSection("Categories.Suits")) {
            ConfigurationSection suits = config.createSection("Categories.Suits");
            suits.set("Main-Menu-Item", XMaterial.LEATHER_CHESTPLATE.parseMaterial().toString());
            suits.set("Go-Back-Arrow", true);
        }

        if (!config.isConfigurationSection("TreasureChests.Loots.Commands")) {
            ConfigurationSection section = config.createSection("TreasureChests.Loots.Commands.shoutout");
            section.set("Name", "&d&lShoutout");
            section.set("Material", "NETHER_STAR");
            section.set("Enabled", false);
            section.set("Chance", 100);
            section.set("Message.enabled", false);
            section.set("Message.message", "%prefix% &6&l%name% found a rare shoutout!");
            section.set("Cancel-If-Permission", "no");
            section.set("Commands", Collections.singletonList("say %name% is awesome!"));

            section = config.createSection("TreasureChests.Loots.Commands.flower");
            section.set("Name", "&e&lFlower");
            section.set("Material", "YELLOW_FLOWER");
            section.set("Enabled", false);
            section.set("Chance", 100);
            section.set("Message.enabled", false);
            section.set("Message.message", "%prefix% &6&l%name% found a flower!");
            section.set("Cancel-If-Permission", "example.yellowflower");
            section.set("Commands", Arrays.asList("give %name% yellow_flower 1", "lp user %name% permission set example.yellowflower true"));
        }

        ConfigurationSection oldSQL = SettingsManager.getConfig().getConfigurationSection("Ammo-System-For-Gadgets.MySQL");
        if (oldSQL != null) {
            SettingsManager.getConfig().set("MySQL", oldSQL);
            SettingsManager.getConfig().set("Ammo-System-For-Gadgets.MySQL", null);
        }
        String oldMysqlKey = "Ammo-System-For-Gadgets.System";
        if (config.isString(oldMysqlKey)) {
            config.set("MySQL.Enabled", !config.getString(oldMysqlKey).equalsIgnoreCase("file"));
            config.set(oldMysqlKey, null);
        }
        config.addDefault("MySQL.Enabled", false);
        config.addDefault("MySQL.hostname", "localhost");
        config.addDefault("MySQL.username", "root");
        config.addDefault("MySQL.password", "password");
        config.addDefault("MySQL.port", "3306");
        config.addDefault("MySQL.database", "database");
        config.addDefault("MySQL.table", "UltraCosmeticsData");

        config.addDefault("Categories.Clear-Cosmetic-Item", XMaterial.REDSTONE_BLOCK.parseMaterial().toString(), "Item where user click to clear a cosmetic.");
        config.addDefault("Categories.Previous-Page-Item", XMaterial.ENDER_PEARL.parseMaterial().toString(), "Previous Page Item");
        config.addDefault("Categories.Next-Page-Item", XMaterial.ENDER_EYE.parseMaterial().toString(), "Next Page Item");
        config.addDefault("Categories.Back-Main-Menu-Item", XMaterial.ARROW.parseMaterial().toString(), "Back to Main Menu Item");
        config.addDefault("Categories.Self-View-Item.When-Enabled", XMaterial.ENDER_EYE.parseMaterial().toString(), "Item in Morphs Menu when Self View enabled.");
        config.addDefault("Categories.Self-View-Item.When-Disabled", XMaterial.ENDER_PEARL.parseMaterial().toString(), "Item in Morphs Menu when Self View disabled.");
        config.addDefault("Categories.Gadgets-Item.When-Enabled", XMaterial.LIGHT_GRAY_DYE.parseMaterial().toString(), "Item in Gadgets Menu when Gadgets enabled.");
        config.addDefault("Categories.Gadgets-Item.When-Disabled", XMaterial.GRAY_DYE.parseMaterial().toString(), "Item in Gadgets Menu when Gadgets disabled.");
        config.addDefault("Categories.Rename-Pet-Item", XMaterial.NAME_TAG.parseMaterial().toString(), "Item in Pets Menu to rename current pet.");
        config.addDefault("Categories.Close-GUI-After-Select", true, "Should GUI close after selecting a cosmetic?");
        config.addDefault("No-Permission.Custom-Item.Lore", Arrays.asList("", "&c&lYou do not have permission for this!", ""));
        config.addDefault("No-Permission.Allow-Purchase", false, "Requires Dont-Show-Item to be false");
        config.addDefault("Categories.Back-To-Main-Menu-Custom-Command.Enabled", false);
        config.addDefault("Categories.Back-To-Main-Menu-Custom-Command.Command", "cc open custommenu.yml {player}");

        config.addDefault("Categories-Enabled.Suits", true, "Do you want to enable Suits category?");

        config.addDefault("Categories.Gadgets.Cooldown-In-ActionBar", true, "You wanna show the cooldown of", "current gadget in action bar?");

        // Remove enabled field, replace by is-enabled (to replace to false by def)
        if (config.contains("Auto-Equip-Cosmetics.enabled")) {
            config.set("Auto-Equip-Cosmetics.enabled", null);
            config.set("Auto-Equip-Cosmetics.is-enabled", false);
        }

        if (config.isBoolean("Auto-Equip-Cosmetics.is-enabled")) {
            boolean autoEquip = config.getBoolean("Auto-Equip-Cosmetics.is-enabled");
            config.set("Auto-Equip-Cosmetics", autoEquip, "Allows for players to auto-equip on join cosmetics they had before disconnecting.", "Supports both flatfile and SQL, choosing SQL when possible.");
        }

        if (!config.contains("allow-damage-to-players-on-mounts")) {
            config.set("allow-damage-to-players-on-mounts", false);
        }

        config.addDefault("WorldGuard-Integration", true, "Whether WorldGuard should be hooked when loading UC", "Disable this if UC has trouble loading WorldGuard");
        config.addDefault("Pets-Are-Silent", false, "Are pets prevented from making sounds?");
        config.addDefault("Gadgets-Are-Silent", false, "Are gadgets prevented from making sounds?");

        if (config.isBoolean("Menu-Item.Give-On-Join")) {
            boolean enabled = config.getBoolean("Menu-Item.Give-On-Join");
            config.set("Menu-Item.Enabled", enabled);
            config.set("Menu-Item.Give-On-Join", null);
            config.set("Menu-Item.Give-On-Respawn", null);
        }
        config.addDefault("Menu-Item.Custom-Model-Data", 0, "Custom model data for the menu item. Only supported on MC >= 1.14.4 (when it was added)");
        config.addDefault("Menu-Item.Open-Menu-On-Inventory-Click", false, "Whether to open cosmetics menu when the menu item is clicked from the player's inventory");
        config.set("Menu-Item.Data", null);
        config.addDefault("Menu-Item.Lore", "&aRight-click with this\n&ato open the menu", "Lore to apply to the menu item. Set to '' to disable");
        config.addDefault("Auto-Equip-Cosmetics", true, "Allows for players to auto-equip on join cosmetics they had before disconnecting.", "Supports both flatfile and SQL, choosing SQL when possible.");
        config.addDefault("Area-Debug", false, "When enabled, prints why area checks failed to the console");
        List<String> airMaterials = new ArrayList<>();
        Arrays.asList(XMaterial.AIR, XMaterial.CAVE_AIR, XMaterial.VOID_AIR, XMaterial.LIGHT).forEach(k -> airMaterials.add(k.name()));
        config.addDefault("Air-Materials", airMaterials, "Materials that are treated as air. Changing these is not recommended.");
        config.addDefault("Auto-Update", false, "Whether UltraCosmetics should automatically download and install new versions.", "Requires Check-For-Updates to be enabled.");
        config.addDefault("Prevent-Cosmetics-In-Vanish", false, "Whether UltraCosmetics should prevent vanished players from using cosmetics.", "Works with any vanish plugin that uses 'vanished' metdata.");
        config.addDefault("Max-Entity-Spawns-Per-Tick", 10, "Limits the number of entities that can be spawned by a single gadget per tick (default 10.)", "Set to 0 to spawn all entities instantly.");

        String pathPrefix = "messages/messages_";
        List<String> supportedLanguages = new ArrayList<>();
        try {
            URL jar = getFile().toURI().toURL();
            ZipInputStream zip = new ZipInputStream(jar.openStream());
            while (true) {
                ZipEntry e = zip.getNextEntry();
                if (e == null) break;
                String path = e.getName();
                if (path.startsWith(pathPrefix)) {
                    // Start string at end of prefix, end string two characters later
                    supportedLanguages.add(path.substring(pathPrefix.length(), pathPrefix.length() + 2));
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        config.set("Supported-Languages", supportedLanguages, "Languages supported by this version of UltraCosmetics.", "This is not a configurable list, just informative.");
        config.addDefault("Language", "en", "The language to use. Can be set to any language listed above.");

        upgradeIdsToMaterials();

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Gets the Custom Player Manager.
     *
     * @return the Custom Player Manager.
     */
    public UltraPlayerManager getPlayerManager() {
        return playerManager;
    }

    /**
     * @return Command Manager.
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * Increase visibility of getFile() for Updater
     */
    @Override
    public File getFile() {
        return super.getFile();
    }

    /**
     * @return Config file
     */
    public File getConfigFile() {
        return file;
    }

    /**
     * Increase visibility of getTextResource for MessageManager
     */
    public Reader getFileReader(String path) {
        return getTextResource(path);
    }

    /**
     * @return Overwrites getConfig to return our own Custom Configuration.
     */
    @Override
    public CustomConfiguration getConfig() {
        return config;
    }

    /**
     * @return Smart Logger Instance.
     */
    public SmartLogger getSmartLogger() {
        return smartLogger;
    }

    /**
     * @return The Update Checker.
     */
    public UpdateManager getUpdateChecker() {
        return updateChecker;
    }

    /**
     * @return The Treasure Chest Manager.
     */
    public TreasureChestManager getTreasureChestManager() {
        return treasureChestManager;
    }

    /**
     * @return The menus.
     */
    public Menus getMenus() {
        return menus;
    }

    /**
     * @return MySql Manager.
     */
    public MySqlConnectionManager getMySqlConnectionManager() {
        return mySqlConnectionManager;
    }

    public ArmorStandManager getArmorStandManager() {
        return armorStandManager;
    }

    public EconomyHandler getEconomyHandler() {
        return economyHandler;
    }

    public PermissionProvider getPermissionProvider() {
        return permissionProvider;
    }

    public WorldGuardManager getFlagManager() {
        return worldGuardManager;
    }

    public boolean worldGuardHooked() {
        return worldGuardManager != null;
    }

    public boolean areCosmeticsAllowedInRegion(Player player, Category category) {
        return !worldGuardHooked() || worldGuardManager.areCosmeticsAllowedHere(player, category);
    }

    public boolean areChestsAllowedInRegion(Player player) {
        return !worldGuardHooked() || worldGuardManager.areChestsAllowedHere(player);
    }

    public boolean arePlayersAffectedInRegion(Player target) {
        return !worldGuardHooked() || worldGuardManager.canAffectPlayersHere(target);
    }

    public CosmeticRegionState cosmeticRegionState(Player player, Category category) {
        if (!worldGuardHooked()) return CosmeticRegionState.ALLOWED;
        return worldGuardManager.allowedCosmeticsState(player, category);
    }

    public void forceRegionCheck(Player target) {
        if (!worldGuardHooked()) return;
        worldGuardManager.doCosmeticCheck(target, this);
    }

    public boolean loadConfiguration(File file) {
        // In 1.18.1 and later, Spigot supports comment preservation and
        // writing comments programmatically, so use built-in methods if we can.
        // Check if the method exists before we load AutoCommentConfig
        try {
            ConfigurationSection.class.getDeclaredMethod("getComments", String.class);
            config = new AutoCommentConfiguration();
        } catch (NoSuchMethodException ignored) {
            // getComments() doesn't exist yet, load ManualCommentConfig
            config = new ManualCommentConfiguration();
        } catch (SecurityException e) {
            // ???
            e.printStackTrace();
            return false;
        }

        try {
            config.load(file);
        } catch (FileNotFoundException ignored) {
        } catch (IOException | InvalidConfigurationException ex) {
            getSmartLogger().write(LogLevel.ERROR, "Cannot load " + file, ex);
            return false;
        }
        return true;
    }

    private void upgradeIdsToMaterials() {
        upgradeKeyToMaterial("Categories.Gadgets.Main-Menu-Item", "409:0", XMaterial.PRISMARINE_SHARD);
        upgradeKeyToMaterial("Categories.Particle-Effects.Main-Menu-Item", "399:0", XMaterial.NETHER_STAR);
        upgradeKeyToMaterial("Categories.Mounts.Main-Menu-Item", "329:0", XMaterial.SADDLE);
        upgradeKeyToMaterial("Categories.Pets.Main-Menu-Item", "352:0", XMaterial.BONE);
        upgradeKeyToMaterial("Categories.Morphs.Main-Menu-Item", "334:0", XMaterial.LEATHER);
        upgradeKeyToMaterial("Categories.Hats.Main-Menu-Item", "314:0", XMaterial.GOLDEN_HELMET);
        upgradeKeyToMaterial("Categories.Suits.Main-Menu-Item", "299:0", XMaterial.LEATHER_CHESTPLATE);
        upgradeKeyToMaterial("Categories.Clear-Cosmetic-Item", "152:0", XMaterial.REDSTONE_BLOCK);

        upgradeKeyToMaterial("Categories.Previous-Page-Item", "368:0", XMaterial.ENDER_PEARL);
        upgradeKeyToMaterial("Categories.Next-Page-Item", "381:0", XMaterial.ENDER_EYE);
        upgradeKeyToMaterial("Categories.Back-Main-Menu-Item", "262:0", XMaterial.ARROW);
        upgradeKeyToMaterial("Categories.Self-View-Item.When-Enabled", "381:0", XMaterial.ENDER_EYE);
        upgradeKeyToMaterial("Categories.Self-View-Item.When-Disabled", "368:0", XMaterial.ENDER_PEARL);
        upgradeKeyToMaterial("Categories.Gadgets-Item.When-Enabled", "351:10", XMaterial.LIGHT_GRAY_DYE);
        upgradeKeyToMaterial("Categories.Gadgets-Item.When-Disabled", "351:8", XMaterial.GRAY_DYE);
        upgradeKeyToMaterial("Categories.Rename-Pet-Item", "421:0", XMaterial.NAME_TAG);

        upgradeKeyToMaterial("TreasureChests.Designs.Classic.center-block", "169:0", XMaterial.SEA_LANTERN);
        upgradeKeyToMaterial("TreasureChests.Designs.Classic.around-center", "5:0", XMaterial.OAK_PLANKS);
        upgradeKeyToMaterial("TreasureChests.Designs.Classic.third-blocks", "5:1", XMaterial.SPRUCE_PLANKS);
        upgradeKeyToMaterial("TreasureChests.Designs.Classic.below-chests", "17:0", XMaterial.OAK_LOG);
        upgradeKeyToMaterial("TreasureChests.Designs.Classic.barriers", "85:0", XMaterial.OAK_FENCE);

        upgradeKeyToMaterial("TreasureChests.Designs.Modern.center-block", "169:0", XMaterial.SEA_LANTERN);
        upgradeKeyToMaterial("TreasureChests.Designs.Modern.around-center", "159:11", XMaterial.BLUE_TERRACOTTA);
        upgradeKeyToMaterial("TreasureChests.Designs.Modern.third-blocks", "155:0", XMaterial.WHITE_TERRACOTTA);
        upgradeKeyToMaterial("TreasureChests.Designs.Modern.below-chests", "159:11", XMaterial.BLUE_TERRACOTTA);
        upgradeKeyToMaterial("TreasureChests.Designs.Modern.barriers", "160:3", XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE);

        upgradeKeyToMaterial("TreasureChests.Designs.Nether.center-block", "89:0", XMaterial.GLOWSTONE);
        upgradeKeyToMaterial("TreasureChests.Designs.Nether.around-center", "88:0", XMaterial.SOUL_SAND);
        upgradeKeyToMaterial("TreasureChests.Designs.Nether.third-blocks", "87:0", XMaterial.NETHERRACK);
        upgradeKeyToMaterial("TreasureChests.Designs.Nether.below-chests", "112:0", XMaterial.NETHER_BRICKS);
        upgradeKeyToMaterial("TreasureChests.Designs.Nether.barriers", "113:0", XMaterial.NETHER_BRICK_FENCE);

        upgradeKeyToMaterial("Fill-Blank-Slots-With-Item.Item", "160:15", XMaterial.BLACK_STAINED_GLASS_PANE);
    }

    private void upgradeKeyToMaterial(String key, String oldValue, XMaterial newValue) {
        if (oldValue.equals(config.getString(key))) {
            if (!legacyMessagePrinted) {
                getSmartLogger().write(LogLevel.WARNING, "You seem to still have numeric IDs in your config, which UC no longer supports.");
                getSmartLogger().write(LogLevel.WARNING, "I'll attempt to upgrade them, but only if the values haven't been touched.");
                legacyMessagePrinted = true;
            }
            config.set(key, newValue.toString());
            getSmartLogger().write(LogLevel.INFO, "Successfully upgraded key '" + key + "' from '" + oldValue + "' to '" + newValue + "'!");
            // this code runs on every startup so don't print "failed to upgrade" message unless there's an actual issue
        } else if (legacyMessagePrinted) {
            getSmartLogger().write(LogLevel.WARNING, "Couldn't upgrade key '" + key + "' because it has been changed. Please upgrade it manually.");
        }
    }

    public void addProblem(Problem problem) {
        activeProblems.add(problem);
    }

    public void removeProblem(Problem problem) {
        activeProblems.remove(problem);
    }

    public Set<Problem> getProblems() {
        return activeProblems;
    }

    public Set<Problem> getSevereProblems() {
        Set<Problem> severe = new HashSet<>(activeProblems);
        severe.removeIf(p -> !p.isSevere());
        return severe;
    }
}
