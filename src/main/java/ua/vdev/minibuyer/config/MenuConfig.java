package ua.vdev.minibuyer.config;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

@Getter
public class MenuConfig {
    private final YamlConfiguration config;
    private final String title;
    private final int size;
    private final List<Integer> sellerItemSlots;
    private final List<String> itemLore;
    private final List<Decoration> decorations;

    public MenuConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "menu.yml");
        if (!file.exists()) plugin.saveResource("menu.yml", false);
        config = YamlConfiguration.loadConfiguration(file);

        title = config.getString("title", "<black>Скупщик");
        size = config.getInt("size", 54);
        sellerItemSlots = config.getIntegerList("seller-item");
        itemLore = config.getStringList("item-lore");
        decorations = Decoration.fromConfig(config.getConfigurationSection("decorations"));
    }

    @Getter
    public static class Decoration {
        private final int slot;
        private final Material material;
        private final String name;
        private final List<String> lore;

        public Decoration(int slot, Material material, String name, List<String> lore) {
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = lore;
        }

        public static List<Decoration> fromConfig(org.bukkit.configuration.ConfigurationSection section) {
            java.util.ArrayList<Decoration> decorations = new java.util.ArrayList<>();
            if (section == null) return decorations;
            for (String key : section.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection deco = section.getConfigurationSection(key);
                if (deco == null) continue;
                int slot = deco.getInt("slot");
                Material material = Material.valueOf(deco.getString("material"));
                String name = deco.getString("name", "");
                List<String> lore = deco.getStringList("lore");
                decorations.add(new Decoration(slot, material, name, lore));
            }
            return decorations;
        }
    }
}