package ua.vdev.minibuyer.manager;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ua.vdev.minibuyer.MiniBuyer;
import ua.vdev.minibuyer.config.ItemConfig;
import ua.vdev.minibuyer.config.MenuConfig;
import ua.vdev.minibuyer.item.SellableItem;
import ua.vdev.minibuyer.menu.MainMenu;
import ua.vdev.minibuyer.menu.MenuHolder;
import ua.vdev.minibuyer.util.ItemBuilder;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;

@Getter
public class MenuManager {
    private final MenuConfig menuConfig;
    private final ItemConfig itemConfig;
    private final EconomyManager economyManager;
    private final MiniBuyer plugin;
    private List<SellableItem> currentAssortment;
    private final Set<Player> openPlayers = ConcurrentHashMap.newKeySet();

    private final Map<Player, MenuUpdateTask> playerTasks = new HashMap<>();

    public MenuManager(MenuConfig menuConfig, ItemConfig itemConfig, EconomyManager economyManager) {
        this.menuConfig = menuConfig;
        this.itemConfig = itemConfig;
        this.economyManager = economyManager;
        this.plugin = MiniBuyer.getInstance();
        this.currentAssortment = itemConfig.getItems();
    }

    public void openMenu(Player player) {
        openPlayers.add(player);
        refreshMenu(player);
        startPlayerMenuUpdate(player);
    }

    public void refreshMenu(Player player) {
        String updateTime = plugin.getAssortmentUpdater().getTimePlaceholder();
        new MainMenu(menuConfig, currentAssortment, updateTime).open(player);
    }

    public void refreshAllMenus() {
        openPlayers.removeIf(p -> !p.isOnline() || !p.getOpenInventory().getTitle().equals(menuConfig.getTitle()));
        openPlayers.forEach(this::updatePlayerMenu);
    }

    public void updateAssortment(List<SellableItem> newAssortment) {
        currentAssortment = newAssortment;
    }

    public void updatePlayerMenu(Player player) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!player.isOnline()) return;
        if (!(inv.getHolder() instanceof MenuHolder)) return;

        List<Integer> slots = menuConfig.getSellerItemSlots();
        String updateTime = plugin.getAssortmentUpdater().getTimePlaceholder();

        for (int i = 0; i < currentAssortment.size() && i < slots.size(); i++) {
            SellableItem item = currentAssortment.get(i);
            Map<String, String> itemPlaceholders = Map.of(
                    "amount", String.valueOf(item.getAmount()),
                    "price", String.valueOf(item.getPrice()),
                    "update_time", updateTime
            );
            inv.setItem(slots.get(i), ItemBuilder.build(
                    item.getMaterial(),
                    item.getAmount(),
                    item.getTranslation(),
                    menuConfig.getItemLore(),
                    itemPlaceholders
            ));
        }

        menuConfig.getDecorations().forEach(deco -> {
            inv.setItem(deco.getSlot(), ItemBuilder.build(
                    deco.getMaterial(),
                    1,
                    deco.getName(),
                    deco.getLore(),
                    Map.of("update_time", updateTime)
            ));
        });
    }

    public void startPlayerMenuUpdate(Player player) {
        stopPlayerMenuUpdate(player);
        MenuUpdateTask task = new MenuUpdateTask(this, player);
        task.runTaskTimer(plugin, 0L, 20L);
        playerTasks.put(player, task);
    }

    public void stopPlayerMenuUpdate(Player player) {
        MenuUpdateTask task = playerTasks.remove(player);
        if (task != null) {
            task.cancel();
        }
        openPlayers.remove(player);
    }
}