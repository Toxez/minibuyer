package ua.vdev.minibuyer.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ua.vdev.minibuyer.config.MenuConfig;
import ua.vdev.minibuyer.item.SellableItem;
import ua.vdev.minibuyer.util.ItemBuilder;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class MainMenu {
    private final MenuConfig menuConfig;
    private final List<SellableItem> items;
    private final String updateTime;

    public void open(Player player) {
        Map<String, String> menuPlaceholders = Map.of("update_time", updateTime);

        Inventory inv = Bukkit.createInventory(new MenuHolder(), menuConfig.getSize(),
                ua.vdev.minibuyer.util.TextUtil.mm(menuConfig.getTitle(), menuPlaceholders)
        );
        updateInventory(inv);
        player.openInventory(inv);
    }

    public void update(Player player) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        updateInventory(inv);
    }

    private void updateInventory(Inventory inv) {
        Map<String, String> menuPlaceholders = Map.of("update_time", updateTime);
        List<Integer> slots = menuConfig.getSellerItemSlots();
        for (int i = 0; i < items.size() && i < slots.size(); i++) {
            SellableItem item = items.get(i);
            Map<String, String> itemPlaceholders = Map.of(
                    "amount", String.valueOf(item.getAmount()),
                    "price", String.valueOf(item.getPrice()),
                    "update_time", updateTime
            );
            ItemStack stack = ItemBuilder.build(item.getMaterial(), item.getAmount(), item.getTranslation(), menuConfig.getItemLore(), itemPlaceholders);
            inv.setItem(slots.get(i), stack);
        }
        menuConfig.getDecorations().forEach(deco -> {
            ItemStack stack = ItemBuilder.build(
                    deco.getMaterial(),
                    1,
                    deco.getName(),
                    deco.getLore(),
                    menuPlaceholders
            );
            inv.setItem(deco.getSlot(), stack);
        });
    }
}