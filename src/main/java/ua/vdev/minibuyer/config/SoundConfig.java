package ua.vdev.minibuyer.config;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ua.vdev.minibuyer.config.model.SoundEffect;

public class SoundConfig {
    private final FileConfiguration config;

    public SoundConfig(JavaPlugin plugin) {
        this.config = plugin.getConfig();
    }

    public SoundEffect getUpdateSellerSound() {
        return getSoundEffect("sounds.update-seller", new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f));
    }

    public SoundEffect getSellSound() {
        return getSoundEffect("sounds.sell", new SoundEffect(Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f));
    }

    public SoundEffect getNoItemsSound() {
        return getSoundEffect("sounds.no-items", new SoundEffect(Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f));
    }

    private SoundEffect getSoundEffect(String path, SoundEffect defaultEffect) {
        try {
            String raw = config.getString(path, defaultEffect.sound().name());
            String[] parts = raw.split(";");
            Sound sound = Sound.valueOf(parts[0].trim().toUpperCase());
            float volume = parts.length > 1 ? Float.parseFloat(parts[1].trim()) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2].trim()) : 1.0f;
            return new SoundEffect(sound, volume, pitch);
        } catch (Exception e) {
            return defaultEffect;
        }
    }
}