package bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates bot token from configuration.
 */
public class TokenValidator {
  private static final Logger logger = LoggerFactory.getLogger(TokenValidator.class);

  /**
   * Validates the bot token from configuration.
   *
   * @param config The configuration instance (from default package)
   * @return The validated token
   * @throws IllegalStateException if token is missing or empty
   */
  @SuppressWarnings("all")
  public static String validate(Object config) {
    try {
      java.lang.reflect.Method getToken = config.getClass().getMethod("getDiscordBotToken");
      String token = (String) getToken.invoke(config);

      if (token == null || token.isEmpty()) {
        logger.error("DISCORD_BOT_TOKEN is required!");
        logger.error("Please set your bot token in one of the following ways:");
        logger.error("  1. Create a .env file with DISCORD_BOT_TOKEN=your_token_here");
        logger.error("  2. Set environment variable: export DISCORD_BOT_TOKEN=your_token_here");
        logger.error("  3. Set in application.properties: discord.bot.token=your_token_here");
        logger.error("  (Copy .env.example to .env and fill in your token)");
        System.exit(1);
      }

      return token;
    } catch (Exception e) {
      logger.error("Failed to validate token", e);
      System.exit(1);
      return null;
    }
  }
}

