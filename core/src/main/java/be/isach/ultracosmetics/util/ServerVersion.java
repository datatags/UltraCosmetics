package be.isach.ultracosmetics.util;

/**
 * Created by Sacha on 6/03/16.
 */
public enum ServerVersion {

    // Do not supply a mapping version when there is no NMS module.
    v1_8("1.8.8", null, 3),
    v1_9("1.9.4", null, 0),
    v1_10("1.10.2", null, 0),
    v1_11("1.11.2", null, 0),
    v1_12("1.12.2", null, 1),
    v1_13("1.13.2", null, 0),
    v1_14("1.14.4", null, 0),
    v1_15("1.15.2", null, 0),
    v1_16("1.16.5", null, 0),
    v1_17("1.17.1", null, 0),
    v1_18("1.18.2", "eaeedbff51b16ead3170906872fda334", 2),
    v1_19("1.19.3", "1afe2ffe8a9d7fc510442a168b3d4338", 2);

    private final String name;
    // mappingsVersion is a random string that is changed whenever NMS changes
    // which is more often than actual NMS revisions happen. You can find this
    // value by checking the source code of this method:
    // org.bukkit.craftbukkit.util.CraftMagicNumbers#getMappingsVersion
    // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/util/CraftMagicNumbers.java#240
    // getMappingsVersion was added in 1.13.2, earlier versions don't have it.
    private final String mappingsVersion;
    // The NMS revision the corresponding module is built for, or 0 for no module.
    private final int nmsRevision;

    private ServerVersion(String name, String mappingsVersion, int nmsRevision) {
        this.name = name;
        this.mappingsVersion = mappingsVersion;
        this.nmsRevision = nmsRevision;
    }

    public String getName() {
        return name;
    }

    public String getMappingsVersion() {
        return mappingsVersion;
    }

    public static ServerVersion earliest() {
        return values()[0];
    }

    public static ServerVersion latest() {
        return values()[values().length - 1];
    }

    public boolean isAtLeast(ServerVersion version) {
        return this.compareTo(version) >= 0;
    }

    public boolean offhandAvailable() {
        return isAtLeast(v1_9);
    }

    public boolean isNmsSupported() {
        return nmsRevision > 0;
    }

    public String getNmsVersion() {
        return toString() + "_R" + nmsRevision;
    }
}
