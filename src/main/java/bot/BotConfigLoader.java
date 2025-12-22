package bot;

import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads bot configurations for multi-instance mode.
 */
public class BotConfigLoader {
  private static final Logger logger = LoggerFactory.getLogger(BotConfigLoader.class);

  /**
   * Loads bot instance configurations from Config.
   *
   * @param config The configuration instance (from default package)
   * @return List of BotInstanceConfig
   */
  @SuppressWarnings("all")
  public static List<BotInstanceConfig> loadConfigs(Object config) {
    List<BotInstanceConfig> configs = new ArrayList<>();

    try {
      // Get bot tokens
      java.lang.reflect.Method getBotTokens = config.getClass().getMethod("getBotTokens");
      String[] tokens = (String[]) getBotTokens.invoke(config);

      if (tokens == null || tokens.length == 0) {
        logger.warn("No bot tokens found. Please configure DISCORD_BOT_TOKEN or DISCORD_BOT_TOKENS");
        return configs;
      }

      logger.info("Found {} bot token(s)", tokens.length);

      // Load config for each token
      for (int i = 0; i < tokens.length; i++) {
        String token = tokens[i].trim();
        if (token.isEmpty()) {
          continue;
        }

        int index = i + 1;
        String name = "Bot-" + index;

        // Get activity type and name for this instance
        java.lang.reflect.Method getActivityType = config.getClass().getMethod("getActivityType", int.class);
        java.lang.reflect.Method getActivityName = config.getClass().getMethod("getActivityName", int.class);
        String activityType = (String) getActivityType.invoke(config, index);
        String activityName = (String) getActivityName.invoke(config, index);

        // Get prefix for this instance
        java.lang.reflect.Method getPrefix = config.getClass().getMethod("getCommandPrefix", int.class);
        String prefix = (String) getPrefix.invoke(config, index);

        Activity activity = ActivityFactory.create(activityType, activityName);
        BotInstanceConfig botConfig = new BotInstanceConfig(token, prefix, activity, name);
        configs.add(botConfig);

        logger.info("Loaded config for {}: prefix={}, activity={} {}", name, prefix, activityType, activityName);
      }

    } catch (Exception e) {
      logger.error("Failed to load bot configurations", e);
      throw new RuntimeException("Failed to load bot configurations", e);
    }

    return configs;
  }
}

