package ua.vdev.minibuyer.menu.actions;

import org.bukkit.Sound;
import ua.vdev.minibuyer.menu.actions.impl.OpenMenuAction;
import ua.vdev.minibuyer.menu.actions.impl.SoundAction;

import java.util.ArrayList;
import java.util.List;

public class ActionFactory {

    public static List<MenuAction> createActions(List<String> commands) {
        List<MenuAction> actions = new ArrayList<>();
        if (commands == null || commands.isEmpty()) {
            return actions;
        }

        for (String command : commands) {
            try {
                if (command.startsWith("[openmenu]")) {
                    String menuName = command.replace("[openmenu]", "").trim();
                    actions.add(new OpenMenuAction(menuName));
                } else if (command.startsWith("[sound]")) {
                    String[] parts = command.replace("[sound]", "").trim().split(";");
                    if (parts.length >= 3) {
                        Sound sound = Sound.valueOf(parts[0].trim());
                        float volume = Float.parseFloat(parts[1].trim());
                        float pitch = Float.parseFloat(parts[2].trim());
                        actions.add(new SoundAction(sound, volume, pitch));
                    }
                }
            } catch (Exception e) {
            }
        }
        return actions;
    }
}