package ua.vdev.minibuyer.menu.actions.impl;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ua.vdev.minibuyer.menu.actions.MenuAction;

public class SoundAction implements MenuAction {
    private final Sound sound;
    private final float volume;
    private final float pitch;

    public SoundAction(Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void execute(Player player) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}