package be.isach.ultracosmetics.cosmetics.type;

import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.CustomConfiguration;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.config.SettingsManager;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.morphs.*;
import be.isach.ultracosmetics.version.ServerVersion;
import be.isach.ultracosmetics.version.VersionManager;
import com.cryptomorin.xseries.XMaterial;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;

/**
 * Morph types.
 *
 * @author iSach
 * @since 12-19-2015
 */
public class MorphType extends CosmeticType<Morph> {

    /**
     * Disguise Type of the morph.
     */
    private final EntityType disguiseType;
    private final boolean doesSkillExist;

    private MorphType(String configName, XMaterial material, EntityType disguiseType) {
        this(configName, material, disguiseType, MorphBasic.class, false);
    }

    private MorphType(String configName, XMaterial material, EntityType disguiseType, Class<? extends Morph> clazz) {
        this(configName, material, disguiseType, clazz, true);
    }

    private MorphType(String configName, XMaterial material, EntityType disguiseType, Class<? extends Morph> clazz, boolean doesSkillExist) {
        super(Category.MORPHS, configName, material, clazz);
        this.disguiseType = disguiseType;
        this.doesSkillExist = doesSkillExist;
        if (GENERATE_MISSING_MESSAGES) {
            MessageManager.addMessage(getConfigPath() + ".name", configName);
            if (doesSkillExist) {
                MessageManager.addMessage(getConfigPath() + ".skill", "Skill");
            }
        }
    }

    /**
     * Get the skill message.
     *
     * @return The skill message of the morph.
     */
    public Component getSkill() {
        return MessageManager.getMessage(getConfigPath() + ".skill");
    }

    public boolean canUseSkill() {
        return doesSkillExist && SettingsManager.getConfig().getBoolean(getConfigPath() + ".Skill-Enabled", true);
    }

    /**
     * Get the morph Disguise Type.
     *
     * @return disguise type
     */
    public EntityType getDisguiseType() {
        return disguiseType;
    }

    @Override
    public void setupConfig(CustomConfiguration config, String path) {
        super.setupConfig(config, path);
        if (doesSkillExist) {
            config.addDefault(path + ".Skill-Enabled", true, "Whether this morph's skill should be enabled.");
        }
        if (MorphNoFall.class.isAssignableFrom(getClazz())) {
            config.addDefault(path + ".No-Fall-Damage", true,
                    "Whether to disable fall damage while this morph is equipped.",
                    "(Normal use of the morph may cause fall damage otherwise.)");
        }
    }

