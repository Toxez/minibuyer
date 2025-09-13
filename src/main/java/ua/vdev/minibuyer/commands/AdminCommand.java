package ua.vdev.minibuyer.commands;

import lombok.Getter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import ua.vdev.minibuyer.MiniBuyer;
import ua.vdev.minibuyer.commands.types.CommandType;
import ua.vdev.minibuyer.util.TextUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class AdminCommand implements CommandExecutor, TabCompleter {
    private final MiniBuyer plugin;

    public AdminCommand(MiniBuyer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minibuyer.admin")) {
            sender.sendMessage(TextUtil.mm(plugin.getConfig().getString("messages.no_permission", "<red>У вас нет прав"), null));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        CommandType type = CommandType.fromString(args[0]);
        if (type == null) {
            sendUsage(sender);
            return true;
        }

        switch (type) {
            case RELOAD -> {
                long start = System.currentTimeMillis();
                plugin.reloadPlugin();
                sender.sendMessage(TextUtil.mm(plugin.getConfig().getString("messages.reload_success", "<green>Плагин перезагружен за {time} мс")
                        .replace("{time}", String.valueOf(System.currentTimeMillis() - start)), null));
                return true;
            }
            case LEVEL -> { return handleLevel(sender, args); }
            case ITEMS -> { return handleItems(sender, args); }
            default -> {
                sendUsage(sender);
                return true;
            }
        }
    }

    private boolean handleLevel(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("set")) {
            sendUsage(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(TextUtil.mm(plugin.getConfig().getString("messages.player_not_found", "<red>Игрок {player} не найден")
                    .replace("{player}", args[2]), null));
            return true;
        }

        try {
            int level = Integer.parseInt(args[3]);
            if (!plugin.getLevelManager().getLevels().containsKey(level)) {
                int maxLevel = plugin.getLevelManager().getLevels().keySet().stream().max(Integer::compare).orElse(1);
                sender.sendMessage(TextUtil.mm(plugin.getConfig().getString("messages.invalid_level", "<red>Уровень {level} не существует! Максимум: {max_level}")
                        .replace("{level}", args[3]).replace("{max_level}", String.valueOf(maxLevel)), null));
                return true;
            }

            plugin.getLevelManager().setPlayerLevel(target, level);
            sender.sendMessage(TextUtil.mm(plugin.getConfig().getString("messages.level_set", "<green>Уровень {player} установлен на {level}")
                    .replace("{player}", target.getName()).replace("{level}", args[3]), null));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(TextUtil.mm(plugin.getConfig().getString("messages.invalid_number", "<red>Уровень должен быть числом"), null));
            return true;
        }
    }

    private boolean handleItems(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("add")) {
            sendUsage(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(TextUtil.mm(plugin.getConfig().getString("messages.player_not_found", "<red>Игрок {player} не найден!")
                    .replace("{player}", args[2]), null));
            return true;
        }

        try {
            int amount = Integer.parseInt(args[3]);
            if (amount <= 0) {
                sender.sendMessage(TextUtil.mm(plugin.getConfig().getString("messages.invalid_number", "<red>Количество должно быть положительным!"), null));
                return true;
            }

            plugin.getLevelManager().addItemsSold(target, amount);
            sender.sendMessage(TextUtil.mm(plugin.getConfig().getString("messages.items_added", "<green>Добавлено {amount} предметов игроку {player}!")
                    .replace("{player}", target.getName()).replace("{amount}", args[3]), null));
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage(TextUtil.mm(plugin.getConfig().getString("messages.invalid_number", "<red>Количество должно быть числом!"), null));
            return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        plugin.getConfig().getStringList("messages.admin_usage").forEach(msg -> sender.sendMessage(TextUtil.mm(msg, null)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("minibuyer.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.stream(CommandType.values()).map(CommandType::getCommand)
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("level") || args[0].equalsIgnoreCase("items"))) {
            return List.of(args[0].equalsIgnoreCase("level") ? "set" : "add");
        }
        if (args.length == 3 && ((args[0].equalsIgnoreCase("level") && args[1].equalsIgnoreCase("set")) ||
                (args[0].equalsIgnoreCase("items") && args[1].equalsIgnoreCase("add")))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("level") && args[1].equalsIgnoreCase("set")) {
            return plugin.getLevelManager().getLevels().keySet().stream().map(String::valueOf)
                    .filter(level -> level.startsWith(args[3])).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}