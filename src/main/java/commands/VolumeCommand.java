package commands;

import managers.VoiceManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import utils.EmbedUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Command to control audio volume.
 */
public class VolumeCommand extends Command {
  @Override
  @NotNull
  public SlashCommandData getCommandData() {
    return Commands.slash("volume", "Set or check the audio volume (0-100)")
        .addOption(OptionType.INTEGER, "level", "Volume level (0-100). Leave empty to check current volume.", false)
        .addOption(OptionType.STRING, "action", "Action: 'up' to increase, 'down' to decrease. Leave empty to set specific level.", false);
  }

  @Override
  public void execute(@NotNull SlashCommandInteractionEvent event) {
    if (!event.isFromGuild()) {
      event.replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    VoiceManager voiceManager = VoiceManager.getInstance();

    // Check if bot is playing music
    if (!voiceManager.isConnected(event.getGuild())) {
      event.replyEmbeds(EmbedUtils.createWarningEmbed("Bot is not playing music in any voice channel!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    Integer levelOption = event.getOption("level", null, opt -> opt.getAsInt());
    String actionOption = event.getOption("action", null, opt -> opt.getAsString());

    int currentVolume = voiceManager.getVolume(event.getGuild());
    int newVolume = currentVolume;

    if (levelOption != null) {
      // Set specific volume level
      newVolume = levelOption;
    } else if (actionOption != null) {
      // Handle up/down actions
      String action = actionOption.toLowerCase();
      if ("up".equals(action)) {
        newVolume = Math.min(100, currentVolume + 10);
      } else if ("down".equals(action)) {
        newVolume = Math.max(0, currentVolume - 10);
      } else {
        event.replyEmbeds(EmbedUtils.createErrorEmbed("Invalid action! Use 'up' or 'down'.", event.getUser()))
            .setEphemeral(true).queue();
        return;
      }
    } else {
      // No options provided, show current volume
      event.replyEmbeds(EmbedUtils.createInfoEmbed(
          String.format("🔊 **Current Volume: %d%%**", currentVolume),
          event.getUser())).setEphemeral(false).queue();
      return;
    }

    // Validate volume range
    if (newVolume < 0 || newVolume > 100) {
      event.replyEmbeds(EmbedUtils.createErrorEmbed("Volume must be between 0 and 100!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    // Set volume
    if (voiceManager.setVolume(event.getGuild(), newVolume)) {
      String message = String.format("🔊 **Volume set to %d%%**", newVolume);
      if (actionOption != null) {
        String action = actionOption.toLowerCase();
        if ("up".equals(action)) {
          message = String.format("🔊 **Volume increased to %d%%** (was %d%%)", newVolume, currentVolume);
        } else if ("down".equals(action)) {
          message = String.format("🔊 **Volume decreased to %d%%** (was %d%%)", newVolume, currentVolume);
        }
      }
      event.replyEmbeds(EmbedUtils.createSuccessEmbed(message, event.getUser()))
          .setEphemeral(false).queue();
    } else {
      event.replyEmbeds(EmbedUtils.createErrorEmbed("Failed to set volume!", event.getUser()))
          .setEphemeral(true).queue();
    }
  }

  @Override
  public void executeMessage(@NotNull MessageReceivedEvent event, @NotNull String args) {
    if (!event.isFromGuild()) {
      event.getMessage().replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!", event.getAuthor()))
          .queue();
      return;
    }

    VoiceManager voiceManager = VoiceManager.getInstance();

    // Check if bot is playing music
    if (!voiceManager.isConnected(event.getGuild())) {
      event.getMessage().replyEmbeds(EmbedUtils.createWarningEmbed("Bot is not playing music in any voice channel!", event.getAuthor()))
          .queue();
      return;
    }

    args = args.trim();
    int currentVolume = voiceManager.getVolume(event.getGuild());
    int newVolume = currentVolume;

    if (args.isEmpty()) {
      // No arguments, show current volume
      event.getMessage().replyEmbeds(EmbedUtils.createInfoEmbed(
          String.format("🔊 **Current Volume: %d%%**", currentVolume),
          event.getAuthor())).queue();
      return;
    }

    String lowerArgs = args.toLowerCase();
    if ("up".equals(lowerArgs)) {
      newVolume = Math.min(100, currentVolume + 10);
    } else if ("down".equals(lowerArgs)) {
      newVolume = Math.max(0, currentVolume - 10);
    } else {
      // Try to parse as number
      try {
        newVolume = Integer.parseInt(args);
      } catch (NumberFormatException e) {
        event.getMessage().replyEmbeds(EmbedUtils.createErrorEmbed(
            "Invalid volume! Use a number (0-100), 'up', or 'down'.", event.getAuthor())).queue();
        return;
      }
    }

    // Validate volume range
    if (newVolume < 0 || newVolume > 100) {
      event.getMessage().replyEmbeds(EmbedUtils.createErrorEmbed("Volume must be between 0 and 100!", event.getAuthor()))
          .queue();
      return;
    }

    // Set volume
    if (voiceManager.setVolume(event.getGuild(), newVolume)) {
      String message = String.format("🔊 **Volume set to %d%%**", newVolume);
      if ("up".equals(lowerArgs)) {
        message = String.format("🔊 **Volume increased to %d%%** (was %d%%)", newVolume, currentVolume);
      } else if ("down".equals(lowerArgs)) {
        message = String.format("🔊 **Volume decreased to %d%%** (was %d%%)", newVolume, currentVolume);
      }
      event.getMessage().replyEmbeds(EmbedUtils.createSuccessEmbed(message, event.getAuthor()))
          .queue();
    } else {
      event.getMessage().replyEmbeds(EmbedUtils.createErrorEmbed("Failed to set volume!", event.getAuthor()))
          .queue();
    }
  }
}

