package ua.vdev.minibuyer;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.HandlerList;
import ua.vdev.minibuyer.commands.AdminCommand;
import ua.vdev.minibuyer.commands.BuyerCommand;
import ua.vdev.minibuyer.config.ItemConfig;
import ua.vdev.minibuyer.config.MenuConfig;
import ua.vdev.minibuyer.config.SoundConfig;
import ua.vdev.minibuyer.manager.AssortmentUpdater;
import ua.vdev.minibuyer.manager.EconomyManager;
import ua.vdev.minibuyer.manager.LevelManager;
import ua.vdev.minibuyer.manager.MenuManager;
import ua.vdev.minibuyer.listener.MenuListener;
import ua.vdev.minibuyer.database.DatabaseManager;
import ua.vdev.minibuyer.menu.MenuHolder;
import ua.vdev.minibuyer.menu.MenuLoader;
import ua.vdev.minibuyer.util.MiniBuyerPlaceholder;

@Getter
public class MiniBuyer extends JavaPlugin {

    @Getter private static MiniBuyer instance;

    private ItemConfig itemConfig;
    private MenuConfig menuConfig;
    private EconomyManager economyManager;
    private MenuManager menuManager;
    private AssortmentUpdater assortmentUpdater;
    private DatabaseManager databaseManager;
    private LevelManager levelManager;
    private MenuListener menuListener;
    private MenuLoader menuLoader;
    private SoundConfig soundConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadPlugin();
    }

    public void reloadPlugin() {
        if (assortmentUpdater != null) {
            assortmentUpdater.stop();
        }
        if (levelManager != null) {
            levelManager.closeConnection();
        }
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        if (menuListener != null) {
            HandlerList.unregisterAll(menuListener);
        }

        reloadConfig();

        databaseManager = new DatabaseManager(this);
        levelManager = new LevelManager(this);
        itemConfig = new ItemConfig(this);
        menuLoader = new MenuLoader(this);
        String mainMenuName = getConfig().getString("main-menu", "main-menu");
        menuConfig = menuLoader.getMenuConfig(mainMenuName);
        if (menuConfig == null) {
            getLogger().warning("Основное меню '" + mainMenuName + "' не найдено будет используеься первый доступный файл");
            menuConfig = menuLoader.getMenuNames().stream().findFirst()
                    .map(menuLoader::getMenuConfig)
                    .orElse(null);
        }
        economyManager = new EconomyManager(this);
        soundConfig = new SoundConfig(this);
        menuManager = new MenuManager(menuConfig, itemConfig, economyManager, levelManager);
        assortmentUpdater = new AssortmentUpdater(this, menuManager);

        menuListener = new MenuListener(menuManager, economyManager, levelManager);
        getServer().getPluginManager().registerEvents(menuListener, this);

        AdminCommand adminCommand = new AdminCommand(this);
        getCommand("minibuyer").setExecutor(adminCommand);
        getCommand("minibuyer").setTabCompleter(adminCommand);

        getCommand("buyer").setExecutor(new BuyerCommand(menuManager));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MiniBuyerPlaceholder(this).register();
        }

        assortmentUpdater.start();

        getServer().getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                menuManager.updatePlayerMenu(player);
            }
        });
    }

    @Override
    public void onDisable() {
        if (assortmentUpdater != null) {
            assortmentUpdater.stop();
        }
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        if (levelManager != null) {
            levelManager.closeConnection();
        }
        if (menuListener != null) {
            HandlerList.unregisterAll(menuListener);
        }
    }
}