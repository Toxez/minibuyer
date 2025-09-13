package ua.vdev.minibuyer.menu;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ua.vdev.minibuyer.config.MenuConfig;
import ua.vdev.minibuyer.item.SellableItem;
import ua.vdev.minibuyer.util.ItemBuilder;
import ua.vdev.minibuyer.util.TextUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class MainMenu {
    private final MenuConfig menuConfig;
    private final List<SellableItem> items;
    private final String updateTime;
    private final int level;
    private final double multiplier;
    private final String itemsRequired;
    private final String menuName;

    public void open(Player player) {
        Map<String, String> menuPlaceholders = new HashMap<>();
        menuPlaceholders.put("update_time", updateTime);
        menuPlaceholders.put("level", String.valueOf(level));
        menuPlaceholders.put("boost", String.format("%.2f", multiplier));
        menuPlaceholders.put("items_required", itemsRequired);

        Inventory inv = Bukkit.createInventory(
                new MenuHolder(menuName),
                menuConfig.getSize(),
                TextUtil.mm(menuConfig.getTitle(), menuPlaceholders)
        );
        updateInventory(inv);
        player.openInventory(inv);
    }

    public void update(Player player) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (inv != null && inv.getHolder() instanceof MenuHolder) {
            updateInventory(inv);
        }
    }

    private void updateInventory(Inventory inv) {
        Map<String, String> menuPlaceholders = new HashMap<>();
        menuPlaceholders.put("update_time", updateTime);
        menuPlaceholders.put("level", String.valueOf(level));
        menuPlaceholders.put("boost", String.format("%.2f", multiplier));
        menuPlaceholders.put("items_required", itemsRequired);

        inv.clear();

        List<Integer> slots = menuConfig.getSellerItemSlots();
        for (int i = 0; i < items.size() && i < slots.size(); i++) {
            SellableItem item = items.get(i);

            Map<String, String> itemPlaceholders = new HashMap<>();
            itemPlaceholders.put("amount", String.valueOf(item.amount()));
            itemPlaceholders.put("price", String.valueOf((int) (item.price() * multiplier)));
            itemPlaceholders.put("update_time", updateTime);
            itemPlaceholders.put("level", String.valueOf(level));
            itemPlaceholders.put("boost", String.format("%.2f", multiplier));
            itemPlaceholders.put("items_required", itemsRequired);

            ItemStack stack = ItemBuilder.build(
                    item.material(),
                    item.amount(),
                    item.name(),
                    menuConfig.getItemLore(),
                    itemPlaceholders
            );
            inv.setItem(slots.get(i), stack);
        }

        menuConfig.getDecorations().forEach(deco -> {
            Map<String, String> menuPlaceholdersForDeco = new HashMap<>(menuPlaceholders);
            ItemStack stack = ItemBuilder.build(
                    deco.material(),
                    1,
                    deco.name(),
                    deco.lore(),
                    menuPlaceholdersForDeco
            );
            deco.slots().forEach(slot -> inv.setItem(slot, stack));
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

            ItemStack stack = ItemBuilder.build(
                    item.material(),
                    item.amount(),
                    item.name(),
                    staticItem.lore(),
                    itemPlaceholders
            );
            inv.setItem(staticItem.slot(), stack);
        });
    }
}