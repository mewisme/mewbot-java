import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Configuration manager that loads properties from application.properties
 * and .env file. Environment variables take precedence.
 */
public class Config {
  private static final Logger logger = LoggerFactory.getLogger(Config.class);
  private static Config instance;
  private final Properties properties;
  private Dotenv dotenv;

  private Config() {
    properties = new Properties();
    loadProperties();
    loadDotenv();
  }

  /**
   * Gets the singleton instance of Config.
   *
   * @return Config instance
   */
  public static Config getInstance() {
    if (instance == null) {
      instance = new Config();
    }
    return instance;
  }

  /**
   * Loads properties from application.properties file.
   */
  private void loadProperties() {
    try (InputStream input = getClass().getClassLoader()
        .getResourceAsStream("application.properties")) {
      if (input == null) {
        logger.warn("application.properties not found, using defaults");
        return;
      }
      properties.load(input);
    } catch (Exception e) {
      logger.error("Error loading application.properties", e);
    }
  }

  /**
   * Loads .env file if it exists.
   */
  private void loadDotenv() {
    try {
      dotenv = Dotenv.configure()
          .ignoreIfMissing()
          .load();
    } catch (Exception e) {
      logger.info(".env file not found, using environment variables and properties");
    }
  }

  /**
   * Gets a property value with the following priority:
   * 1. Environment variable
   * 2. .env file
   * 3. application.properties
   * 4. Default value
   *
   * @param key          The property key
   * @param defaultValue Default value if not found
   * @return The property value
   */
  public String getProperty(String key, String defaultValue) {
    // First check environment variable
    String envValue = System.getenv(key);
    if (envValue != null && !envValue.isEmpty()) {
      return envValue;
    }

    // Then check .env file
    if (dotenv != null) {
      String dotenvValue = dotenv.get(key);
      if (dotenvValue != null && !dotenvValue.isEmpty()) {
        return dotenvValue;
      }
    }

    // Then check application.properties (with variable substitution)
    String propValue = properties.getProperty(key);
    if (propValue != null && !propValue.isEmpty()) {
      // Replace ${VAR} with actual values
      propValue = substituteVariables(propValue);
      if (!propValue.isEmpty()) {
        return propValue;
      }
    }

    return defaultValue;
  }

  /**
   * Gets a property value.
   *
   * @param key The property key
   * @return The property value or null if not found
   */
  public String getProperty(String key) {
    return getProperty(key, null);
  }

  /**
   * Substitutes variables in the format ${VAR} with their values.
   *
   * @param value The string with variables
   * @return The string with substituted values
   */
  private String substituteVariables(String value) {
    if (value == null) {
      return null;
    }

    String result = value;
    int startIndex;
    while ((startIndex = result.indexOf("${")) != -1) {
      int endIndex = result.indexOf("}", startIndex);
      if (endIndex == -1) {
        break;
      }

      String varName = result.substring(startIndex + 2, endIndex);
      String varValue = getProperty(varName, "");

      result = result.substring(0, startIndex) + varValue + result.substring(endIndex + 1);
    }

    return result;
  }

  /**
   * Gets the Discord bot token.
   *
   * @return The bot token
   */
  public String getDiscordBotToken() {
    return getProperty("DISCORD_BOT_TOKEN", null);
  }

  /**
   * Gets the bot activity type.
   *
   * @return The activity type (WATCHING, PLAYING, etc.)
   */
  public String getActivityType() {
    return getProperty("DISCORD_BOT_ACTIVITY_TYPE",
        properties.getProperty("discord.bot.activity.type", "WATCHING"));
  }

  /**
   * Gets the bot activity name.
   *
   * @return The activity name
   */
  public String getActivityName() {
    return getProperty("DISCORD_BOT_ACTIVITY_NAME",
        properties.getProperty("discord.bot.activity.name", "for slash commands"));
  }

  /**
   * Gets the command prefix for message commands.
   *
   * @return The command prefix (default: "m/")
   */
  public String getCommandPrefix() {
    return getProperty("DISCORD_BOT_PREFIX",
        properties.getProperty("discord.bot.prefix", "m/"));
  }

  /**
   * Gets bot tokens for multi-instance mode.
   * Supports format: DISCORD_BOT_TOKENS=token1,token2,token3
   * or DISCORD_BOT_TOKEN_1, DISCORD_BOT_TOKEN_2, etc.
   *
   * @return Array of bot tokens, or null if not configured
   */
  public String[] getBotTokens() {
    // First try comma-separated list
    String tokensStr = getProperty("DISCORD_BOT_TOKENS", null);
    if (tokensStr != null && !tokensStr.isEmpty()) {
      return tokensStr.split(",");
    }

    // Then try numbered tokens (DISCORD_BOT_TOKEN_1, DISCORD_BOT_TOKEN_2, ...)
    List<String> tokens = new ArrayList<>();
    int index = 1;
    while (true) {
      String token = getProperty("DISCORD_BOT_TOKEN_" + index, null);
      if (token == null || token.isEmpty()) {
        break;
      }
      tokens.add(token);
      index++;
    }

    if (!tokens.isEmpty()) {
      return tokens.toArray(new String[0]);
    }

    // Fallback to single token
    String singleToken = getDiscordBotToken();
    if (singleToken != null && !singleToken.isEmpty()) {
      return new String[] { singleToken };
    }

    return null;
  }

  /**
   * Gets activity type for a specific bot instance.
   *
   * @param index Bot instance index (1-based)
   * @return Activity type
   */
  public String getActivityType(int index) {
    if (index == 1) {
      return getActivityType();
    }
    return getProperty("DISCORD_BOT_ACTIVITY_TYPE_" + index,
        getProperty("DISCORD_BOT_ACTIVITY_TYPE", "WATCHING"));
  }

  /**
   * Gets activity name for a specific bot instance.
   *
   * @param index Bot instance index (1-based)
   * @return Activity name
   */
  public String getActivityName(int index) {
    if (index == 1) {
      return getActivityName();
    }
    return getProperty("DISCORD_BOT_ACTIVITY_NAME_" + index,
        getProperty("DISCORD_BOT_ACTIVITY_NAME", "for slash commands"));
  }

  /**
   * Gets command prefix for a specific bot instance.
   *
   * @param index Bot instance index (1-based)
   * @return Command prefix
   */
  public String getCommandPrefix(int index) {
    if (index == 1) {
      return getCommandPrefix();
    }
    return getProperty("DISCORD_BOT_PREFIX_" + index,
        getProperty("DISCORD_BOT_PREFIX", "m/"));
  }
}
