package ua.vdev.minibuyer.listener;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import ua.vdev.minibuyer.manager.MenuManager;
import ua.vdev.minibuyer.manager.EconomyManager;
import ua.vdev.minibuyer.item.SellableItem;
import ua.vdev.minibuyer.menu.MenuHolder;
import ua.vdev.minibuyer.util.TextUtil;
import ua.vdev.minibuyer.config.SoundConfig;

import java.util.List;
import java.util.Map;

public class MenuListener implements Listener {
    private final MenuManager menuManager;
    private final EconomyManager economyManager;
    private final String sellMessage;
    private final String sellAllMessage;
    private final String noItemsMessage;
    private final SoundConfig soundConfig;

    public MenuListener(MenuManager menuManager, EconomyManager economyManager) {
        this.menuManager = menuManager;
        this.economyManager = economyManager;

        this.sellMessage = menuManager.getPlugin().getConfig().getString("messages.sell", "&aВы продали {amount} {item} за &e{price}$");
        this.sellAllMessage = menuManager.getPlugin().getConfig().getString("messages.sell-all", "&aВы продали {total} {item} за &e{total_money}$");
        this.noItemsMessage = menuManager.getPlugin().getConfig().getString("messages.no-items", "&cУ вас нет этого предмета!");
        this.soundConfig = new SoundConfig(menuManager.getPlugin());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);

            if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
                return;
            }

            List<Integer> sellerSlots = menuManager.getMenuConfig().getSellerItemSlots();
            int slot = event.getRawSlot();

            if (!sellerSlots.contains(slot)) return;

            int index = sellerSlots.indexOf(slot);
            if (index >= menuManager.getCurrentAssortment().size()) return;
            SellableItem sellable = menuManager.getCurrentAssortment().get(index);

            switch (event.getClick()) {
                case LEFT:
                case SHIFT_LEFT:
                    if (removeItem(player, sellable.getMaterial(), sellable.getAmount())) {
                        economyManager.deposit(player, sellable.getPrice());
                        sendFormattedMessage(player, sellMessage,
                                Map.of(
                                        "amount", String.valueOf(sellable.getAmount()),
                                        "item", sellable.getTranslation(),
                                        "price", String.valueOf(sellable.getPrice())
                                ));
                        player.playSound(player.getLocation(), soundConfig.getSellSound(), 1.0F, 1.0F);
                    } else {
                        player.sendMessage(TextUtil.mm(noItemsMessage, Map.of()));
                        player.playSound(player.getLocation(), soundConfig.getNoItemsSound(), 1.0F, 1.0F);
                    }
                    break;
                case RIGHT:
                case SHIFT_RIGHT:
                    int total = removeAll(player, sellable.getMaterial());
                    if (total > 0) {
                        int totalMoney = sellable.getPrice() * total / sellable.getAmount();
                        economyManager.deposit(player, totalMoney);
                        sendFormattedMessage(player, sellAllMessage,
                                Map.of(
                                        "total", String.valueOf(total),
                                        "item", sellable.getTranslation(),
                                        "total_money", String.valueOf(totalMoney))
                        );
                        player.playSound(player.getLocation(), soundConfig.getSellSound(), 1.0F, 1.0F);
                    } else {
                        player.sendMessage(TextUtil.mm(noItemsMessage, Map.of()));
                        player.playSound(player.getLocation(), soundConfig.getNoItemsSound(), 1.0F, 1.0F);
                    }
                    break;
                default:
                    break;
            }

            menuManager.updatePlayerMenu(player);
        }
    }

    private void sendFormattedMessage(Player player, String message, Map<String, String> placeholders) {
        player.sendMessage(TextUtil.mm(message, placeholders));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            menuManager.stopPlayerMenuUpdate(player);
        }
    }

    private boolean removeItem(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == material && stack.getAmount() >= amount) {
                stack.setAmount(stack.getAmount() - amount);
                if (stack.getAmount() == 0) contents[i] = null;
                player.getInventory().setContents(contents);
                return true;
            }
        }
        if (player.getInventory().getItemInOffHand().getType() == material && player.getInventory().getItemInOffHand().getAmount() >= amount) {
            ItemStack off = player.getInventory().getItemInOffHand();
            off.setAmount(off.getAmount() - amount);
            if (off.getAmount() == 0) player.getInventory().setItemInOffHand(null);
            return true;
        }
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() == material && armor.getAmount() >= amount) {
                armor.setAmount(armor.getAmount() - amount);
                if (armor.getAmount() == 0) armor.setType(Material.AIR);
                player.getInventory().setArmorContents(player.getInventory().getArmorContents());
                return true;
            }
        }
        return false;
    }

    private int removeAll(Player player, Material material) {
        int total = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);

        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == material) {
            total += off.getAmount();
            player.getInventory().setItemInOffHand(null);
        }
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece != null && piece.getType() == material) {
                total += piece.getAmount();
                armor[i] = null;
            }
        }
        player.getInventory().setArmorContents(armor);

        return total;
    }
}