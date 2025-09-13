package ua.vdev.minibuyer.config.model;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public record SoundEffect(Sound sound, float volume, float pitch) {
    public void play(Player player) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}