    public static void register() {
        ServerVersion version = UltraCosmeticsData.get().getServerVersion();
        VersionManager vm = UltraCosmeticsData.get().getVersionManager();

        new MorphType("Bat", XMaterial.COAL, EntityType.BAT, MorphBat.class);
        new MorphType("Blaze", XMaterial.BLAZE_POWDER, EntityType.BLAZE, MorphBlaze.class);
        new MorphType("Chicken", XMaterial.EGG, EntityType.CHICKEN, MorphChicken.class);
        new MorphType("Pig", XMaterial.PORKCHOP, EntityType.PIG, MorphPig.class);
        new MorphType("Enderman", XMaterial.ENDER_PEARL, EntityType.ENDERMAN, MorphEnderman.class) {
            @Override
            public void setupConfig(CustomConfiguration config, String path) {
                super.setupConfig(config, path);
                config.addDefault(path + ".Mode", "Ray trace",
                        "Changes how the enderman morph teleports.",
                        "'Ray trace' (default) teleports a player to the block they're looking at,",
                        "up to 16 blocks away. Doesn't perform as well as the other options.",
                        "'Fast', teleports player 16 blocks in the direction they are looking,",
                        "while trying to avoid suffocation. Players will be able to teleport through walls.",
                        "'Enderpearl' simply launches an ender pearl in the direction the player is looking.");
            }
        };
        new MorphType("Slime", XMaterial.SLIME_BALL, EntityType.SLIME, MorphSlime.class);
        new MorphType("Creeper", XMaterial.GUNPOWDER, EntityType.CREEPER, MorphCreeper.class);
        new MorphType("Snowman", XMaterial.SNOWBALL, EntityType.SNOWMAN, MorphSnowman.class);
        new MorphType("Cow", XMaterial.MILK_BUCKET, EntityType.COW, MorphCow.class);
        new MorphType("Mooshroom", XMaterial.RED_MUSHROOM, EntityType.MUSHROOM_COW, MorphMooshroom.class);
        new MorphType("Villager", XMaterial.EMERALD, EntityType.VILLAGER, MorphVillager.class);
        new MorphType("Witch", XMaterial.POISONOUS_POTATO, EntityType.WITCH, MorphWitch.class);
        new MorphType("Sheep", XMaterial.WHITE_WOOL, EntityType.SHEEP, MorphSheep.class);
        new MorphType("CaveSpider", XMaterial.SPIDER_EYE, EntityType.CAVE_SPIDER, MorphCaveSpider.class, false);
        new MorphType("Endermite", XMaterial.ENDER_EYE, EntityType.ENDERMITE, MorphEndermite.class, false);
        new MorphType("Guardian", XMaterial.PRISMARINE_SHARD, EntityType.GUARDIAN, MorphGuardian.class, false);
        new MorphType("Wolf", XMaterial.BONE, EntityType.WOLF);
        new MorphType("IronGolem", XMaterial.IRON_INGOT, EntityType.IRON_GOLEM);
        new MorphType("Wither", XMaterial.WITHER_SKELETON_SKULL, EntityType.WITHER);
        new MorphType("Skeleton", XMaterial.BONE, EntityType.SKELETON);
        new MorphType("Zombie", XMaterial.ROTTEN_FLESH, EntityType.ZOMBIE);
        new MorphType("Squid", XMaterial.INK_SAC, EntityType.SQUID);
        new MorphType("Spider", XMaterial.STRING, EntityType.SPIDER);
        new MorphType("Ghast", XMaterial.GHAST_TEAR, EntityType.GHAST);
        new MorphType("MagmaCube", XMaterial.MAGMA_CREAM, EntityType.MAGMA_CUBE);
        new MorphType("Horse", XMaterial.SADDLE, EntityType.HORSE);

        switch (version) {
            case NEW:
            case v1_20:
                new MorphType("Sniffer", XMaterial.SNIFFER_EGG, EntityType.SNIFFER, MorphSniffer.class, false);
            case v1_19:
                new MorphType("Allay", XMaterial.ALLAY_SPAWN_EGG, EntityType.ALLAY, MorphAllay.class);
                new MorphType("Frog", XMaterial.FROG_SPAWN_EGG, EntityType.FROG, MorphFrog.class, false);
                new MorphType("Warden", XMaterial.SCULK_SHRIEKER, EntityType.WARDEN);
                new MorphType("Tadpole", XMaterial.TADPOLE_BUCKET, EntityType.TADPOLE);
            case v1_18:
                new MorphType("Goat", XMaterial.GOAT_HORN.or(XMaterial.WHEAT), EntityType.GOAT, MorphGoat.class, false);
            case v1_17:
                new MorphType("Axolotl", XMaterial.AXOLOTL_BUCKET, EntityType.AXOLOTL, MorphAxolotl.class);
                new MorphType("GlowSquid", XMaterial.GLOW_INK_SAC, EntityType.GLOW_SQUID, MorphGlowSquid.class, false);
            case v1_16:
                new MorphType("Piglin", XMaterial.GOLD_INGOT, EntityType.PIGLIN);
                new MorphType("Strider", XMaterial.WARPED_FUNGUS_ON_A_STICK, EntityType.STRIDER);
                new MorphType("Hoglin", XMaterial.COOKED_PORKCHOP, EntityType.HOGLIN);
                new MorphType("PiglinBrute", XMaterial.GOLDEN_AXE, EntityType.PIGLIN_BRUTE);
                new MorphType("Zoglin", XMaterial.ZOGLIN_SPAWN_EGG, EntityType.ZOGLIN);
                new MorphType("ZombifiedPiglin", XMaterial.GOLDEN_SWORD, EntityType.ZOMBIFIED_PIGLIN);
            case v1_15:
                new MorphType("Bee", XMaterial.HONEYCOMB, EntityType.BEE, MorphBee.class);
            case v1_14:
                new MorphType("Cat", XMaterial.TROPICAL_FISH, EntityType.CAT, MorphCat.class);
                new MorphType("Fox", XMaterial.SWEET_BERRIES, EntityType.FOX, MorphFox.class, false);
                new MorphType("Panda", XMaterial.BAMBOO, EntityType.PANDA);
                new MorphType("Ocelot", XMaterial.COD, EntityType.OCELOT);
                new MorphType("WanderingTrader", XMaterial.WANDERING_TRADER_SPAWN_EGG, EntityType.WANDERING_TRADER);
                new MorphType("Pillager", XMaterial.CROSSBOW, EntityType.PILLAGER);
                new MorphType("Ravager", XMaterial.RAVAGER_SPAWN_EGG, EntityType.RAVAGER);
            case v1_13:
                new MorphType("Cod", XMaterial.COD_BUCKET, EntityType.COD, MorphCod.class, false);
                new MorphType("Dolphin", XMaterial.DOLPHIN_SPAWN_EGG, EntityType.DOLPHIN, MorphDolphin.class, false);
                new MorphType("Drowned", XMaterial.TRIDENT, EntityType.DROWNED, MorphDrowned.class, false);
                new MorphType("Pufferfish", XMaterial.PUFFERFISH, EntityType.PUFFERFISH);
                new MorphType("Salmon", XMaterial.SALMON_BUCKET, EntityType.SALMON);
                new MorphType("TropicalFish", XMaterial.TROPICAL_FISH_BUCKET, EntityType.TROPICAL_FISH);
                new MorphType("Turtle", XMaterial.TURTLE_HELMET, EntityType.TURTLE);
                new MorphType("Phantom", XMaterial.PHANTOM_MEMBRANE, EntityType.PHANTOM);
            case v1_12:
                new MorphType("Parrot", XMaterial.COOKIE, EntityType.PARROT, MorphParrot.class);
                new MorphType("Illusioner", XMaterial.COMMAND_BLOCK, EntityType.ILLUSIONER);
            case v1_11:
                new MorphType("Llama", XMaterial.RED_WOOL, EntityType.LLAMA, MorphLlama.class);
                new MorphType("Evoker", XMaterial.TOTEM_OF_UNDYING, EntityType.EVOKER, MorphEvoker.class, false);
                new MorphType("WitherSkeleton", XMaterial.WITHER_SKELETON_SKULL, EntityType.WITHER_SKELETON, MorphWitherSkeleton.class);
                new MorphType("Donkey", XMaterial.CHEST, EntityType.DONKEY, MorphDonkey.class, false);
                new MorphType("Vex", XMaterial.IRON_SWORD, EntityType.VEX);
                new MorphType("Vindicator", XMaterial.IRON_AXE, EntityType.VINDICATOR);
                new MorphType("Mule", XMaterial.SADDLE, EntityType.MULE);
                new MorphType("SkeletonHorse", XMaterial.BONE_BLOCK, EntityType.SKELETON_HORSE);
                new MorphType("ZombieHorse", XMaterial.ZOMBIE_HORSE_SPAWN_EGG, EntityType.ZOMBIE_HORSE);
                if (vm.isUsingNMS()) {
                    new MorphType("ElderGuardian", XMaterial.PRISMARINE_CRYSTALS, EntityType.ELDER_GUARDIAN, UltraCosmeticsData.get().getVersionManager().getModule().getElderGuardianClass());
                }
            case v1_10:
                new MorphType("PolarBear", XMaterial.SNOW_BLOCK, EntityType.POLAR_BEAR, MorphPolarBear.class, vm.isUsingNMS());
            case v1_9:
                new MorphType("Shulker", XMaterial.SHULKER_BOX, EntityType.SHULKER);
            case v1_8:
                break;
        }
    }
}
