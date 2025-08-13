package ua.vdev.minibuyer.manager;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Getter
public class EconomyManager {
    private Economy economy;

    public EconomyManager(JavaPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            economy = Bukkit.getServicesManager().load(Economy.class);
        }
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        economy.depositPlayer(player, amount);
        return true;
    }
}