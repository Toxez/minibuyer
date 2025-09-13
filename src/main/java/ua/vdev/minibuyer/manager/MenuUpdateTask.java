package ua.vdev.minibuyer.manager;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import ua.vdev.minibuyer.menu.MenuHolder;

public class MenuUpdateTask extends BukkitRunnable {

    private final MenuManager menuManager;
    private final Player player;

    public MenuUpdateTask(MenuManager menuManager, Player player) {
        this.menuManager = menuManager;
        this.player = player;
    }

    @Override
    public void run() {
        if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
            menuManager.updatePlayerMenu(player);
        } else {
            menuManager.stopPlayerMenuUpdate(player);
            this.cancel();
        }
    }
}