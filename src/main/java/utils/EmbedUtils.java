package utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.time.Instant;

/**
 * Utility class for creating beautiful embeds with consistent styling.
 */
public class EmbedUtils {
  // Default colors
  public static final Color COLOR_SUCCESS = new Color(46, 204, 113); // Green
  public static final Color COLOR_ERROR = new Color(231, 76, 60); // Red
  public static final Color COLOR_INFO = new Color(52, 152, 219); // Blue
  public static final Color COLOR_WARNING = new Color(241, 196, 15); // Yellow
  public static final Color COLOR_MUSIC = new Color(155, 89, 182); // Purple

  /**
   * Creates a success embed with green color.
   *
   * @param description The description text
   * @param user        The user who triggered the command (for footer)
   * @return MessageEmbed
   */
  @NotNull
  public static MessageEmbed createSuccessEmbed(@NotNull String description, @Nullable User user) {
    return createEmbed(description, COLOR_SUCCESS, user);
  }

  /**
   * Creates an error embed with red color.
   *
   * @param description The description text
   * @param user        The user who triggered the command (for footer)
   * @return MessageEmbed
   */
  @NotNull
  public static MessageEmbed createErrorEmbed(@NotNull String description, @Nullable User user) {
    return createEmbed(description, COLOR_ERROR, user);
  }

  /**
   * Creates an info embed with blue color.
   *
   * @param description The description text
   * @param user        The user who triggered the command (for footer)
   * @return MessageEmbed
   */
  @NotNull
  public static MessageEmbed createInfoEmbed(@NotNull String description, @Nullable User user) {
    return createEmbed(description, COLOR_INFO, user);
  }

  /**
   * Creates a warning embed with yellow color.
   *
   * @param description The description text
   * @param user        The user who triggered the command (for footer)
   * @return MessageEmbed
   */
  @NotNull
  public static MessageEmbed createWarningEmbed(@NotNull String description, @Nullable User user) {
    return createEmbed(description, COLOR_WARNING, user);
  }

  /**
   * Creates a music embed with purple color.
   *
   * @param description The description text
   * @param user        The user who triggered the command (for footer)
   * @return MessageEmbed
   */
  @NotNull
  public static MessageEmbed createMusicEmbed(@NotNull String description, @Nullable User user) {
    return createEmbed(description, COLOR_MUSIC, user);
  }

  /**
   * Creates a custom embed with specified color.
   *
   * @param description The description text
   * @param color       The embed color
   * @param user        The user who triggered the command (for footer)
   * @return MessageEmbed
   */
  @NotNull
  public static MessageEmbed createEmbed(@NotNull String description, @NotNull Color color, @Nullable User user) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setDescription(description);
    embed.setColor(color);
    embed.setTimestamp(Instant.now());

    if (user != null) {
      embed.setFooter(user.getName(), user.getEffectiveAvatarUrl());
    }

    return embed.build();
  }

  /**
   * Creates a custom embed with title and description.
   *
   * @param title       The embed title
   * @param description The description text
   * @param color       The embed color
   * @param user        The user who triggered the command (for footer)
   * @return MessageEmbed
   */
  @NotNull
  public static MessageEmbed createEmbed(@NotNull String title, @NotNull String description, @NotNull Color color,
      @Nullable User user) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle(title);
    embed.setDescription(description);
    embed.setColor(color);
    embed.setTimestamp(Instant.now());

    if (user != null) {
      embed.setFooter(user.getName(), user.getEffectiveAvatarUrl());
    }

    return embed.build();
  }

  /**
   * Creates a custom embed with bot's avatar in footer.
   *
   * @param description The description text
   * @param color       The embed color
   * @param botUser     The bot user (for footer)
   * @return MessageEmbed
   */
  @NotNull
  public static MessageEmbed createBotEmbed(@NotNull String description, @NotNull Color color, @NotNull User botUser) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setDescription(description);
    embed.setColor(color);
    embed.setTimestamp(Instant.now());
    embed.setFooter(botUser.getName(), botUser.getEffectiveAvatarUrl());

    return embed.build();
  }

  /**
   * Creates a custom embed with bot's avatar and custom footer text.
   *
   * @param description The description text
   * @param color       The embed color
   * @param footerText  Custom footer text
   * @param botUser     The bot user (for footer icon)
   * @return MessageEmbed
   */
  @NotNull
  public static MessageEmbed createBotEmbed(@NotNull String description, @NotNull Color color,
      @NotNull String footerText,
      @NotNull User botUser) {
    EmbedBuilder embed = new EmbedBuilder();
    embed.setDescription(description);
    embed.setColor(color);
    embed.setTimestamp(Instant.now());
    embed.setFooter(footerText, botUser.getEffectiveAvatarUrl());

    return embed.build();
  }
}
