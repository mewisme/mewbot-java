package bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages a cluster of bot instances with primary/secondary failover mechanism.
 * Only the primary bot responds to commands in each guild.
 */
public class BotCluster {
  private static final Logger logger = LoggerFactory.getLogger(BotCluster.class);
  private static BotCluster instance;
  private final List<BotInstance> instances;
  private final Map<Long, BotInstance> primaryBots; // Guild ID -> Primary BotInstance
  private final Map<BotInstance, Set<Long>> botGuilds; // BotInstance -> Set of Guild IDs
  private final ScheduledExecutorService healthCheckExecutor;
  private static final long HEALTH_CHECK_INTERVAL_SECONDS = 30;

  private BotCluster(List<BotInstance> instances) {
    this.instances = new ArrayList<>(instances);
    this.primaryBots = new ConcurrentHashMap<>();
    this.botGuilds = new ConcurrentHashMap<>();
    this.healthCheckExecutor = Executors.newScheduledThreadPool(1);

    // Initialize bot guilds mapping
    for (BotInstance instance : instances) {
      botGuilds.put(instance, ConcurrentHashMap.newKeySet());
    }

    // Start health check
    startHealthCheck();
  }

  /**
   * Gets or creates the BotCluster instance.
   *
   * @param instances List of bot instances
   * @return BotCluster instance
   */
  public static BotCluster getInstance(List<BotInstance> instances) {
    if (instance == null) {
      instance = new BotCluster(instances);
    }
    return instance;
  }

  /**
   * Gets the singleton BotCluster instance.
   *
   * @return BotCluster instance or null if not initialized
   */
  public static BotCluster getInstance() {
    return instance;
  }

  /**
   * Checks if this bot instance should respond to a command in the given guild.
   * Only the primary bot for each guild should respond.
   *
   * @param botInstance The bot instance checking
   * @param guildId     The guild ID (null for DM)
   * @return true if this bot should respond, false otherwise
   */
  public boolean shouldRespond(BotInstance botInstance, Long guildId) {
    if (guildId == null) {
      // DMs: all bots can respond
      return true;
    }

    // Check if this bot is healthy first
    if (!isHealthy(botInstance)) {
      return false;
    }

    BotInstance primary = primaryBots.get(guildId);
    if (primary == null) {
      // No primary set yet, elect this bot as primary
      electPrimary(botInstance, guildId);
      return true;
    }

    // Check if primary is still healthy
    if (!isHealthy(primary)) {
      logger.warn("Primary bot for guild {} is unhealthy, electing new primary", guildId);
      electPrimary(botInstance, guildId);
      // Check if this bot was elected as primary
      return primaryBots.get(guildId).equals(botInstance);
    }

    return primary.equals(botInstance);
  }

  /**
   * Elects a primary bot for a guild.
   * Prefers the requesting bot if healthy, otherwise finds the first healthy bot.
   *
   * @param botInstance The bot instance requesting to be primary
   * @param guildId     The guild ID
   */
  private synchronized void electPrimary(BotInstance botInstance, Long guildId) {
    BotInstance currentPrimary = primaryBots.get(guildId);

    // If current primary is healthy, don't change
    if (currentPrimary != null && isHealthy(currentPrimary)) {
      return;
    }

    // Prefer the requesting bot if it's healthy
    BotInstance newPrimary = null;
    if (isHealthy(botInstance)) {
      newPrimary = botInstance;
    } else {
      // Find the first healthy bot instance
      newPrimary = findHealthyBot();
    }

    if (newPrimary == null) {
      logger.error("No healthy bot instances available for guild {}", guildId);
      return;
    }

    primaryBots.put(guildId, newPrimary);
    botGuilds.get(newPrimary).add(guildId);

    if (currentPrimary != null && !currentPrimary.equals(newPrimary)) {
      botGuilds.get(currentPrimary).remove(guildId);
      logger.info("Elected bot '{}' as primary for guild {} (replaced '{}')", 
          newPrimary.getConfig().getName(), guildId, 
          currentPrimary.getConfig().getName());
    } else if (currentPrimary == null) {
      logger.info("Elected bot '{}' as primary for guild {}", newPrimary.getConfig().getName(), guildId);
    }
  }

  /**
   * Finds a healthy bot instance.
   *
   * @return Healthy BotInstance or null if none available
   */
  private BotInstance findHealthyBot() {
    for (BotInstance instance : instances) {
      if (isHealthy(instance)) {
        return instance;
      }
    }
    return null;
  }

  /**
   * Checks if a bot instance is healthy.
   *
   * @param botInstance The bot instance to check
   * @return true if healthy, false otherwise
   */
  private boolean isHealthy(BotInstance botInstance) {
    try {
      if (botInstance == null) {
        return false;
      }

      net.dv8tion.jda.api.JDA jda = botInstance.getJda();
      if (jda == null) {
        return false;
      }

      // Check if JDA is ready and connected
      net.dv8tion.jda.api.JDA.Status status = jda.getStatus();
      return status == net.dv8tion.jda.api.JDA.Status.CONNECTED ||
          status == net.dv8tion.jda.api.JDA.Status.INITIALIZED ||
          status == net.dv8tion.jda.api.JDA.Status.LOADING_SUBSYSTEMS;
    } catch (Exception e) {
      logger.debug("Error checking bot health", e);
      return false;
    }
  }

  /**
   * Starts periodic health check for all bot instances.
   */
  private void startHealthCheck() {
    healthCheckExecutor.scheduleAtFixedRate(() -> {
      try {
        checkAndReelectPrimaries();
      } catch (Exception e) {
        logger.error("Error during health check", e);
      }
    }, HEALTH_CHECK_INTERVAL_SECONDS, HEALTH_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  /**
   * Checks all primary bots and re-elects if necessary.
   */
  private void checkAndReelectPrimaries() {
    Set<Long> guildsToReelect = new HashSet<>();

    for (Map.Entry<Long, BotInstance> entry : primaryBots.entrySet()) {
      Long guildId = entry.getKey();
      BotInstance primary = entry.getValue();

      if (!isHealthy(primary)) {
        guildsToReelect.add(guildId);
      }
    }

    // Re-elect primaries for unhealthy guilds
    for (Long guildId : guildsToReelect) {
      BotInstance newPrimary = findHealthyBot();
      if (newPrimary != null) {
        primaryBots.put(guildId, newPrimary);
        logger.info("Re-elected bot '{}' as primary for guild {} due to health check",
            newPrimary.getConfig().getName(), guildId);
      } else {
        logger.error("No healthy bot available for guild {}", guildId);
      }
    }
  }

  /**
   * Gets the primary bot for a guild.
   *
   * @param guildId The guild ID
   * @return Primary BotInstance or null if not found
   */
  public BotInstance getPrimaryBot(Long guildId) {
    return primaryBots.get(guildId);
  }

  /**
   * Gets all bot instances in the cluster.
   *
   * @return List of BotInstance
   */
  public List<BotInstance> getInstances() {
    return new ArrayList<>(instances);
  }

  /**
   * Shuts down the cluster and all bot instances.
   */
  public void shutdown() {
    logger.info("Shutting down bot cluster...");
    healthCheckExecutor.shutdown();
    try {
      if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        healthCheckExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      healthCheckExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    for (BotInstance instance : instances) {
      instance.shutdown();
    }
  }
}

