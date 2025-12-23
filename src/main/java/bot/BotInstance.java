package bot;

import managers.CommandManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single bot instance.
 */
public class BotInstance extends ListenerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(BotInstance.class);
  private final BotInstanceConfig config;
  private final CommandManager commandManager;
  private JDA jda;

  public BotInstance(BotInstanceConfig config) {
    this.config = config;
    this.commandManager = new CommandManager(config.getPrefix());
  }

  /**
   * Initializes and starts this bot instance.
   *
   * @throws InterruptedException if interrupted while waiting for JDA to be ready
   */
  public void initialize() throws InterruptedException {
    logger.info("Initializing bot instance: {}", config.getName());
    logger.info("Message command prefix: {}", config.getPrefix());

    jda = JdaBuilderHelper.build(config.getToken(), commandManager, config.getPrefix(), config.getActivity(), this);
    
    // Add event listener for guild ready events to register with cluster
    jda.addEventListener(this);
    
    jda.awaitReady();

    logger.info("Bot instance '{}' is ready! Logged in as: {}", config.getName(), jda.getSelfUser().getAsTag());
    CommandRegistrar.register(jda, commandManager);
    
    // Register all guilds with cluster
    jda.getGuilds().forEach(guild -> {
      BotCluster cluster = BotCluster.getInstance();
      if (cluster != null) {
        cluster.shouldRespond(this, guild.getIdLong());
      }
    });
  }

  @Override
  public void onGuildReady(@NotNull net.dv8tion.jda.api.events.guild.GuildReadyEvent event) {
    BotCluster cluster = BotCluster.getInstance();
    if (cluster != null) {
      cluster.shouldRespond(this, event.getGuild().getIdLong());
    }
  }

  /**
   * Gets the JDA instance for this bot.
   *
   * @return JDA instance
   */
  public JDA getJda() {
    return jda;
  }

  /**
   * Gets the command manager for this bot instance.
   *
   * @return CommandManager instance
   */
  public CommandManager getCommandManager() {
    return commandManager;
  }

  /**
   * Gets the configuration for this bot instance.
   *
   * @return BotInstanceConfig
   */
  public BotInstanceConfig getConfig() {
    return config;
  }

  /**
   * Checks if this bot instance is healthy.
   *
   * @return true if healthy, false otherwise
   */
  public boolean isHealthy() {
    try {
      if (jda == null) {
        return false;
      }
      net.dv8tion.jda.api.JDA.Status status = jda.getStatus();
      return status == net.dv8tion.jda.api.JDA.Status.CONNECTED ||
          status == net.dv8tion.jda.api.JDA.Status.INITIALIZED ||
          status == net.dv8tion.jda.api.JDA.Status.LOADING_SUBSYSTEMS;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Shuts down this bot instance.
   */
  public void shutdown() {
    if (jda != null) {
      logger.info("Shutting down bot instance: {}", config.getName());
      jda.shutdown();
    }
  }
}

