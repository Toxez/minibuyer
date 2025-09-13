package ua.vdev.minibuyer.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MenuHolder implements InventoryHolder {
    private final String menuName;

    public MenuHolder(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuName() {
        return menuName;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}