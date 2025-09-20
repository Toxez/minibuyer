package ua.vdev.minibuyer.manager;

import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import ua.vdev.minibuyer.MiniBuyer;
import ua.vdev.minibuyer.config.model.SoundEffect;
import ua.vdev.minibuyer.database.DatabaseManager;
import ua.vdev.minibuyer.util.TextUtil;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LevelManager {

    private final MiniBuyer plugin;
    private final DatabaseManager databaseManager;
    private final YamlConfiguration levelsConfig;
    private final Map<Integer, Level> levels = new HashMap<>();
    private final Map<UUID, CachedPlayerData> playerDataCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = TimeUnit.SECONDS.toMillis(5);

    public record Level(int itemsRequired, double priceMultiplier, List<String> levelUpMessage, SoundEffect soundEffect) {}

    public record CachedPlayerData(int level, int itemsSold, double priceMultiplier, String itemsRequired, long timestamp) {}

    public LevelManager(MiniBuyer plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        File levelsFile = new File(plugin.getDataFolder(), "levels.yml");
        if (!levelsFile.exists()) {
            plugin.saveResource("levels.yml", false);
        }
        this.levelsConfig = YamlConfiguration.loadConfiguration(levelsFile);
        loadLevels();
    }

    private void loadLevels() {
        levels.put(1, new Level(0, 1.0, List.of(), null));

        levelsConfig.getMapList("seller-levels").forEach(levelMap -> {
            try {
                int level = toInt(levelMap.get("level"), -1);
                if (level <= 1) {
                    plugin.getLogger().warning("Уровень в конфиге должен быть > 1");
                    return;
                }
                int itemsRequired = toInt(levelMap.get("required-items"), 0);
                double multiplier = toDouble(levelMap.get("multiplier"), 1.0);

                Object rawMessage = levelMap.get("message");
                List<String> message = rawMessage instanceof List<?> rawList
                        ? rawList.stream().map(String::valueOf).collect(Collectors.toList())
                        : List.of();

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
            }
        });
    }

    private int toInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception ignored) {
            return def;
        }
    }

    private double toDouble(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception ignored) {
            return def;
        }
    }

    public CompletableFuture<Void> addItemsSold(Player player, int amount) {
        return databaseManager.addItemsSold(player.getUniqueId(), amount)
                .thenCompose(v -> checkLevelUp(player))
                .thenRun(() -> invalidateCache(player.getUniqueId()));
    }

    public CompletableFuture<Void> setPlayerLevel(Player player, int level) {
        UUID playerId = player.getUniqueId();
        return databaseManager.updatePlayerLevel(playerId, level)
                .thenCompose(v -> databaseManager.getItemsSoldAsync(playerId)
                        .thenCompose(itemsSold -> databaseManager.addItemsSold(playerId, -itemsSold)))
                .thenRun(() -> {
                    invalidateCache(playerId);
                    plugin.getMenuManager().updatePlayerMenu(player);
                });
    }

    public CompletableFuture<Integer> getPlayerLevelAsync(Player player) {
        CachedPlayerData cached = playerDataCache.get(player.getUniqueId());
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_DURATION_MS) {
            return CompletableFuture.completedFuture(cached.level());
        }
        return databaseManager.getPlayerLevelAsync(player.getUniqueId())
                .thenApply(level -> {
                    updateCache(player.getUniqueId());
                    return level;
                });
    }

    public int getPlayerLevel(Player player) {
        CachedPlayerData cached = playerDataCache.get(player.getUniqueId());
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_DURATION_MS) {
            return cached.level();
        }
        return databaseManager.getPlayerLevel(player.getUniqueId());
    }

    public CompletableFuture<Integer> getItemsSoldAsync(Player player) {
        CachedPlayerData cached = playerDataCache.get(player.getUniqueId());
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_DURATION_MS) {
            return CompletableFuture.completedFuture(cached.itemsSold());
        }
        return databaseManager.getItemsSoldAsync(player.getUniqueId())
                .thenApply(itemsSold -> {
                    updateCache(player.getUniqueId());
                    return itemsSold;
                });
    }

    public int getItemsSold(Player player) {
        CachedPlayerData cached = playerDataCache.get(player.getUniqueId());
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_DURATION_MS) {
            return cached.itemsSold();
        }
        return databaseManager.getItemsSold(player.getUniqueId());
    }

    public CompletableFuture<Double> getPriceMultiplierAsync(Player player) {
        CachedPlayerData cached = playerDataCache.get(player.getUniqueId());
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_DURATION_MS) {
            return CompletableFuture.completedFuture(cached.priceMultiplier());
        }
        return getPlayerLevelAsync(player)
                .thenApply(level -> {
                    updateCache(player.getUniqueId());
                    return levels.getOrDefault(level, new Level(0, 1.0, List.of(), null)).priceMultiplier();
                });
    }

    public double getPriceMultiplier(Player player) {
        CachedPlayerData cached = playerDataCache.get(player.getUniqueId());
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_DURATION_MS) {
            return cached.priceMultiplier();
        }
        return getPriceMultiplierAsync(player).join();
    }

    public CompletableFuture<String> getItemsRequiredForNextLevelAsync(Player player) {
        CachedPlayerData cached = playerDataCache.get(player.getUniqueId());
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_DURATION_MS) {
            return CompletableFuture.completedFuture(cached.itemsRequired());
        }
        return databaseManager.getPlayerDataAsync(player.getUniqueId())
                .thenApply(playerData -> {
                    int currentLevel = playerData.getLevel();
                    int itemsSold = playerData.getItemsSold();
                    Level nextLevel = levels.get(currentLevel + 1);
                    if (nextLevel == null) return "max";
                    int remaining = nextLevel.itemsRequired() - itemsSold;
                    String result = String.valueOf(Math.max(remaining, 0));
                    updateCache(player.getUniqueId());
                    return result;
                });
    }

    public String getItemsRequiredForNextLevel(Player player) {
        CachedPlayerData cached = playerDataCache.get(player.getUniqueId());
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_DURATION_MS) {
            return cached.itemsRequired();
        }
        return getItemsRequiredForNextLevelAsync(player).join();
    }

    private CompletableFuture<Void> checkLevelUp(Player player) {
        return databaseManager.getPlayerDataAsync(player.getUniqueId())
                .thenCompose(playerData -> {
                    int itemsSold = playerData.getItemsSold();
                    int currentLevel = playerData.getLevel();
                    boolean leveledUp = false;

                    while (true) {
                        Level nextLevel = levels.get(currentLevel + 1);
                        if (nextLevel == null || itemsSold < nextLevel.itemsRequired()) break;

                        itemsSold -= nextLevel.itemsRequired();
                        currentLevel++;
                        leveledUp = true;

                        final int finalLevel = currentLevel;
                        nextLevel.levelUpMessage().forEach(message ->
                                player.sendMessage(TextUtil.mm(message, Map.of("level", String.valueOf(finalLevel))))
                        );

                        Optional.ofNullable(nextLevel.soundEffect()).ifPresent(effect -> {
                            try {
                                effect.play(player);
                            } catch (Exception ignored) {}
                        });
                    }

                    if (leveledUp) {
                        int finalItemsSold = itemsSold;
                        return databaseManager.savePlayerData(player.getUniqueId(), currentLevel, finalItemsSold)
                                .thenRun(() -> {
                                    invalidateCache(player.getUniqueId());
                                    plugin.getMenuManager().updatePlayerMenu(player);
                                });
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }

    private void updateCache(UUID playerId) {
        databaseManager.getPlayerDataAsync(playerId).thenAccept(playerData -> {
            int level = playerData.getLevel();
            int itemsSold = playerData.getItemsSold();
            double multiplier = levels.getOrDefault(level, new Level(0, 1.0, List.of(), null)).priceMultiplier();
            String itemsRequired = calculateItemsRequired(level, itemsSold);
            playerDataCache.put(playerId, new CachedPlayerData(level, itemsSold, multiplier, itemsRequired, System.currentTimeMillis()));
        });
    }

    private String calculateItemsRequired(int currentLevel, int itemsSold) {
        Level nextLevel = levels.get(currentLevel + 1);
        if (nextLevel == null) return "max";
        int remaining = nextLevel.itemsRequired() - itemsSold;
        return String.valueOf(Math.max(remaining, 0));
    }

    private void invalidateCache(UUID playerId) {
        playerDataCache.remove(playerId);
    }

    public Map<Integer, Level> getLevels() {
        return Map.copyOf(levels);
    }

    public void closeConnection() {
        playerDataCache.clear();
    }
}