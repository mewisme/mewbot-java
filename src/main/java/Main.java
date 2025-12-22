import bot.BotInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Discord bot application.
 */
public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    try {
      BotInitializer.initialize();
    } catch (InterruptedException e) {
      logger.error("Interrupted while initializing bot", e);
      Thread.currentThread().interrupt();
    }
  }
}
