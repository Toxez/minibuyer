package ua.vdev.minibuyer.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ua.vdev.minibuyer.MiniBuyer;
import ua.vdev.minibuyer.config.MenuConfig;
import ua.vdev.minibuyer.item.SellableItem;
import ua.vdev.minibuyer.config.model.StaticItem;

import java.io.File;
import java.util.*;

public class MenuLoader {
    private final JavaPlugin plugin;
    private final Map<String, MenuConfig> menus;
    private final Map<String, List<SellableItem>> menuAssortments;

    public MenuLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.menus = new HashMap<>();
        this.menuAssortments = new HashMap<>();
        loadMenus();
    }

    private void loadMenus() {
        File menuFolder = new File(plugin.getDataFolder(), "menu");
        if (!menuFolder.exists()) {
            menuFolder.mkdirs();
            plugin.saveResource("menu/main-menu.yml", false);
            plugin.saveResource("menu/test-menu.yml", false);
        }

        File[] menuFiles = menuFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (menuFiles != null) {
            for (File file : menuFiles) {
                String menuName = file.getName().replace(".yml", "");
                MenuConfig menuConfig = new MenuConfig(plugin, file.getName());
                menus.put(menuName, menuConfig);

                List<SellableItem> staticItems = menuConfig.getStaticItems().stream()
                        .map(StaticItem::item)
                        .toList();
                if (!staticItems.isEmpty()) {
                    menuAssortments.put(menuName, new ArrayList<>(staticItems));
                } else {
                    List<SellableItem> allItems = new ArrayList<>(MiniBuyer.getInstance().getItemConfig().getItems());
                    Collections.shuffle(allItems);
                    int max = Math.min(menuConfig.getSellerItemSlots().size(), allItems.size());
                    menuAssortments.put(menuName, allItems.stream().limit(max).toList());
                }
            }
        }
    }

    public void openMenu(Player player, String menuName) {
        MenuConfig menuConfig = menus.get(menuName);
        if (menuConfig == null) {
            Bukkit.getLogger().warning("Меню " + menuName + " не найдено");
            return;
        }

        List<SellableItem> assortment = menuAssortments.getOrDefault(menuName, new ArrayList<>());
        String updateTime = MiniBuyer.getInstance().getAssortmentUpdater() != null
                ? MiniBuyer.getInstance().getAssortmentUpdater().getTimePlaceholder()
                : "00:00:00";
        int level = MiniBuyer.getInstance().getLevelManager() != null
                ? MiniBuyer.getInstance().getLevelManager().getPlayerLevel(player)
                : 0;
        double multiplier = MiniBuyer.getInstance().getLevelManager() != null
                ? MiniBuyer.getInstance().getLevelManager().getPriceMultiplier(player)
                : 1.0;
        String itemsRequired = MiniBuyer.getInstance().getLevelManager() != null
                ? MiniBuyer.getInstance().getLevelManager().getItemsRequiredForNextLevel(player)
                : "0";

        new MainMenu(menuConfig, assortment, updateTime, level, multiplier, itemsRequired, menuName).open(player);
    }

    public MenuConfig getMenuConfig(String menuName) {
        return menus.get(menuName);
    }

    public List<SellableItem> getAssortment(String menuName) {
        return menuAssortments.getOrDefault(menuName, new ArrayList<>());
    }

    public void updateAssortment(String menuName, List<SellableItem> newAssortment) {
        menuAssortments.put(menuName, newAssortment);
        if (MiniBuyer.getInstance().getMenuManager() != null) {
            MiniBuyer.getInstance().getMenuManager().refreshAllMenus();
        }
    }

    public List<String> getMenuNames() {
        return new ArrayList<>(menus.keySet());
    }
}