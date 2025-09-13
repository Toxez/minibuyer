package ua.vdev.minibuyer.menu.actions.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ua.vdev.minibuyer.MiniBuyer;
import ua.vdev.minibuyer.config.MenuConfig;
import ua.vdev.minibuyer.menu.MenuHolder;
import ua.vdev.minibuyer.menu.MenuLoader;
import ua.vdev.minibuyer.menu.actions.MenuAction;

public class OpenMenuAction implements MenuAction {
    private final String menuName;

    public OpenMenuAction(String menuName) {
        this.menuName = menuName;
    }

    @Override
    public void execute(Player player) {
        MenuLoader menuLoader = MiniBuyer.getInstance().getMenuLoader();
        MenuConfig menuConfig = menuLoader.getMenuConfig(menuName);
        if (menuConfig == null) {
            Bukkit.getLogger().warning("Меню " + menuName + " не найдено");
            return;
        }

        Inventory currentInventory = player.getOpenInventory().getTopInventory();
        if (currentInventory.getHolder() instanceof MenuHolder currentHolder) {
            String currentMenuName = currentHolder.getMenuName();
            if (currentMenuName.equals(menuName)) {
                MiniBuyer.getInstance().getMenuManager().updatePlayerMenu(player);
                return;
            }
        }

        menuLoader.openMenu(player, menuName);
    }
}