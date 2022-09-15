package be.isach.ultracosmetics.cosmetics.type;

import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.pets.*;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.util.ServerVersion;
import be.isach.ultracosmetics.util.SmartLogger;
import be.isach.ultracosmetics.util.SmartLogger.LogLevel;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.cryptomorin.xseries.XMaterial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pet types.
 *
 * @author iSach
 * @since 12-20-2015
 */
public final class PetType extends CosmeticEntType<Pet> {

    private static final List<PetType> ENABLED = new ArrayList<>();
    private static final List<PetType> VALUES = new ArrayList<>();

    private static final Map<EntityType,Class<? extends Pet>> PET_MAP = new HashMap<>();

    public static List<PetType> enabled() {
        return ENABLED;
    }

    public static List<PetType> values() {
        return VALUES;
    }

    public static PetType valueOf(String s) {
        for (PetType petType : VALUES) {
            if (petType.getConfigName().equalsIgnoreCase(s)) return petType;
        }
        return null;
    }

    public static PetType getByName(String s) {
        Optional<PetType> optional = VALUES.stream().filter(value -> value.getConfigName().equalsIgnoreCase(s)).findFirst();
        if (!optional.isPresent()) return null;
        return optional.get();
    }

    public static void checkEnabled() {
        ENABLED.addAll(values().stream().filter(CosmeticType::isEnabled).collect(Collectors.toList()));
    }

    private final String customization;

    private PetType(String configName, XMaterial material, EntityType entityType, Class<? extends Pet> clazz, String customization) {
        super(Category.PETS, configName, material, entityType, clazz);
        this.customization = customization;

        VALUES.add(this);
        PET_MAP.putIfAbsent(entityType, clazz);
    }

    private PetType(String configName, XMaterial material, EntityType entityType, Class<? extends Pet> clazz) {
        this(configName, material, entityType, clazz, null);
    }

    public String getEntityName(Player player) {
        return MessageManager.getMessage("Pets." + getConfigName() + ".entity-displayname").replace("%playername%", player.getName());
    }

    @Override
    public String getName() {
        return MessageManager.getMessage("Pets." + getConfigName() + ".menu-name");
    }

    @Override
    public Pet equip(UltraPlayer player, UltraCosmetics ultraCosmetics) {
        Pet pet = super.equip(player, ultraCosmetics);
        if (pet != null && customization != null) {
            if (!pet.customize(customization)) {
                UltraCosmeticsData.get().getPlugin().getSmartLogger().write(LogLevel.WARNING, "Invalid customization string for pet " + getConfigName());
                player.sendMessage(ChatColor.RED + "Invalid customization string, please contact an admin.");
            }
        }
        return pet;
    }

