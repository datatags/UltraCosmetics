package be.isach.ultracosmetics.economy;

import com.github.SoKnight.PEconomy.API.PEconomyAPI;
import org.bukkit.entity.Player;

public class PEconomy implements EconomyManager {

    private PEconomyAPI pEconomyAPI;

    public PEconomy() {
        this.pEconomyAPI = PEconomyAPI.getPEconomyAPI();
    }

    @Override
    public double getBalance(Player player) {
        return pEconomyAPI.getBalance(player.getUniqueId());
    }

    @Override
    public boolean has(Player player, double amount) {
        return pEconomyAPI.getBalance(player.getUniqueId()) >= amount;
    }

    @Override
    public void withdraw(Player player, double amount) {
        pEconomyAPI.withdraw(player.getUniqueId(), amount);
    }

    @Override
    public void deposit(Player player, double amount) {
        pEconomyAPI.deposit(player.getUniqueId(), amount);
    }
}
