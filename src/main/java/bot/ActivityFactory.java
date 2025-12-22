package bot;

import net.dv8tion.jda.api.entities.Activity;

/**
 * Factory class for creating Activity objects.
 */
public class ActivityFactory {
  /**
   * Creates an Activity object from configuration.
   *
   * @param config The configuration instance (from default package)
   * @return Activity object
   */
  @SuppressWarnings("all")
  public static Activity createFromConfig(Object config) {
    try {
      java.lang.reflect.Method getActivityType = config.getClass().getMethod("getActivityType");
      java.lang.reflect.Method getActivityName = config.getClass().getMethod("getActivityName");
      String activityType = (String) getActivityType.invoke(config);
      String activityName = (String) getActivityName.invoke(config);
      return create(activityType, activityName);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create activity from config", e);
    }
  }

  /**
   * Creates an Activity object based on the activity type and name.
   *
   * @param activityType The type of activity (WATCHING, PLAYING, LISTENING, etc.)
   * @param activityName The name of the activity
   * @return Activity object
   */
  public static Activity create(String activityType, String activityName) {
    if (activityName == null || activityName.isEmpty()) {
      activityName = "for slash commands";
    }

    return switch (activityType.toUpperCase()) {
      case "PLAYING" -> Activity.playing(activityName);
      case "LISTENING" -> Activity.listening(activityName);
      case "STREAMING" -> Activity.streaming(activityName, null);
      case "COMPETING" -> Activity.competing(activityName);
      default -> Activity.watching(activityName);
    };
  }
}

