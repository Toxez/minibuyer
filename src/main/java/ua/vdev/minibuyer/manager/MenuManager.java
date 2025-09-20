package ua.vdev.minibuyer.manager;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ua.vdev.minibuyer.MiniBuyer;
import ua.vdev.minibuyer.config.ItemConfig;
import ua.vdev.minibuyer.config.MenuConfig;
import ua.vdev.minibuyer.item.SellableItem;
import ua.vdev.minibuyer.menu.MenuHolder;
import ua.vdev.minibuyer.util.ItemBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class MenuManager {
    private final MenuConfig menuConfig;
    private final ItemConfig itemConfig;
    private final EconomyManager economyManager;
    private final LevelManager levelManager;
    private final MiniBuyer plugin;
    private final Set<Player> openPlayers = ConcurrentHashMap.newKeySet();
    private final Map<Player, MenuUpdateTask> playerTasks = new HashMap<>();
    private final ExecutorService asyncExecutor;

    public MenuManager(MenuConfig menuConfig, ItemConfig itemConfig, EconomyManager economyManager, LevelManager levelManager) {
        this.menuConfig = menuConfig;
        this.itemConfig = itemConfig;
        this.economyManager = economyManager;
        this.levelManager = levelManager;
        this.plugin = MiniBuyer.getInstance();
        this.asyncExecutor = Executors.newFixedThreadPool(4);
    }

    public void openMenu(Player player, String menuName) {
        openPlayers.add(player);
        plugin.getMenuLoader().openMenu(player, menuName);
        startPlayerMenuUpdate(player);
    }

    public void refreshMenu(Player player) {
        if (plugin.getAssortmentUpdater() == null || levelManager == null) {
            return;
        }

        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder)) {
            stopPlayerMenuUpdate(player);
            return;
        }

        String menuName = ((MenuHolder) player.getOpenInventory().getTopInventory().getHolder()).getMenuName();
        plugin.getMenuLoader().openMenu(player, menuName);
    }

    public void refreshAllMenus() {
        openPlayers.removeIf(p -> !p.isOnline() || !(p.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder));
        openPlayers.forEach(this::updatePlayerMenu);
    }

    public void updatePlayerMenu(Player player) {
        if (!player.isOnline() || !(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder)) {
            stopPlayerMenuUpdate(player);
            return;
        }

        Inventory inv = player.getOpenInventory().getTopInventory();
        String menuName = ((MenuHolder) inv.getHolder()).getMenuName();
        MenuConfig menuConfig = plugin.getMenuLoader().getMenuConfig(menuName);
        List<SellableItem> currentAssortment = plugin.getMenuLoader().getAssortment(menuName);
        if (menuConfig == null) {
            stopPlayerMenuUpdate(player);
            return;
        }

        String updateTime = plugin.getAssortmentUpdater().getTimePlaceholder();

        CompletableFuture.allOf(
                levelManager.getPlayerLevelAsync(player),
                levelManager.getPriceMultiplierAsync(player),
                levelManager.getItemsRequiredForNextLevelAsync(player)
        ).thenAcceptAsync(v -> {
            int level = levelManager.getPlayerLevel(player);
            double multiplier = levelManager.getPriceMultiplier(player);
            String itemsRequired = levelManager.getItemsRequiredForNextLevel(player);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                inv.clear();

                List<Integer> slots = menuConfig.getSellerItemSlots();
                for (int i = 0; i < currentAssortment.size() && i < slots.size(); i++) {
                    SellableItem item = currentAssortment.get(i);
                    Map<String, String> itemPlaceholders = new HashMap<>();
                    itemPlaceholders.put("amount", String.valueOf(item.amount()));
                    itemPlaceholders.put("price", String.valueOf((int) (item.price() * multiplier)));
                    itemPlaceholders.put("update_time", updateTime);
                    itemPlaceholders.put("level", String.valueOf(level));
                    itemPlaceholders.put("boost", String.format("%.2f", multiplier));
                    itemPlaceholders.put("items_required", itemsRequired);

                    inv.setItem(slots.get(i), ItemBuilder.build(
                            item.material(),
                            item.amount(),
                            item.name(),
                            menuConfig.getItemLore(),
                            itemPlaceholders
                    ));
                }

                menuConfig.getDecorations().forEach(deco -> {
                    Map<String, String> decoPlaceholders = new HashMap<>();
                    decoPlaceholders.put("update_time", updateTime);
                    decoPlaceholders.put("level", String.valueOf(level));
                    decoPlaceholders.put("boost", String.format("%.2f", multiplier));
                    decoPlaceholders.put("items_required", itemsRequired);

                    deco.slots().forEach(slot -> inv.setItem(slot, ItemBuilder.build(
                            deco.material(),
                            1,
                            deco.name(),
                            deco.lore(),
                            decoPlaceholders
                    )));
                });

                menuConfig.getStaticItems().forEach(staticItem -> {
                    SellableItem item = staticItem.item();
                    Map<String, String> itemPlaceholders = new HashMap<>();
                    itemPlaceholders.put("amount", String.valueOf(item.amount()));
                    itemPlaceholders.put("price", String.valueOf((int) (item.price() * multiplier)));
                    itemPlaceholders.put("update_time", updateTime);
                    itemPlaceholders.put("level", String.valueOf(level));
                    itemPlaceholders.put("boost", String.format("%.2f", multiplier));
                    itemPlaceholders.put("items_required", itemsRequired);

                    inv.setItem(staticItem.slot(), ItemBuilder.build(
                            item.material(),
                            item.amount(),
                            item.name(),
                            staticItem.lore(),
                            itemPlaceholders
                    ));
                });
            });
        }, asyncExecutor);
    }

    public void startPlayerMenuUpdate(Player player) {
        stopPlayerMenuUpdate(player);
        if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
            MenuUpdateTask task = new MenuUpdateTask(this, player);
            task.runTaskTimer(plugin, 0L, 20L);
            playerTasks.put(player, task);
        }
    }

    public void stopPlayerMenuUpdate(Player player) {
        MenuUpdateTask task = playerTasks.remove(player);
        if (task != null) {
            task.cancel();
        }
        openPlayers.remove(player);
    }

    public void shutdown() {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
        }
    }
}