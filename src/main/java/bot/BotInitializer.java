package bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Initializes and starts Discord bot(s) in single or multi-instance mode.
 */
public class BotInitializer {
  private static final Logger logger = LoggerFactory.getLogger(BotInitializer.class);
  private static final List<BotInstance> instances = new ArrayList<>();

  /**
   * Initializes and starts the Discord bot(s).
   * Supports both single-instance and multi-instance modes.
   *
   * @throws InterruptedException if interrupted while waiting for JDA to be ready
   */
  @SuppressWarnings("all")
  public static void initialize() throws InterruptedException {
    Object config;
    try {
      Class<?> configClass = Class.forName("Config");
      java.lang.reflect.Method getInstance = configClass.getMethod("getInstance");
      config = getInstance.invoke(null);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get Config instance", e);
    }

    // Load bot configurations
    List<BotInstanceConfig> botConfigs = BotConfigLoader.loadConfigs(config);

    if (botConfigs.isEmpty()) {
      logger.error("No bot configurations found!");
      System.exit(1);
      return;
    }

    logger.info("Starting {} bot instance(s)...", botConfigs.size());

    // Initialize all bot instances
    for (BotInstanceConfig botConfig : botConfigs) {
      BotInstance instance = new BotInstance(botConfig);
      instance.initialize();
      instances.add(instance);
    }

    // Initialize cluster if multiple instances
    if (instances.size() > 1) {
      BotCluster.getInstance(instances);
      logger.info("Bot cluster initialized with {} instances", instances.size());
    }

    logger.info("All {} bot instance(s) are ready!", instances.size());

    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutting down all bot instances...");
      BotCluster cluster = BotCluster.getInstance();
      if (cluster != null) {
        cluster.shutdown();
      } else {
        for (BotInstance instance : instances) {
          instance.shutdown();
        }
      }
    }));
  }

  /**
   * Gets all running bot instances.
   *
   * @return List of BotInstance
   */
  public static List<BotInstance> getInstances() {
    return new ArrayList<>(instances);
  }
}

