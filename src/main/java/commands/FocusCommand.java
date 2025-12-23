package commands;

import managers.VoiceManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import utils.EmbedUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Command to mute all users in the voice channel (focus mode - only bot plays
 * music).
 */
public class FocusCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(FocusCommand.class);

  @Override
  @NotNull
  public SlashCommandData getCommandData() {
    return Commands.slash("focus", "Mute all users in voice channel (focus mode - only bot plays music)");
  }

  @Override
  public void execute(@NotNull SlashCommandInteractionEvent event) {
    if (!event.isFromGuild()) {
      event.replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    // Check if user has permission to mute members
    Member member = event.getMember();
    if (member == null || !member.hasPermission(Permission.VOICE_MUTE_OTHERS)) {
      event
          .replyEmbeds(EmbedUtils.createErrorEmbed("You need the 'Mute Members' permission to use this command!",
              event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    VoiceManager voiceManager = VoiceManager.getInstance();

    // Check if bot is connected to a voice channel
    if (!voiceManager.isConnected(event.getGuild())) {
      event.replyEmbeds(EmbedUtils.createWarningEmbed("Bot is not connected to any voice channel!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    // Get bot's voice channel
    VoiceChannel botChannel = event.getGuild().getSelfMember().getVoiceState().getChannel().asVoiceChannel();
    if (botChannel == null) {
      event.replyEmbeds(EmbedUtils.createErrorEmbed("Unable to get bot's voice channel!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    // Get all members in the channel (except bots)
    List<Member> membersToMute = botChannel.getMembers().stream()
        .filter(m -> !m.getUser().isBot())
        .collect(Collectors.toList());

    if (membersToMute.isEmpty()) {
      event
          .replyEmbeds(
              EmbedUtils.createInfoEmbed("🔇 **Focus Mode**\n\nNo other users in the voice channel.", event.getUser()))
          .setEphemeral(false).queue();
      return;
    }

    // Defer reply to allow time for mute operations
    event.deferReply().queue();

    // Mute all members in the channel
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failedCount = new AtomicInteger(0);

    List<CompletableFuture<Void>> muteFutures = membersToMute.stream()
        .map(channelMember -> {
          CompletableFuture<Void> future = new CompletableFuture<>();
          event.getGuild().mute(channelMember, true).queue(
              success -> {
                logger.debug("Muted member {} in guild {}", channelMember.getUser().getAsTag(),
                    event.getGuild().getName());
                successCount.incrementAndGet();
                future.complete(null);
              },
              error -> {
                logger.warn("Failed to mute member {} in guild {}: {}",
                    channelMember.getUser().getAsTag(), event.getGuild().getName(), error.getMessage());
                failedCount.incrementAndGet();
                future.complete(null);
              });
          return future;
        })
        .collect(Collectors.toList());

    // Wait for all mute operations to complete, then reply
    CompletableFuture.allOf(muteFutures.toArray(new CompletableFuture[0]))
        .thenRun(() -> {
          String message = String.format("🔇 **Focus Mode Activated**\n\n✅ Muted %d user(s)", successCount.get());
          if (failedCount.get() > 0) {
            message += String.format("\n⚠️ Failed to mute %d user(s)", failedCount.get());
          }
          message += "\n\nOnly the bot will play music now.";

          event.getHook().editOriginalEmbeds(EmbedUtils.createSuccessEmbed(message, event.getUser()))
              .queue();
        });
  }

  @Override
  public void executeMessage(@NotNull MessageReceivedEvent event, @NotNull String args) {
    if (!event.isFromGuild()) {
      event.getMessage()
          .replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!", event.getAuthor()))
          .queue();
      return;
    }

    // Check if user has permission to mute members
    Member member = event.getMember();
    if (member == null || !member.hasPermission(Permission.VOICE_MUTE_OTHERS)) {
      event.getMessage()
          .replyEmbeds(EmbedUtils.createErrorEmbed("You need the 'Mute Members' permission to use this command!",
              event.getAuthor()))
          .queue();
      return;
    }

    VoiceManager voiceManager = VoiceManager.getInstance();

    // Check if bot is connected to a voice channel
    if (!voiceManager.isConnected(event.getGuild())) {
      event.getMessage()
          .replyEmbeds(EmbedUtils.createWarningEmbed("Bot is not connected to any voice channel!", event.getAuthor()))
          .queue();
      return;
    }

    // Get bot's voice channel
    VoiceChannel botChannel = event.getGuild().getSelfMember().getVoiceState().getChannel().asVoiceChannel();
    if (botChannel == null) {
      event.getMessage()
          .replyEmbeds(EmbedUtils.createErrorEmbed("Unable to get bot's voice channel!", event.getAuthor()))
          .queue();
      return;
    }

    // Get all members in the channel (except bots)
    List<Member> membersToMute = botChannel.getMembers().stream()
        .filter(m -> !m.getUser().isBot())
        .collect(Collectors.toList());

    if (membersToMute.isEmpty()) {
      event.getMessage()
          .replyEmbeds(EmbedUtils.createInfoEmbed("🔇 **Focus Mode**\n\nNo other users in the voice channel.",
              event.getAuthor()))
          .queue();
      return;
    }

    // Send initial reply
    event.getMessage()
        .replyEmbeds(EmbedUtils.createInfoEmbed("🔇 **Focus Mode**\n\nMuting users...", event.getAuthor()))
        .queue(replyMessage -> {
          // Mute all members in the channel
          AtomicInteger successCount = new AtomicInteger(0);
          AtomicInteger failedCount = new AtomicInteger(0);

          List<CompletableFuture<Void>> muteFutures = membersToMute.stream()
              .map(channelMember -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                event.getGuild().mute(channelMember, true).queue(
                    success -> {
                      logger.debug("Muted member {} in guild {}", channelMember.getUser().getAsTag(),
                          event.getGuild().getName());
                      successCount.incrementAndGet();
                      future.complete(null);
                    },
                    error -> {
                      logger.warn("Failed to mute member {} in guild {}: {}",
                          channelMember.getUser().getAsTag(), event.getGuild().getName(), error.getMessage());
                      failedCount.incrementAndGet();
                      future.complete(null);
                    });
                return future;
              })
              .collect(Collectors.toList());

          // Wait for all mute operations to complete, then update reply
          CompletableFuture.allOf(muteFutures.toArray(new CompletableFuture[0]))
              .thenRun(() -> {
                String message = String.format("🔇 **Focus Mode Activated**\n\n✅ Muted %d user(s)", successCount.get());
                if (failedCount.get() > 0) {
                  message += String.format("\n⚠️ Failed to mute %d user(s)", failedCount.get());
                }
                message += "\n\nOnly the bot will play music now.";

                replyMessage.editMessageEmbeds(EmbedUtils.createSuccessEmbed(message, event.getAuthor()))
                    .queue();
              });
        });
  }
}
