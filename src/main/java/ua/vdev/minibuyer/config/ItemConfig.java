package ua.vdev.minibuyer.config;

import lombok.Getter;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import ua.vdev.minibuyer.item.SellableItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class ItemConfig {
    private final List<SellableItem> items = new ArrayList<>();

    public ItemConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "item.yml");
        if (!file.exists()) plugin.saveResource("item.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (Map<?, ?> itemMap : config.getMapList("items")) {
            try {
                Material material = Material.valueOf(String.valueOf(itemMap.get("material")));
                String name = itemMap.containsKey("name") ? String.valueOf(itemMap.get("name")) : material.name();
                int price = toInt(itemMap.get("price"), 0);
                int amount = toInt(itemMap.get("amount"), 1);
                items.add(new SellableItem(material, name, price, amount));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private int toInt(Object o, int def) {
        return o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(String.valueOf(o));
    }
}