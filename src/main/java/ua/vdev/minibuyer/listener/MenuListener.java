package ua.vdev.minibuyer.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.vdev.minibuyer.config.MenuConfig;
import ua.vdev.minibuyer.manager.MenuManager;
import ua.vdev.minibuyer.manager.EconomyManager;
import ua.vdev.minibuyer.item.SellableItem;
import ua.vdev.minibuyer.menu.MenuHolder;
import ua.vdev.minibuyer.util.TextUtil;
import ua.vdev.minibuyer.config.SoundConfig;
import ua.vdev.minibuyer.config.model.SoundEffect;
import ua.vdev.minibuyer.config.model.Decoration;
import ua.vdev.minibuyer.manager.LevelManager;
import ua.vdev.minibuyer.menu.actions.ActionFactory;
import ua.vdev.minibuyer.config.model.StaticItem;

import java.util.List;
import java.util.Map;

public class MenuListener implements Listener {
    private final MenuManager menuManager;
    private final EconomyManager economyManager;
    private final LevelManager levelManager;
    private final String sellMessage;
    private final String sellAllMessage;
    private final String noItemsMessage;
    private final SoundConfig soundConfig;
    private final SoundEffect sellSoundEffect;
    private final SoundEffect noItemsSoundEffect;

    public MenuListener(MenuManager menuManager, EconomyManager economyManager, LevelManager levelManager) {
        this.menuManager = menuManager;
        this.economyManager = economyManager;
        this.levelManager = levelManager;

        this.sellMessage = menuManager.getPlugin().getConfig().getString("messages.sell", "<#E25822>✔ <gray>| <white>Вы продали <#E25822>{amount} {item} <white>за <#E25822>{price}$");
        this.sellAllMessage = menuManager.getPlugin().getConfig().getString("messages.sell-all", "<#E25822>✔ <gray>| <white>Вы продали <#E25822>{total} {item} <white>за <#E25822>{total_money}$");
        this.noItemsMessage = menuManager.getPlugin().getConfig().getString("messages.no-items", "<#E25822>✘ <gray>| <white>У вас нет этого предмета");
        this.soundConfig = new SoundConfig(menuManager.getPlugin());
        this.sellSoundEffect = soundConfig.getSellSound();
        this.noItemsSoundEffect = soundConfig.getNoItemsSound();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);

            if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
                return;
            }

            int slot = event.getRawSlot();
            String menuName = ((MenuHolder) event.getInventory().getHolder()).getMenuName();
            SellableItem sellable = getSellableItem(slot, menuName);
            if (sellable != null) {
                handleSellableItemClick(event, player, sellable, menuName);
                return;
            }

            Decoration decoration = getDecoration(slot, menuName);
            if (decoration != null) {
                handleDecorationClick(event, player, decoration);
                return;
            }
        }
    }

    private void handleSellableItemClick(InventoryClickEvent event, Player player, SellableItem sellable, String menuName) {
        switch (event.getClick()) {
            case LEFT:
                if (removeItem(player, sellable.material(), sellable.amount())) {
                    double priceWithMultiplier = sellable.price() * levelManager.getPriceMultiplier(player);
                    economyManager.deposit(player, priceWithMultiplier);
                    levelManager.addItemsSold(player, sellable.amount()).join();
                    sendFormattedMessage(player, sellMessage,
                            Map.of(
                                    "amount", String.valueOf(sellable.amount()),
                                    "item", sellable.name(),
                                    "price", String.valueOf((int) priceWithMultiplier)
                            ));
                    sellSoundEffect.play(player);
                } else {
                    player.sendMessage(TextUtil.mm(noItemsMessage, Map.of()));
                    noItemsSoundEffect.play(player);
                }
                break;
            case RIGHT:
                int total = removeAll(player, sellable.material());
                if (total > 0) {
                    double totalMoney = (sellable.price() * total / sellable.amount()) * levelManager.getPriceMultiplier(player);
                    economyManager.deposit(player, totalMoney);
                    levelManager.addItemsSold(player, total).join();
                    sendFormattedMessage(player, sellAllMessage,
                            Map.of(
                                    "total", String.valueOf(total),
                                    "item", sellable.name(),
                                    "total_money", String.valueOf((int) totalMoney)
                            ));
                    sellSoundEffect.play(player);
                } else {
                    player.sendMessage(TextUtil.mm(noItemsMessage, Map.of()));
                    noItemsSoundEffect.play(player);
                }
                break;
            default:
                break;
        }

        menuManager.updatePlayerMenu(player);
    }

    private void handleDecorationClick(InventoryClickEvent event, Player player, Decoration decoration) {
        List<String> commands = switch (event.getClick()) {
            case LEFT -> decoration.leftClickCommands();
            case RIGHT -> decoration.rightClickCommands();
            default -> List.of();
        };

        ActionFactory.createActions(commands).forEach(action -> action.execute(player));
    }

    private SellableItem getSellableItem(int slot, String menuName) {
        MenuConfig menuConfig = menuManager.getPlugin().getMenuLoader().getMenuConfig(menuName);
        if (menuConfig == null) return null;

        List<Integer> sellerSlots = menuConfig.getSellerItemSlots();
        int index = sellerSlots.indexOf(slot);
        if (index >= 0 && index < menuManager.getPlugin().getMenuLoader().getAssortment(menuName).size()) {
            return menuManager.getPlugin().getMenuLoader().getAssortment(menuName).get(index);
        }

        return menuConfig.getStaticItems().stream()
                .filter(staticItem -> staticItem.slot() == slot)
                .map(StaticItem::item)
                .findFirst()
                .orElse(null);
    }

    private Decoration getDecoration(int slot, String menuName) {
        MenuConfig menuConfig = menuManager.getPlugin().getMenuLoader().getMenuConfig(menuName);
        if (menuConfig == null) return null;

        return menuConfig.getDecorations().stream()
                .filter(deco -> deco.slots().contains(slot))
                .findFirst()
                .orElse(null);
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
            if (stack != null && stack.getType() == material && stack.getAmount() >= amount && !hasNbtTags(stack)) {
                stack.setAmount(stack.getAmount() - amount);
                if (stack.getAmount() == 0) contents[i] = null;
                player.getInventory().setContents(contents);
                return true;
            }
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.getType() == material && off.getAmount() >= amount && !hasNbtTags(off)) {
            off.setAmount(off.getAmount() - amount);
            if (off.getAmount() == 0) player.getInventory().setItemInOffHand(null);
            return true;
        }
        return false;
    }

    private int removeAll(Player player, Material material) {
        int total = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.getType() == material && !hasNbtTags(stack)) {
                total += stack.getAmount();
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);

        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.getType() == material && !hasNbtTags(off)) {
            total += off.getAmount();
            player.getInventory().setItemInOffHand(null);
        }
        return total;
    }

    private boolean hasNbtTags(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.hasEnchants() || meta.hasCustomModelData() || meta.hasAttributeModifiers() ||
                meta.hasDisplayName() || meta.hasLore();
    }
}