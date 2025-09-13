package ua.vdev.minibuyer.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import ua.vdev.minibuyer.MiniBuyer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DatabaseManager {

    private final MiniBuyer plugin;
    private HikariDataSource dataSource;
    private ExecutorService executor;

    public DatabaseManager(MiniBuyer plugin) {
        this.plugin = plugin;
        initPool();
    }

    private void initPool() {
        closeConnection();

        this.executor = Executors.newFixedThreadPool(4);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + "/mini.db");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("MiniBuyerPool");
        dataSource = new HikariDataSource(config);

        executeAsync("CREATE TABLE IF NOT EXISTS player_levels (" +
                "player_uuid TEXT PRIMARY KEY," +
                "level INTEGER DEFAULT 1," +
                "items_sold INTEGER DEFAULT 0)");
    }

    private void executeAsync(String sql, Consumer<PreparedStatement> setter) {
        if (executor == null || executor.isShutdown()) {
            return;
        }

        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (setter != null) setter.accept(stmt);
                stmt.executeUpdate();
            } catch (SQLException ignored) {}
        });
    }

    private void executeAsync(String sql) {
        executeAsync(sql, null);
    }

    public void loadPlayerData(UUID uuid, Consumer<ResultSet> callback) {
        if (executor == null || executor.isShutdown()) {
            return;
        }

        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT level, items_sold FROM player_levels WHERE player_uuid=?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (callback != null) {
                        callback.accept(rs);
                    }
                }
            } catch (SQLException ignored) {}
        });
    }

    public CompletableFuture<Void> savePlayerData(UUID uuid, int level, int itemsSold) {
        if (executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO player_levels (player_uuid, level, items_sold) VALUES (?,?,?)")) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, level);
                stmt.setInt(3, itemsSold);
                stmt.executeUpdate();
            } catch (SQLException ignored) {}
        }, executor);
    }

    public CompletableFuture<Void> addItemsSold(UUID playerId, int amount) {
        if (executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement selectStmt = conn.prepareStatement(
                         "SELECT level, items_sold FROM player_levels WHERE player_uuid=?")) {

                selectStmt.setString(1, playerId.toString());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    int currentItemsSold = 0;
                    int currentLevel = 1;

                    if (rs.next()) {
                        currentItemsSold = rs.getInt("items_sold");
                        currentLevel = rs.getInt("level");
                    }

                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "INSERT OR REPLACE INTO player_levels (player_uuid, level, items_sold) VALUES (?,?,?)")) {
                        updateStmt.setString(1, playerId.toString());
                        updateStmt.setInt(2, currentLevel);
                        updateStmt.setInt(3, currentItemsSold + amount);
                        updateStmt.executeUpdate();
                    }
                }
            } catch (SQLException ignored) {}
        }, executor);
    }

    public CompletableFuture<Void> updatePlayerLevel(UUID playerId, int level) {
        if (executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement selectStmt = conn.prepareStatement(
                         "SELECT items_sold FROM player_levels WHERE player_uuid=?")) {

                selectStmt.setString(1, playerId.toString());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    int currentItemsSold = 0;

                    if (rs.next()) {
                        currentItemsSold = rs.getInt("items_sold");
                    }

                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "INSERT OR REPLACE INTO player_levels (player_uuid, level, items_sold) VALUES (?,?,?)")) {
                        updateStmt.setString(1, playerId.toString());
                        updateStmt.setInt(2, level);
                        updateStmt.setInt(3, currentItemsSold);
                        updateStmt.executeUpdate();
                    }
                }
            } catch (SQLException ignored) {}
        }, executor);
    }

    public int getPlayerLevel(UUID playerId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT level FROM player_levels WHERE player_uuid=?")) {

            stmt.setString(1, playerId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("level") : 1;
            }
        } catch (SQLException e) {
            return 1;
        }
    }

    public int getItemsSold(UUID playerId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT items_sold FROM player_levels WHERE player_uuid=?")) {

            stmt.setString(1, playerId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("items_sold") : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public CompletableFuture<Integer> getPlayerLevelAsync(UUID playerId) {
        if (executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(1);
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT level FROM player_levels WHERE player_uuid=?")) {

                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("level");
                    } else {
                        return 1;
                    }
                }
            } catch (SQLException e) {
                return 1;
            }
        }, executor);
    }

    public CompletableFuture<Integer> getItemsSoldAsync(UUID playerId) {
        if (executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(0);
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT items_sold FROM player_levels WHERE player_uuid=?")) {

                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("items_sold");
                    } else {
                        return 0;
                    }
                }
            } catch (SQLException e) {
                return 0;
            }
        }, executor);
    }

    public CompletableFuture<PlayerData> getPlayerDataAsync(UUID playerId) {
        if (executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(new PlayerData(1, 0));
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT level, items_sold FROM player_levels WHERE player_uuid=?")) {

                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new PlayerData(rs.getInt("level"), rs.getInt("items_sold"));
                    } else {
                        return new PlayerData(1, 0);
                    }
                }
            } catch (SQLException e) {
                return new PlayerData(1, 0);
            }
        }, executor);
    }

    public static class PlayerData {
        private final int level;
        private final int itemsSold;

        public PlayerData(int level, int itemsSold) {
            this.level = level;
            this.itemsSold = itemsSold;
        }

        public int getLevel() {
            return level;
        }

        public int getItemsSold() {
            return itemsSold;
        }
    }

    public void closeConnection() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}