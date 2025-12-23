package managers;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages database operations using SQLite.
 */
public class DatabaseManager {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
  private static DatabaseManager instance;
  private static final String DB_PATH = "data/bot.db";
  private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
  private Connection connection;

  private DatabaseManager() {
    initializeDatabase();
  }

  /**
   * Gets the singleton instance of DatabaseManager.
   *
   * @return DatabaseManager instance
   */
  public static DatabaseManager getInstance() {
    if (instance == null) {
      instance = new DatabaseManager();
    }
    return instance;
  }

  /**
   * Initializes the database and runs migrations.
   */
  private void initializeDatabase() {
    try {
      // Create data directory if it doesn't exist
      File dataDir = new File("data");
      if (!dataDir.exists()) {
        dataDir.mkdirs();
      }

      // Run Flyway migrations
      Flyway flyway = Flyway.configure()
          .dataSource(DB_URL, null, null)
          .locations("classpath:db/migration")
          .load();
      flyway.migrate();

      logger.info("Database initialized successfully at: {}", DB_PATH);
    } catch (Exception e) {
      logger.error("Failed to initialize database", e);
      throw new RuntimeException("Database initialization failed", e);
    }
  }

  /**
   * Gets a database connection.
   *
   * @return Connection
   */
  private Connection getConnection() throws SQLException {
    if (connection == null || connection.isClosed()) {
      connection = DriverManager.getConnection(DB_URL);
    }
    return connection;
  }

