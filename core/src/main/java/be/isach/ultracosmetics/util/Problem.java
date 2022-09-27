package be.isach.ultracosmetics.util;

import org.bukkit.ChatColor;

public enum Problem {
    BAD_MC_VERSION(true, "This version of Minecraft is not supported. See: " + ChatColor.GOLD + "https://github.com/datatags/UltraCosmetics/wiki/Supported-Configurations"),
    UNKNOWN_MC_VERSION(true, "Could not parse Minecraft version, please report this issue."),
    BAD_CONFIG(true, "config.yml is invalid, please run it through a YAML checker."),
    BAD_MESSAGES(true, "Messages file is invalid, please run it through a YAML checker."),
    NMS_LOAD_FAILURE(true, "Couldn't load support for this MC version, please report this issue."),

    BAD_MAPPINGS_VERSION(false, "Server internals seem to have changed since this build was created, you will likely experience issues."),
    TALL_DISGUISES_DISABLED(false, "TallSelfDisguises is disabled in LibsDisguises players.yml. This may cause morph self view to not behave as intended."),
    SQL_INIT_FAILURE(false, "SQL failed to connect, using flatfile support instead."),
    WORLDGUARD_HOOK_FAILURE(false, "Failed to hook into WorldGuard."),
    PERMISSION_COMMAND_NOT_SET(false, "Permission-Add-Command needs to be set or treasure chests will not give cosmetics."),
    ;

    private final boolean severe;
    private final String description;

    private Problem(boolean severe, String description) {
        this.severe = severe;
        this.description = description;
    }

    public boolean isSevere() {
        return severe;
    }

    public String getDescription() {
        return description;
    }
}
