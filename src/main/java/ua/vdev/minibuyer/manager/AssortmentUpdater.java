package ua.vdev.minibuyer.manager;

import lombok.Getter;
import org.bukkit.scheduler.BukkitTask;
import ua.vdev.minibuyer.MiniBuyer;
import ua.vdev.minibuyer.config.model.SoundEffect;
import ua.vdev.minibuyer.item.SellableItem;
import ua.vdev.minibuyer.util.TextUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class AssortmentUpdater {
    private final MiniBuyer plugin;
    private final MenuManager menuManager;
    private BukkitTask task;
    private int seconds;
    private final int intervalSeconds;
    private final List<String> updateSellerMessage;
    private final SoundEffect updateSoundEffect;
    private boolean showNow = false;

    public AssortmentUpdater(MiniBuyer plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;

        String time = plugin.getConfig().getString("update-interval", "01.00.00");
        this.intervalSeconds = parseInterval(time);
        this.seconds = intervalSeconds;
        this.updateSellerMessage = plugin.getConfig().getStringList("messages.update-seller");
        this.updateSoundEffect = plugin.getSoundConfig().getUpdateSellerSound();
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            seconds--;
            if (seconds <= 0) {
                updateAssortments();
                broadcastUpdate();
                showNow = true;
                seconds = intervalSeconds;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> showNow = false, 40);
            }
        }, 20, 20);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private int parseInterval(String time) {
        String[] parts = time.split("\\.");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int sec = Integer.parseInt(parts[2]);
        return hours * 3600 + minutes * 60 + sec;
    }

    private void updateAssortments() {
        plugin.getMenuLoader().getMenuNames().forEach(menuName -> {
            List<SellableItem> allItems = new ArrayList<>(menuManager.getItemConfig().getItems());
            Collections.shuffle(allItems);
            int max = Math.min(menuManager.getMenuConfig().getSellerItemSlots().size(), allItems.size());
            plugin.getMenuLoader().updateAssortment(menuName, allItems.stream().limit(max).toList());
        });
    }

    private void broadcastUpdate() {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            updateSellerMessage.forEach(line -> player.sendMessage(TextUtil.mm(line, null)));
            updateSoundEffect.play(player);
        });
    }

    public String getTimePlaceholder() {
        if (showNow) return "Сейчас";
        int s = seconds;
        int h = s / 3600;
        int m = (s % 3600) / 60;
        int sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }
}