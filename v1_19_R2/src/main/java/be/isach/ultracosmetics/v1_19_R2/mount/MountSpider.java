package be.isach.ultracosmetics.v1_19_R2.mount;

import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import be.isach.ultracosmetics.UltraCosmetics;
import be.isach.ultracosmetics.cosmetics.type.MountType;
import be.isach.ultracosmetics.player.UltraPlayer;
import be.isach.ultracosmetics.v1_19_R2.customentities.RideableSpider;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * @author RadBuilder
 */
public class MountSpider extends MountCustomEntity {
    public MountSpider(UltraPlayer owner, MountType type, UltraCosmetics ultraCosmetics) {
        super(owner, type, ultraCosmetics);
    }

    @Override
    public void onUpdate() {
    }

    @Override
    public LivingEntity getNewEntity() {
        return new RideableSpider(EntityType.SPIDER, ((CraftWorld) getPlayer().getWorld()).getHandle());
    }
}
