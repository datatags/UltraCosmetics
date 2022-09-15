package be.isach.ultracosmetics.version;

import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.util.ReflectionUtils;
import be.isach.ultracosmetics.util.ServerVersion;

import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VersionManager {
    // Used for knowing whether to use 'new' API methods that were added in 1.13
    public static boolean IS_VERSION_1_13 = UltraCosmeticsData.get().getServerVersion().isAtLeast(ServerVersion.v1_13);
    public static final String PACKAGE = "be.isach.ultracosmetics";
    // TODO: value as Pair or something?
    private static final Map<UUID,Integer> WORLD_MIN_HEIGHTS = new HashMap<>();
    private static final Map<UUID,Integer> WORLD_MAX_HEIGHTS = new HashMap<>();
    private final ServerVersion serverVersion;
    private IModule module;
    private IEntityUtil entityUtil;
    private IAncientUtil ancientUtil;
    private IFireworkFactory fireworkFactory;
    private Mounts mounts;

    public VersionManager(ServerVersion serverVersion) {
        this.serverVersion = serverVersion;
    }

    public void load() throws ReflectiveOperationException {
        if (serverVersion == ServerVersion.v1_8) {
            ancientUtil = loadModule("AncientUtil");
        } else {
            ancientUtil = new APIAncientUtil();
        }
        module = loadModule("Module");
        entityUtil = loadModule("EntityUtil");
        mounts = new Mounts();
        fireworkFactory = loadModule("FireworkFactory");
    }

    @SuppressWarnings("unchecked")
    private <T> T loadModule(String name) throws ReflectiveOperationException {
        return (T) ReflectionUtils.instantiateObject(Class.forName(PACKAGE + "." + serverVersion.getNmsVersion() + "." + name));
    }

    public IEntityUtil getEntityUtil() {
        return entityUtil;
    }

    public IAncientUtil getAncientUtil() {
        return ancientUtil;
    }

    public IFireworkFactory getFireworkFactory() {
        return fireworkFactory;
    }

    public Mounts getMounts() {
        return mounts;
    }

    public IModule getModule() {
        return module;
    }

    public int getWorldMinHeight(World world) {
        return WORLD_MIN_HEIGHTS.computeIfAbsent(world.getUID(), w -> {
            try {
                return world.getMinHeight();
            } catch (NoSuchMethodError ex) {
                return 0;
            }
        });
    }

    public int getWorldMaxHeight(World world) {
        return WORLD_MAX_HEIGHTS.computeIfAbsent(world.getUID(), w -> {
            try {
                return world.getMaxHeight();
            } catch (NoSuchMethodError ex) {
                return 255;
            }
        });
    }
}
