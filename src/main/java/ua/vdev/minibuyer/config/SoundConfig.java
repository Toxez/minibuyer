package ua.vdev.minibuyer.config;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class SoundConfig {
    private final FileConfiguration config;

    public SoundConfig(JavaPlugin plugin) {
        this.config = plugin.getConfig();
    }

    public Sound getUpdateSellerSound() {
        return getSound("sounds.update-seller", Sound.ENTITY_PLAYER_LEVELUP);
    }

    public Sound getSellSound() {
        return getSound("sounds.sell", Sound.ENTITY_VILLAGER_YES);
    }

    public Sound getNoItemsSound() {
        return getSound("sounds.no-items", Sound.ENTITY_VILLAGER_NO);
    }

    private Sound getSound(String path, Sound defaultSound) {
        try {
            String name = config.getString(path, defaultSound.name());
            return Sound.valueOf(name);
        } catch (Exception e) {
            return defaultSound;
        }
    }
}