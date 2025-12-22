package bot;

import net.dv8tion.jda.api.entities.Activity;

/**
 * Configuration for a single bot instance.
 */
public class BotInstanceConfig {
  private final String token;
  private final String prefix;
  private final Activity activity;
  private final String name;

  public BotInstanceConfig(String token, String prefix, Activity activity, String name) {
    this.token = token;
    this.prefix = prefix;
    this.activity = activity;
    this.name = name;
  }

  public String getToken() {
    return token;
  }

  public String getPrefix() {
    return prefix;
  }

  public Activity getActivity() {
    return activity;
  }

  public String getName() {
    return name;
  }
}

