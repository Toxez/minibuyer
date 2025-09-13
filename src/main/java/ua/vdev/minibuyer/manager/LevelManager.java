package ua.vdev.minibuyer.manager;

import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import ua.vdev.minibuyer.MiniBuyer;
import ua.vdev.minibuyer.config.model.SoundEffect;
import ua.vdev.minibuyer.database.DatabaseManager;
import ua.vdev.minibuyer.util.TextUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LevelManager {

    private final MiniBuyer plugin;
    private final DatabaseManager databaseManager;
    private final File levelsFile;
    private final YamlConfiguration levelsConfig;
    private final Map<Integer, Level> levels = new HashMap<>();

    public static class Level {
        private final int itemsRequired;
        private final double priceMultiplier;
        private final List<String> levelUpMessage;
        private final SoundEffect soundEffect;

        public Level(int itemsRequired, double priceMultiplier, List<String> levelUpMessage, SoundEffect soundEffect) {
            this.itemsRequired = itemsRequired;
            this.priceMultiplier = priceMultiplier;
            this.levelUpMessage = levelUpMessage;
            this.soundEffect = soundEffect;
        }

        public int getItemsRequired() {
            return itemsRequired;
        }

        public double getPriceMultiplier() {
            return priceMultiplier;
        }

        public List<String> getLevelUpMessage() {
            return levelUpMessage;
        }

        public SoundEffect getSoundEffect() {
            return soundEffect;
        }
    }

    public LevelManager(MiniBuyer plugin) {
        this.plugin = plugin;
        this.databaseManager = new DatabaseManager(plugin);
        this.levelsFile = new File(plugin.getDataFolder(), "levels.yml");
        if (!levelsFile.exists()) {
            plugin.saveResource("levels.yml", false);
        }
        this.levelsConfig = YamlConfiguration.loadConfiguration(levelsFile);

        loadLevels();
    }

    private void loadLevels() {
        levels.put(1, new Level(0, 1.0, List.of(), null));

        List<Map<?, ?>> levelList = levelsConfig.getMapList("seller-levels");
        for (Map<?, ?> levelMap : levelList) {
            try {
                int level = toInt(levelMap.get("level"), -1);
                if (level <= 1) {
                    plugin.getLogger().warning("Уровень в конфиге должен быть > 1");
                    continue;
                }
                int itemsRequired = toInt(levelMap.get("required-items"), 0);
                double multiplier = toDouble(levelMap.get("multiplier"), 1.0);
                @SuppressWarnings("unchecked")
                List<String> message = levelMap.containsKey("message") ? (List<String>) levelMap.get("message") : List.of();

                SoundEffect soundEffect = null;
                if (levelMap.containsKey("sound")) {
                    String raw = String.valueOf(levelMap.get("sound"));
                    String[] parts = raw.split(";");
                    Sound sound = Sound.valueOf(parts[0].trim().toUpperCase());
                    float volume = parts.length > 1 ? Float.parseFloat(parts[1].trim()) : 1.0f;
                    float pitch = parts.length > 2 ? Float.parseFloat(parts[2].trim()) : 1.0f;
                    soundEffect = new SoundEffect(sound, volume, pitch);
                }

                levels.put(level, new Level(itemsRequired, multiplier, message, soundEffect));
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка загрузки уровня: " + e.getMessage());
            }
        }
    }

    private int toInt(Object o, int def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception ex) {
            return def;
        }
    }

    private double toDouble(Object o, double def) {
        if (o == null) return def;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception ex) {
            return def;
        }
    }

    public CompletableFuture<Void> addItemsSold(Player player, int amount) {
        return databaseManager.addItemsSold(player.getUniqueId(), amount)
                .thenCompose(v -> checkLevelUp(player));
    }

    public CompletableFuture<Void> setPlayerLevel(Player player, int level) {
        UUID playerId = player.getUniqueId();
        return databaseManager.updatePlayerLevel(playerId, level)
                .thenCompose(v -> databaseManager.getItemsSoldAsync(playerId)
                        .thenCompose(itemsSold -> databaseManager.addItemsSold(playerId, -itemsSold)))
                .thenRun(() -> plugin.getMenuManager().updatePlayerMenu(player));
    }

    public CompletableFuture<Integer> getPlayerLevelAsync(Player player) {
        return databaseManager.getPlayerLevelAsync(player.getUniqueId());
    }

    public int getPlayerLevel(Player player) {
        return databaseManager.getPlayerLevel(player.getUniqueId());
    }

    public CompletableFuture<Integer> getItemsSoldAsync(Player player) {
        return databaseManager.getItemsSoldAsync(player.getUniqueId());
    }

    public int getItemsSold(Player player) {
        return databaseManager.getItemsSold(player.getUniqueId());
    }

    public CompletableFuture<Double> getPriceMultiplierAsync(Player player) {
        return getPlayerLevelAsync(player)
                .thenApply(level -> {
                    Level l = levels.getOrDefault(level, new Level(0, 1.0, List.of(), null));
                    return l.getPriceMultiplier();
                });
    }

    public double getPriceMultiplier(Player player) {
        return getPriceMultiplierAsync(player).join();
    }

    public Map<Integer, Level> getLevels() {
        return Map.copyOf(levels);
    }

    public CompletableFuture<String> getItemsRequiredForNextLevelAsync(Player player) {
        return databaseManager.getPlayerDataAsync(player.getUniqueId())
                .thenApply(playerData -> {
                    int currentLevel = playerData.getLevel();
                    int itemsSold = playerData.getItemsSold();
                    Level nextLevel = levels.get(currentLevel + 1);
                    if (nextLevel == null) {
                        return "max";
                    }
                    int remaining = nextLevel.getItemsRequired() - itemsSold;
                    if (remaining < 0) remaining = 0;
                    return String.valueOf(remaining);
                });
    }

    public String getItemsRequiredForNextLevel(Player player) {
        return getItemsRequiredForNextLevelAsync(player).join();
    }

    private CompletableFuture<Void> checkLevelUp(Player player) {
        return databaseManager.getPlayerDataAsync(player.getUniqueId())
                .thenCompose(playerData -> {
                    final int[] currentItemsSold = {playerData.getItemsSold()};
                    int currentLevel = playerData.getLevel();
                    boolean leveledUp = false;

                    while (true) {
                        Level nextLevel = levels.get(currentLevel + 1);
                        if (nextLevel == null || currentItemsSold[0] < nextLevel.getItemsRequired()) {
                            break;
                        }

                        currentLevel++;
                        leveledUp = true;

                        currentItemsSold[0] -= nextLevel.getItemsRequired();

                        final int finalLevel = currentLevel;
                        nextLevel.getLevelUpMessage().forEach(message ->
                                player.sendMessage(TextUtil.mm(message, Map.of("level", String.valueOf(finalLevel))))
                        );

                        if (nextLevel.getSoundEffect() != null) {
                            try {
                                nextLevel.getSoundEffect().play(player);
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    if (leveledUp) {
                        return databaseManager.savePlayerData(player.getUniqueId(), currentLevel, currentItemsSold[0])
                                .thenRun(() -> plugin.getMenuManager().updatePlayerMenu(player));
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }

    public void closeConnection() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
    }
}