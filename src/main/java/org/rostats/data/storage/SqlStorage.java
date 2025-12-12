package org.rostats.data.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.engine.effect.ActiveEffect;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SqlStorage implements PlayerDataStorage {

    private final ThaiRoCorePlugin plugin;
    private HikariDataSource dataSource;
    private final String tablePrefix;
    private final Gson gson;

    public SqlStorage(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        this.tablePrefix = plugin.getConfig().getString("storage.table-prefix", "ro_");
        this.gson = new Gson();
        initialize();
    }

    private void initialize() {
        String type = plugin.getConfig().getString("storage.type", "SQLITE").toUpperCase();
        HikariConfig config = new HikariConfig();

        if (type.equals("MYSQL")) {
            String host = plugin.getConfig().getString("storage.mysql.host");
            int port = plugin.getConfig().getInt("storage.mysql.port");
            String db = plugin.getConfig().getString("storage.mysql.database");
            String user = plugin.getConfig().getString("storage.mysql.username");
            String pass = plugin.getConfig().getString("storage.mysql.password");
            boolean ssl = plugin.getConfig().getBoolean("storage.mysql.use-ssl");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + ssl);
            config.setUsername(user);
            config.setPassword(pass);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            // SQLite Setup
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            if (!dbFile.exists()) {
                try {
                    dbFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create database.db!");
                }
            }
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setPoolName("ThaiRo-Pool");
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(5000);

        try {
            dataSource = new HikariDataSource(config);
            createTable();
            plugin.getLogger().info("Database connected (" + type + ")");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to database! " + e.getMessage());
        }
    }

    private void createTable() {
        // ใช้ TEXT สำหรับ active_effects เพื่อเก็บ JSON String
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "players (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(32), " +
                "base_level INT DEFAULT 1, " +
                "base_exp BIGINT DEFAULT 0, " +
                "job_level INT DEFAULT 1, " +
                "job_exp BIGINT DEFAULT 0, " +
                "points INT DEFAULT 0, " +
                "skill_points INT DEFAULT 0, " +
                "reset_count INT DEFAULT 0, " +
                "current_sp DOUBLE DEFAULT 0, " +
                "str INT DEFAULT 1, " +
                "agi INT DEFAULT 1, " +
                "vit INT DEFAULT 1, " +
                "intel INT DEFAULT 1, " + // 'int' เป็นคำสงวนใน SQL บางตัว ใช้ 'intel' แทน
                "dex INT DEFAULT 1, " +
                "luk INT DEFAULT 1, " +
                "active_effects TEXT" +
                ");";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create table", e);
        }
    }

    @Override
    public void loadData(Player player) {
        UUID uuid = player.getUniqueId();
        String sql = "SELECT * FROM " + tablePrefix + "players WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Read Data
                int baseLvl = rs.getInt("base_level");
                long baseExp = rs.getLong("base_exp");
                int jobLvl = rs.getInt("job_level");
                long jobExp = rs.getLong("job_exp");
                int points = rs.getInt("points");
                int skillPoints = rs.getInt("skill_points");
                int resetCount = rs.getInt("reset_count");
                double currentSp = rs.getDouble("current_sp");
                int str = rs.getInt("str");
                int agi = rs.getInt("agi");
                int vit = rs.getInt("vit");
                int intel = rs.getInt("intel");
                int dex = rs.getInt("dex");
                int luk = rs.getInt("luk");
                String effectsJson = rs.getString("active_effects");

                // Apply on Main Thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    PlayerData data = plugin.getStatManager().getData(uuid);

                    data.setBaseLevel(baseLvl);
                    data.setBaseExp(baseExp);
                    data.setJobLevel(jobLvl);
                    data.setJobExp(jobExp);
                    data.setStatPoints(points);
                    data.setSkillPoints(skillPoints);
                    data.setResetCount(resetCount);

                    data.setStat("STR", str);
                    data.setStat("AGI", agi);
                    data.setStat("VIT", vit);
                    data.setStat("INT", intel);
                    data.setStat("DEX", dex);
                    data.setStat("LUK", luk);

                    data.calculateMaxSP();
                    data.setCurrentSP(Math.min(currentSp, data.getMaxSP()));

                    // Load Effects form JSON
                    if (effectsJson != null && !effectsJson.isEmpty()) {
                        Type listType = new TypeToken<ArrayList<ActiveEffect>>(){}.getType();
                        List<ActiveEffect> loadedEffects = gson.fromJson(effectsJson, listType);
                        data.getActiveEffects().clear();
                        if (loadedEffects != null) {
                            data.getActiveEffects().addAll(loadedEffects);
                        }
                    }

                    plugin.getAttributeHandler().updatePlayerStats(player);
                    plugin.getManaManager().updateBar(player);
                    plugin.getManaManager().updateBaseExpBar(player);
                    plugin.getManaManager().updateJobExpBar(player);
                    plugin.getLogger().info("💾 [SQL] Loaded data for " + player.getName());
                });

            } else {
                // New Player -> Save Default
                saveData(player);
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load SQL data for " + player.getName(), e);
        }
    }

    @Override
    public void saveData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getStatManager().getData(uuid);
        if (data == null) return;

        // Serialize effects to JSON
        String effectsJson = gson.toJson(data.getActiveEffects());

        // Use REPLACE INTO (Works for SQLite & MySQL) to Handle Insert/Update automatically
        String sql = "REPLACE INTO " + tablePrefix + "players " +
                "(uuid, name, base_level, base_exp, job_level, job_exp, points, skill_points, reset_count, current_sp, str, agi, vit, intel, dex, luk, active_effects) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setString(2, player.getName());
            ps.setInt(3, data.getBaseLevel());
            ps.setLong(4, data.getBaseExp());
            ps.setInt(5, data.getJobLevel());
            ps.setLong(6, data.getJobExp());
            ps.setInt(7, data.getStatPoints());
            ps.setInt(8, data.getSkillPoints());
            ps.setInt(9, data.getResetCount());
            ps.setDouble(10, data.getCurrentSP());
            ps.setInt(11, data.getStat("STR"));
            ps.setInt(12, data.getStat("AGI"));
            ps.setInt(13, data.getStat("VIT"));
            ps.setInt(14, data.getStat("INT"));
            ps.setInt(15, data.getStat("DEX"));
            ps.setInt(16, data.getStat("LUK"));
            ps.setString(17, effectsJson);

            ps.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save SQL data for " + player.getName(), e);
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}