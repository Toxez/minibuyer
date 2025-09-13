package ua.vdev.minibuyer.config;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ua.vdev.minibuyer.config.model.Decoration;
import ua.vdev.minibuyer.config.model.StaticItem;
import ua.vdev.minibuyer.item.SellableItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class MenuConfig {
    private final YamlConfiguration config;
    private final String title;
    private final int size;
    private final List<Integer> sellerItemSlots;
    private final List<String> itemLore;
    private final List<Decoration> decorations;
    private final List<StaticItem> staticItems;

    public MenuConfig(JavaPlugin plugin, String fileName) {
        File menuFolder = new File(plugin.getDataFolder(), "menu");
        if (!menuFolder.exists()) {
            menuFolder.mkdirs();
        }
        File file = new File(menuFolder, fileName.endsWith(".yml") ? fileName : fileName + ".yml");
        if (!file.exists()) {
            plugin.saveResource("menu/" + file.getName(), false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        var menuSection = config.getConfigurationSection("menu");
        if (menuSection == null) {
            menuSection = config;
        }

        title = menuSection.getString("title", "<black>Скупщик");
        size = menuSection.getInt("size", 54);
        sellerItemSlots = menuSection.getIntegerList("seller-item-slots");
        itemLore = menuSection.getStringList("item-lore");
        decorations = parseDecorations(menuSection.getMapList("decorations"));
        staticItems = parseStaticItems(menuSection.getMapList("static-items"));
    }

    private List<Decoration> parseDecorations(List<Map<?, ?>> decorationMaps) {
        if (decorationMaps == null || decorationMaps.isEmpty()) {
            return Collections.emptyList();
        }

        return decorationMaps.stream()
                .map(this::parseDecoration)
                .filter(decoration -> !decoration.slots().isEmpty())
                .collect(Collectors.toList());
    }

    private Decoration parseDecoration(Map<?, ?> map) {
        Material material = parseMaterial(map.get("material"));
        String name = map.get("name") != null ? String.valueOf(map.get("name")) : "";
        List<String> lore = map.get("lore") instanceof List<?> loreList
                ? loreList.stream().map(String::valueOf).collect(Collectors.toList())
                : Collections.emptyList();
        List<String> leftClickCommands = map.get("left_click_command") instanceof List<?> cmdList
                ? cmdList.stream().map(String::valueOf).collect(Collectors.toList())
                : Collections.emptyList();
        List<String> rightClickCommands = map.get("right_click_command") instanceof List<?> cmdList
                ? cmdList.stream().map(String::valueOf).collect(Collectors.toList())
                : Collections.emptyList();
        List<Integer> slots = parseSlots(map);

        return new Decoration(slots, material, name, lore, leftClickCommands, rightClickCommands);
    }

    private List<StaticItem> parseStaticItems(List<Map<?, ?>> staticItemMaps) {
        if (staticItemMaps == null || staticItemMaps.isEmpty()) {
            return Collections.emptyList();
        }

        return staticItemMaps.stream()
                .map(this::parseStaticItem)
                .filter(item -> item.slot() >= 0)
                .collect(Collectors.toList());
    }

    private StaticItem parseStaticItem(Map<?, ?> map) {
        Material material = parseMaterial(map.get("material"));
        String name = map.get("name") != null ? String.valueOf(map.get("name")) : material.name();
        int price = toInt(map.get("price"), 0);
        int amount = toInt(map.get("amount"), 1);
        int slot = toInt(map.get("slot"), -1);
        List<String> lore = map.get("lore") instanceof List<?> loreList
                ? loreList.stream().map(String::valueOf).collect(Collectors.toList())
                : Collections.emptyList();

        return new StaticItem(slot, new SellableItem(material, name, price, amount), lore);
    }

    private Material parseMaterial(Object materialObj) {
        if (materialObj == null) {
            return Material.AIR;
        }
        try {
            return Material.valueOf(String.valueOf(materialObj));
        } catch (IllegalArgumentException e) {
            return Material.AIR;
        }
    }

    private List<Integer> parseSlots(Map<?, ?> map) {
        List<Integer> slots = new ArrayList<>();

        if (map.containsKey("slot")) {
            try {
                slots.add(Integer.parseInt(String.valueOf(map.get("slot"))));
            } catch (NumberFormatException ignored) {
            }
        }

        Object slotsObj = map.get("slots");
        if (slotsObj instanceof List<?> rawSlots) {
            for (Object slot : rawSlots) {
                try {
                    slots.add(Integer.parseInt(String.valueOf(slot)));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return slots;
    }

    private int toInt(Object o, int def) {
        try {
            return o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}