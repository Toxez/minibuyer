package ua.vdev.minibuyer;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import ua.vdev.minibuyer.config.ItemConfig;
import ua.vdev.minibuyer.config.MenuConfig;
import ua.vdev.minibuyer.manager.AssortmentUpdater;
import ua.vdev.minibuyer.manager.EconomyManager;
import ua.vdev.minibuyer.manager.MenuManager;
import ua.vdev.minibuyer.listener.MenuListener;

@Getter
public class MiniBuyer extends JavaPlugin {
    @Getter private static MiniBuyer instance;
    private ItemConfig itemConfig;
    private MenuConfig menuConfig;
    private EconomyManager economyManager;
    private MenuManager menuManager;
    private AssortmentUpdater assortmentUpdater;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        itemConfig = new ItemConfig(this);
        menuConfig = new MenuConfig(this);
        economyManager = new EconomyManager(this);

        menuManager = new MenuManager(menuConfig, itemConfig, economyManager);
        assortmentUpdater = new AssortmentUpdater(this, menuManager);

        getServer().getPluginManager().registerEvents(new MenuListener(menuManager, economyManager), this);

        getCommand("buyer").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                menuManager.openMenu(player);
                return true;
            }
            sender.sendMessage("Эта команда для игроков");
            return false;
        });

        assortmentUpdater.start();
    }

    @Override
    public void onDisable() {
        assortmentUpdater.stop();
    }
}