    public static void register() {
        ServerVersion serverVersion = UltraCosmeticsData.get().getServerVersion();

        new PetType("Piggy", XMaterial.PORKCHOP, EntityType.PIG, PetPiggy.class);
        new PetType("EasterBunny", XMaterial.CARROT, EntityType.RABBIT, PetEasterBunny.class);
        new PetType("Cow", XMaterial.MILK_BUCKET, EntityType.COW, PetCow.class);
        new PetType("Mooshroom", XMaterial.RED_MUSHROOM, EntityType.MUSHROOM_COW, PetMooshroom.class);
        new PetType("Dog", XMaterial.BONE, EntityType.WOLF, PetDog.class);
        new PetType("Chick", XMaterial.EGG, EntityType.CHICKEN, PetChick.class);
        new PetType("Pumpling", XMaterial.PUMPKIN, EntityType.ZOMBIE, UltraCosmeticsData.get().getVersionManager().getModule().getPumplingClass());
        new PetType("ChristmasElf", XMaterial.BEACON, EntityType.VILLAGER, PetChristmasElf.class);
        new PetType("IronGolem", XMaterial.IRON_INGOT, EntityType.IRON_GOLEM, PetIronGolem.class);
        new PetType("Snowman", XMaterial.SNOWBALL, EntityType.SNOWMAN, PetSnowman.class);
        new PetType("Villager", XMaterial.EMERALD, EntityType.VILLAGER, PetVillager.class);
        new PetType("Bat", XMaterial.COAL, EntityType.BAT, PetBat.class);
        new PetType("Sheep", XMaterial.WHITE_WOOL, EntityType.SHEEP, PetSheep.class);
        new PetType("Wither", XMaterial.WITHER_SKELETON_SKULL, EntityType.WITHER, PetWither.class);
        /* Slime disabled because its just constantly jumping in one direction instead of following the player */
        /* new PetType("Slime", XMaterial.SLIME_BALL, "&7&oSquish...", EntityType.SLIME, PetSlime.class); */
        new PetType("Silverfish", XMaterial.GRAY_DYE, EntityType.SILVERFISH, PetSilverfish.class);
        new PetType("Blaze", XMaterial.BLAZE_ROD, EntityType.BLAZE, PetBlaze.class);
        new PetType("Creeper", XMaterial.GUNPOWDER, EntityType.CREEPER, PetCreeper.class);
        new PetType("Enderman", XMaterial.ENDER_PEARL, EntityType.ENDERMAN, PetEnderman.class);
        new PetType("Skeleton", XMaterial.BOW, EntityType.SKELETON, PetSkeleton.class);
        new PetType("Zombie", XMaterial.ROTTEN_FLESH, EntityType.ZOMBIE, PetZombie.class);
        new PetType("Donkey", XMaterial.CHEST, EntityType.DONKEY, PetDonkey.class);
        new PetType("Mule", XMaterial.SADDLE, EntityType.MULE, PetMule.class);
        new PetType("SkeletonHorse", XMaterial.BONE_BLOCK, EntityType.SKELETON_HORSE, PetSkeletonHorse.class);
        /* Squid disabled because its not moving at all, its just turning around all the time */
        /* new PetType("Squid", XMaterial.INK_SAC, EntityType.SQUID, PetSquid.class); */

        // Cases fall through, so for example v1_19 gets all pets of 1.19 and below.
        switch (serverVersion) {
        case v1_19:
            new PetType("Frog", XMaterial.LILY_PAD, EntityType.FROG, PetFrog.class);
            new PetType("Warden", XMaterial.SCULK_SHRIEKER, EntityType.WARDEN, PetWarden.class);
            new PetType("Allay", XMaterial.ALLAY_SPAWN_EGG, EntityType.ALLAY, PetAllay.class);
            new PetType("Goat", XMaterial.GOAT_HORN, EntityType.GOAT, PetGoat.class);
            new PetType("Tadpole", XMaterial.TADPOLE_BUCKET, EntityType.TADPOLE, PetTadpole.class);
        case v1_17:
            new PetType("Axolotl", XMaterial.AXOLOTL_BUCKET, EntityType.AXOLOTL, PetAxolotl.class);
            /* Glow Squid disabled because its not moving at all, its just turning around all the time */
            /* new PetType("GlowSquid", XMaterial.GLOW_INK_SAC, EntityType.GLOW_SQUID, PetGlowSquid.class); */
        case v1_16:
            new PetType("Piglin", XMaterial.GOLD_INGOT, EntityType.PIGLIN, PetPiglin.class);
            new PetType("Strider", XMaterial.WARPED_FUNGUS, EntityType.STRIDER, PetStrider.class);
        case v1_15:
            new PetType("Bee", XMaterial.HONEYCOMB, EntityType.BEE, PetBee.class);
        case v1_14:
            new PetType("Panda", XMaterial.BAMBOO, EntityType.PANDA, PetPanda.class);
            new PetType("Fox", XMaterial.SWEET_BERRIES, EntityType.FOX, PetFox.class);
            new PetType("Kitty", XMaterial.TROPICAL_FISH, EntityType.CAT, PetKitty.class);
            new PetType("Horse", XMaterial.LEATHER_HORSE_ARMOR, EntityType.HORSE, PetHorse.class);
            new PetType("Ocelot", XMaterial.COD, EntityType.OCELOT, PetOcelot.class);
            new PetType("WanderingTrader", XMaterial.WANDERING_TRADER_SPAWN_EGG, EntityType.WANDERING_TRADER, PetWanderingTrader.class);
        case v1_13:
            new PetType("Cod", XMaterial.COD_BUCKET, EntityType.COD, PetCod.class);
            new PetType("Pufferfish", XMaterial.PUFFERFISH, EntityType.PUFFERFISH, PetPufferfish.class);
            new PetType("Salmon", XMaterial.SALMON_BUCKET, EntityType.SALMON, PetSalmon.class);
            new PetType("TropicalFish", XMaterial.TROPICAL_FISH_BUCKET, EntityType.TROPICAL_FISH, PetTropicalFish.class);
            new PetType("Turtle", XMaterial.TURTLE_HELMET, EntityType.TURTLE, PetTurtle.class);
        case v1_12:
            new PetType("Parrot", XMaterial.COOKIE, EntityType.PARROT, PetParrot.class);
        case v1_11:
            new PetType("Llama", XMaterial.RED_WOOL, EntityType.LLAMA, PetLlama.class);
            /* Vex disabled because its just not following the player at all (Besides teleport) */
            /* new PetType("Vex", XMaterial.IRON_SWORD, "&7&oYAAHH Ehehhehe!", EntityType.VEX, PetVex.class); */
        case v1_10:
            new PetType("PolarBear", XMaterial.SNOW_BLOCK, EntityType.POLAR_BEAR, PetPolarBear.class);
        default:
            break;
        }
        if (serverVersion == ServerVersion.v1_18) {
            new PetType("Goat", XMaterial.WHEAT, EntityType.GOAT, PetGoat.class);
        }

        if (!serverVersion.isAtLeast(ServerVersion.v1_14)) {
            new PetType("Kitty", XMaterial.TROPICAL_FISH, EntityType.OCELOT, PetKitty.class);
            new PetType("Horse", XMaterial.LEATHER, EntityType.HORSE, PetHorse.class);
        }

        ConfigurationSection pets = getCustomConfig(Category.PETS);
        if (pets == null) return;

        EntityType type;
        Optional<XMaterial> mat;
        SmartLogger log = UltraCosmeticsData.get().getPlugin().getSmartLogger();
        for (String key : pets.getKeys(false)) {
            ConfigurationSection pet = pets.getConfigurationSection(key);
            if (!pet.isString("type") || !pet.isString("item") || !pet.isString("customization")) {
                log.write(LogLevel.WARNING, "Incomplete custom pet '" + key + "'");
                continue;
            }
            try {
                type = EntityType.valueOf(pet.getString("type").toUpperCase());
            } catch (IllegalArgumentException e) {
                log.write(LogLevel.WARNING, "Invalid entity type for custom pet '" + key + "'");
                continue;
            }
            if (!PET_MAP.containsKey(type)) {
                log.write(LogLevel.WARNING, "Entity type '" + type + "' for pet '" + key + "' does not exist as a pet.");
                continue;
            }
            mat = XMaterial.matchXMaterial(pet.getString("item"));
            if (!mat.isPresent() || !mat.get().parseMaterial().isItem()) {
                log.write(LogLevel.WARNING, "Invalid item for custom pet '" + key + "'");
                continue;
            }
            MessageManager.addMessage(Category.PETS.getConfigPath() + "." + key + ".menu-name", key);
            MessageManager.addMessage(Category.PETS.getConfigPath() + "." + key + ".entity-displayname", "&l%playername%'s " + key);
            MessageManager.addMessage(Category.PETS.getConfigPath() + "." + key + ".Description", "A custom pet!");
            new PetType(key, mat.get(), type, PET_MAP.get(type), pet.getString("customization"));
        }
    }
}
