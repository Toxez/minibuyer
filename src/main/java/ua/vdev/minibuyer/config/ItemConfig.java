package ua.vdev.minibuyer.config;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ua.vdev.minibuyer.item.SellableItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ItemConfig {
    private final List<SellableItem> items;

    public ItemConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "item.yml");
        if (!file.exists()) plugin.saveResource("item.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        items = new ArrayList<>();
        config.getKeys(false).forEach(key -> {
            String path = key + ".";
            Material material = Material.valueOf(config.getString(path + "material"));
            String translation = config.getString(path + "translation", material.name());
            int price = config.getInt(path + "price", 0);
            int amount = config.getInt(path + "amount", 1);
            items.add(new SellableItem(material, translation, price, amount));
        });
    }
}