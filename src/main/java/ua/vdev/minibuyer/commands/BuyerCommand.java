package ua.vdev.minibuyer.commands;

import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ua.vdev.minibuyer.MiniBuyer;
import ua.vdev.minibuyer.manager.MenuManager;
import ua.vdev.minibuyer.util.TextUtil;

@Getter
public class BuyerCommand implements CommandExecutor {
    private final MenuManager menuManager;

    public BuyerCommand(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.mm("<red>Эта команда только для игроков", null));
            return true;
        }
        String mainMenu = MiniBuyer.getInstance().getConfig().getString("main-menu", "main-menu");
        menuManager.openMenu(player, mainMenu);
        return true;
    }
}