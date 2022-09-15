package be.isach.ultracosmetics.cosmetics.pets;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import me.gamercoder215.mobchip.ai.goal.CustomPathfinder;
import me.gamercoder215.mobchip.bukkit.BukkitBrain;

public class PetPathfinder extends CustomPathfinder {
    private final Player target;

    public PetPathfinder(Mob mob, Player target) {
        super(mob);
        this.target = target;
    }

    @Override
    public boolean canStart() {
        return true;
    }

    @Override
    public PathfinderFlag[] getFlags() {
        return new PathfinderFlag[] { PathfinderFlag.MOVEMENT, PathfinderFlag.LOOKING };
    }

    @Override
    public void start() {
        BukkitBrain.getBrain(entity).getController().moveTo(target);
    }

    @Override
    public void tick() {

    }

}
