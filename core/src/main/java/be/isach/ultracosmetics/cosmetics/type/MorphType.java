package be.isach.ultracosmetics.cosmetics.type;

import be.isach.ultracosmetics.UltraCosmeticsData;
import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.cosmetics.Category;
import be.isach.ultracosmetics.cosmetics.morphs.Morph;
import be.isach.ultracosmetics.cosmetics.morphs.MorphBat;
import be.isach.ultracosmetics.cosmetics.morphs.MorphBlaze;
import be.isach.ultracosmetics.cosmetics.morphs.MorphChicken;
import be.isach.ultracosmetics.cosmetics.morphs.MorphCow;
import be.isach.ultracosmetics.cosmetics.morphs.MorphCreeper;
import be.isach.ultracosmetics.cosmetics.morphs.MorphEnderman;
import be.isach.ultracosmetics.cosmetics.morphs.MorphLlama;
import be.isach.ultracosmetics.cosmetics.morphs.MorphMooshroom;
import be.isach.ultracosmetics.cosmetics.morphs.MorphParrot;
import be.isach.ultracosmetics.cosmetics.morphs.MorphPig;
import be.isach.ultracosmetics.cosmetics.morphs.MorphPolarBear;
import be.isach.ultracosmetics.cosmetics.morphs.MorphSheep;
import be.isach.ultracosmetics.cosmetics.morphs.MorphSlime;
import be.isach.ultracosmetics.cosmetics.morphs.MorphSnowman;
import be.isach.ultracosmetics.cosmetics.morphs.MorphVillager;
import be.isach.ultracosmetics.cosmetics.morphs.MorphWitch;
import be.isach.ultracosmetics.cosmetics.morphs.MorphWitherSkeleton;
import be.isach.ultracosmetics.util.ServerVersion;

import com.cryptomorin.xseries.XMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.libraryaddict.disguise.disguisetypes.DisguiseType;

/**
 * Morph types.
 *
 * @author iSach
 * @since 12-19-2015
 */
public class MorphType extends CosmeticType<Morph> {

    private final static List<MorphType> ENABLED = new ArrayList<>();
    private final static List<MorphType> VALUES = new ArrayList<>();

    public static List<MorphType> enabled() {
        return ENABLED;
    }

    public static List<MorphType> values() {
        return VALUES;
    }

    public static MorphType valueOf(String s) {
        for (MorphType morphType : VALUES) {
            if (morphType.getConfigName().equalsIgnoreCase(s)) return morphType;
        }
        return null;
    }

    public static void checkEnabled() {
        ENABLED.addAll(values().stream().filter(CosmeticType::isEnabled).collect(Collectors.toList()));
    }

    /**
     * Disguise Type of the morph.
     */
    private final DisguiseType disguiseType;

    private MorphType(String configName, XMaterial material, DisguiseType disguiseType, Class<? extends Morph> clazz) {
        super(Category.MORPHS, configName, material, clazz);
        this.disguiseType = disguiseType;

        VALUES.add(this);
    }

    /**
     * Get the skill message.
     *
     * @return The skill message of the morph.
     */
    public String getSkill() {
        return MessageManager.getMessage("Morphs." + getConfigName() + ".skill");
    }

    /**
     * Get the morph Disguise Type.
     *
     * @return disguise type
     */
    public DisguiseType getDisguiseType() {
        return disguiseType;
    }

    public static void register() {
        ServerVersion version = UltraCosmeticsData.get().getServerVersion();

        new MorphType("Bat", XMaterial.COAL, DisguiseType.BAT, MorphBat.class);
        new MorphType("Blaze", XMaterial.BLAZE_POWDER, DisguiseType.BLAZE, MorphBlaze.class);
        new MorphType("Chicken", XMaterial.EGG, DisguiseType.CHICKEN, MorphChicken.class);
        new MorphType("Pig", XMaterial.PORKCHOP, DisguiseType.PIG, MorphPig.class);
        new MorphType("Enderman", XMaterial.ENDER_PEARL, DisguiseType.ENDERMAN, MorphEnderman.class);
        new MorphType("Slime", XMaterial.SLIME_BALL, DisguiseType.SLIME, MorphSlime.class);
        new MorphType("Creeper", XMaterial.GUNPOWDER, DisguiseType.CREEPER, MorphCreeper.class);
        new MorphType("Snowman", XMaterial.SNOWBALL, DisguiseType.SNOWMAN, MorphSnowman.class);
        new MorphType("ElderGuardian", XMaterial.PRISMARINE_CRYSTALS, DisguiseType.ELDER_GUARDIAN, UltraCosmeticsData.get().getVersionManager().getMorphs().getElderGuardianClass());
        new MorphType("Cow", XMaterial.MILK_BUCKET, DisguiseType.COW, MorphCow.class);
        new MorphType("Mooshroom", XMaterial.RED_MUSHROOM, DisguiseType.MUSHROOM_COW, MorphMooshroom.class);
        new MorphType("Villager", XMaterial.EMERALD, DisguiseType.VILLAGER, MorphVillager.class);
        new MorphType("Witch", XMaterial.POISONOUS_POTATO, DisguiseType.WITCH, MorphWitch.class);

        if (version.isAtLeast(ServerVersion.v1_10)) {
            new MorphType("PolarBear", XMaterial.SNOW_BLOCK, DisguiseType.POLAR_BEAR, MorphPolarBear.class);
        }

        if (version.isAtLeast(ServerVersion.v1_11)) {
            new MorphType("Llama", XMaterial.RED_WOOL, DisguiseType.LLAMA, MorphLlama.class);
        }

        if (version.isAtLeast(ServerVersion.v1_12)) {
            new MorphType("Parrot", XMaterial.COOKIE, DisguiseType.PARROT, MorphParrot.class);
        }

        new MorphType("Sheep", XMaterial.WHITE_WOOL, DisguiseType.SHEEP, MorphSheep.class);
        new MorphType("WitherSkeleton", XMaterial.WITHER_SKELETON_SKULL, DisguiseType.WITHER_SKELETON, MorphWitherSkeleton.class);
    }
}
