package be.isach.ultracosmetics.v1_12_R1.pets;

import be.isach.ultracosmetics.cosmetics.pets.APlayerFollower;
import be.isach.ultracosmetics.cosmetics.pets.Pet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityInsentient;
import net.minecraft.server.v1_12_R1.PathEntity;

/**
 * @author RadBuilder
 */
public class PlayerFollower extends APlayerFollower {

    public PlayerFollower(Pet pet, Player player) {
        super(pet, player);
    }

    @Override
    public void follow() {
        Entity petEntity;

        if (pet instanceof CustomEntityPet) {
            petEntity = ((CustomEntityPet) pet).getNMSEntity();
        } else {
            petEntity = ((CraftEntity) pet.getEntity()).getHandle();
        }

        if (!player.getWorld().equals(petEntity.getBukkitEntity().getWorld())) {
            petEntity.getBukkitEntity().teleport(player.getLocation());
            return;
        }

        ((EntityInsentient) petEntity).getNavigation().a(2);
        Location targetLocation = player.getLocation();
        PathEntity path = ((EntityInsentient) petEntity).getNavigation().a(targetLocation.getX() + 1, targetLocation.getY(), targetLocation.getZ() + 1);

        try {
            int distance = (int) Bukkit.getPlayer(player.getName()).getLocation().distance(petEntity.getBukkitEntity().getLocation());

            if (distance > 10 && petEntity.valid && player.isOnGround()) {
                petEntity.setLocation(targetLocation.getX(), targetLocation.getY(), targetLocation.getZ(), 0, 0);
            }

            if (path != null && distance > 3.3) {
                double speed = 1.05d;

                if (pet.getType().getEntityType() == EntityType.ZOMBIE) {
                    speed *= 1.5;
                }

                ((EntityInsentient) petEntity).getNavigation().a(path, speed);
                ((EntityInsentient) petEntity).getNavigation().a(speed);
            }
        } catch (IllegalArgumentException exception) {
            petEntity.setLocation(targetLocation.getX(), targetLocation.getY(), targetLocation.getZ(), 0, 0);
            exception.printStackTrace();
        }
    }
}