  /**
   * Records music playback time for a guild.
   *
   * @param guildId The guild ID
   * @param seconds Seconds to add
   */
  public void addGuildPlaybackTime(String guildId, long seconds) {
    try (Connection conn = getConnection()) {
      String sql = """
          INSERT INTO guild_music_stats (guild_id, total_playback_seconds, last_updated)
          VALUES (?, ?, strftime('%s', 'now'))
          ON CONFLICT(guild_id) DO UPDATE SET
            total_playback_seconds = total_playback_seconds + ?,
            last_updated = strftime('%s', 'now')
          """;
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, guildId);
        stmt.setLong(2, seconds);
        stmt.setLong(3, seconds);
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      logger.error("Failed to add guild playback time", e);
    }
  }

  /**
   * Gets total playback time for a guild in seconds.
   *
   * @param guildId The guild ID
   * @return Total playback seconds
   */
  public long getGuildPlaybackTime(String guildId) {
    try (Connection conn = getConnection()) {
      String sql = "SELECT total_playback_seconds FROM guild_music_stats WHERE guild_id = ?";
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, guildId);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return rs.getLong("total_playback_seconds");
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Failed to get guild playback time", e);
    }
    return 0;
  }

  /**
   * Records command usage for a guild.
   *
   * @param guildId     The guild ID
   * @param commandName The command name
   */
  public void recordCommandUsage(String guildId, String commandName) {
    try (Connection conn = getConnection()) {
      String sql = """
          INSERT INTO guild_command_stats (guild_id, command_name, usage_count, last_used)
          VALUES (?, ?, 1, strftime('%s', 'now'))
          ON CONFLICT(guild_id, command_name) DO UPDATE SET
            usage_count = usage_count + 1,
            last_used = strftime('%s', 'now')
          """;
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, guildId);
        stmt.setString(2, commandName);
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      logger.error("Failed to record command usage", e);
    }
  }

  /**
   * Gets total command usage count for a guild.
   *
   * @param guildId The guild ID
   * @return Total command usage count
   */
  public long getTotalCommandUsage(String guildId) {
    try (Connection conn = getConnection()) {
      String sql = "SELECT SUM(usage_count) as total FROM guild_command_stats WHERE guild_id = ?";
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, guildId);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return rs.getLong("total");
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Failed to get total command usage", e);
    }
    return 0;
  }

  /**
   * Starts a playback session for a user in a guild.
   *
   * @param guildId The guild ID
   * @param userId  The user ID
   * @return Session ID
   */
  public long startPlaybackSession(String guildId, String userId) {
    try (Connection conn = getConnection()) {
      String sql = """
          INSERT INTO playback_sessions (guild_id, user_id, start_time)
          VALUES (?, ?, strftime('%s', 'now'))
          """;
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, guildId);
        stmt.setString(2, userId);
        stmt.executeUpdate();

        // SQLite doesn't support getGeneratedKeys(), use last_insert_rowid() instead
        try (Statement queryStmt = conn.createStatement();
            ResultSet rs = queryStmt.executeQuery("SELECT last_insert_rowid()")) {
          if (rs.next()) {
            return rs.getLong(1);
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Failed to start playback session", e);
    }
    return -1;
  }

  /**
   * Ends a playback session and updates user listening stats.
   *
   * @param sessionId The session ID
   */
  public void endPlaybackSession(long sessionId) {
    try (Connection conn = getConnection()) {
      // Get session info
      String selectSql = "SELECT guild_id, user_id, start_time FROM playback_sessions WHERE session_id = ?";
      String guildId = null;
      String userId = null;
      long startTime = 0;

      try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
        stmt.setLong(1, sessionId);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            guildId = rs.getString("guild_id");
            userId = rs.getString("user_id");
            startTime = rs.getLong("start_time");
          } else {
            return; // Session not found
          }
        }
      }

      // Calculate duration
      long endTime = System.currentTimeMillis() / 1000;
      long duration = endTime - startTime;

      // Update session
      String updateSql = """
          UPDATE playback_sessions
          SET end_time = ?, duration_seconds = ?
          WHERE session_id = ?
          """;
      try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
        stmt.setLong(1, endTime);
        stmt.setLong(2, duration);
        stmt.setLong(3, sessionId);
        stmt.executeUpdate();
      }

      // Update user listening stats
      if (guildId != null && userId != null && duration > 0) {
        String userStatsSql = """
            INSERT INTO user_listening_stats (guild_id, user_id, total_listening_seconds, last_updated)
            VALUES (?, ?, ?, strftime('%s', 'now'))
            ON CONFLICT(guild_id, user_id) DO UPDATE SET
              total_listening_seconds = total_listening_seconds + ?,
              last_updated = strftime('%s', 'now')
            """;
        try (PreparedStatement stmt = conn.prepareStatement(userStatsSql)) {
          stmt.setString(1, guildId);
          stmt.setString(2, userId);
          stmt.setLong(3, duration);
          stmt.setLong(4, duration);
          stmt.executeUpdate();
        }
      }
    } catch (SQLException e) {
      logger.error("Failed to end playback session", e);
    }
  }

  /**
   * Gets user listening time for a guild.
   *
   * @param guildId The guild ID
   * @param userId  The user ID
   * @return Total listening seconds
   */
  public long getUserListeningTime(String guildId, String userId) {
    try (Connection conn = getConnection()) {
      String sql = "SELECT total_listening_seconds FROM user_listening_stats WHERE guild_id = ? AND user_id = ?";
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, guildId);
        stmt.setString(2, userId);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return rs.getLong("total_listening_seconds");
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Failed to get user listening time", e);
    }
    return 0;
  }

  /**
   * Gets top listeners for a guild.
   *
   * @param guildId The guild ID
   * @param limit   Number of top listeners to return
   * @return List of maps with user_id and total_listening_seconds
   */
  public List<Map<String, Object>> getTopListeners(String guildId, int limit) {
    List<Map<String, Object>> results = new ArrayList<>();
    try (Connection conn = getConnection()) {
      String sql = """
          SELECT user_id, total_listening_seconds
          FROM user_listening_stats
          WHERE guild_id = ?
          ORDER BY total_listening_seconds DESC
          LIMIT ?
          """;
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, guildId);
        stmt.setInt(2, limit);
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("user_id", rs.getString("user_id"));
            entry.put("total_listening_seconds", rs.getLong("total_listening_seconds"));
            results.add(entry);
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Failed to get top listeners", e);
    }
    return results;
  }

  /**
   * Gets the volume setting for a guild.
   *
   * @param guildId The guild ID
   * @return Volume (0-100), default 50 if not set
   */
  public int getGuildVolume(String guildId) {
    try (Connection conn = getConnection()) {
      String sql = "SELECT volume FROM guild_volume_settings WHERE guild_id = ?";
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, guildId);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return rs.getInt("volume");
          }
        }
      }
    } catch (SQLException e) {
      logger.error("Failed to get guild volume", e);
    }
    return 50; // Default volume
  }

  /**
   * Sets the volume setting for a guild.
   *
   * @param guildId The guild ID
   * @param volume  Volume (0-100)
   */
  public void setGuildVolume(String guildId, int volume) {
    // Clamp volume to 0-100
    volume = Math.max(0, Math.min(100, volume));

    try (Connection conn = getConnection()) {
      String sql = """
          INSERT INTO guild_volume_settings (guild_id, volume, last_updated)
          VALUES (?, ?, strftime('%s', 'now'))
          ON CONFLICT(guild_id) DO UPDATE SET
            volume = ?,
            last_updated = strftime('%s', 'now')
          """;
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, guildId);
        stmt.setInt(2, volume);
        stmt.setInt(3, volume);
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      logger.error("Failed to set guild volume", e);
    }
  }

  /**
   * Closes the database connection.
   */
  public void close() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException e) {
      logger.error("Failed to close database connection", e);
    }
  }
}
