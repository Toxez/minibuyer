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
    private final String dbType;

    public DatabaseManager(MiniBuyer plugin, String dbType, String mysqlHost, int mysqlPort, String mysqlDatabase, String mysqlUsername, String mysqlPassword, String mysqlProperties) {
        this.plugin = plugin;
        this.dbType = dbType.toLowerCase();
        initPool(mysqlHost, mysqlPort, mysqlDatabase, mysqlUsername, mysqlPassword, mysqlProperties);
    }

    private void initPool(String mysqlHost, int mysqlPort, String mysqlDatabase, String mysqlUsername, String mysqlPassword, String mysqlProperties) {
        closeConnection();

        this.executor = Executors.newFixedThreadPool(4);
        HikariConfig config = new HikariConfig();

        if (dbType.equals("mysql")) {
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?%s", mysqlHost, mysqlPort, mysqlDatabase, mysqlProperties));
            config.setUsername(mysqlUsername);
            config.setPassword(mysqlPassword);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder() + "/mini.db");
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("MiniBuyerPool");
        dataSource = new HikariDataSource(config);

        String createTableSql = dbType.equals("mysql")
                ? "CREATE TABLE IF NOT EXISTS player_levels (" +
                "player_uuid VARCHAR(36) PRIMARY KEY," +
                "level INT DEFAULT 1," +
                "items_sold INT DEFAULT 0)"
                : "CREATE TABLE IF NOT EXISTS player_levels (" +
                "player_uuid TEXT PRIMARY KEY," +
                "level INTEGER DEFAULT 1," +
                "items_sold INTEGER DEFAULT 0)";
        executeAsync(createTableSql);
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
            } catch (SQLException e) {
            }
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
            } catch (SQLException e) {
            }
        });
    }

    public CompletableFuture<Void> savePlayerData(UUID uuid, int level, int itemsSold) {
        if (executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO player_levels (player_uuid, level, items_sold) VALUES (?,?,?) " +
                                 (dbType.equals("mysql") ? "ON DUPLICATE KEY UPDATE level=?, items_sold=?" : "ON CONFLICT(player_uuid) DO UPDATE SET level=?, items_sold=?"))) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, level);
                stmt.setInt(3, itemsSold);
                if (dbType.equals("mysql")) {
                    stmt.setInt(4, level);
                    stmt.setInt(5, itemsSold);
                } else {
                    stmt.setInt(4, level);
                    stmt.setInt(5, itemsSold);
                }
                stmt.executeUpdate();
            } catch (SQLException e) {
            }
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
                            "INSERT INTO player_levels (player_uuid, level, items_sold) VALUES (?,?,?) " +
                                    (dbType.equals("mysql") ? "ON DUPLICATE KEY UPDATE level=?, items_sold=?" : "ON CONFLICT(player_uuid) DO UPDATE SET level=?, items_sold=?"))) {
                        updateStmt.setString(1, playerId.toString());
                        updateStmt.setInt(2, currentLevel);
                        updateStmt.setInt(3, currentItemsSold + amount);
                        updateStmt.setInt(4, currentLevel);
                        updateStmt.setInt(5, currentItemsSold + amount);
                        updateStmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
            }
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
                            "INSERT INTO player_levels (player_uuid, level, items_sold) VALUES (?,?,?) " +
                                    (dbType.equals("mysql") ? "ON DUPLICATE KEY UPDATE level=?, items_sold=?" : "ON CONFLICT(player_uuid) DO UPDATE SET level=?, items_sold=?"))) {
                        updateStmt.setString(1, playerId.toString());
                        updateStmt.setInt(2, level);
                        updateStmt.setInt(3, currentItemsSold);
                        updateStmt.setInt(4, level);
                        updateStmt.setInt(5, currentItemsSold);
                        updateStmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
            }
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