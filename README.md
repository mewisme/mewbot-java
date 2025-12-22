# Discord Bot (JDA) - Multi-Instance Cluster Mode

A powerful Java Discord bot using JDA (Java Discord API) with support for slash commands, message commands, audio playback, and **multi-instance cluster mode with automatic failover**.

## Features

### Core Features
- **Slash Commands**: Modern Discord slash command support
- **Message Commands**: Traditional prefix-based commands (default: `m/`)
- **Audio Playback**: Play lofi music 24/7 in voice channels
- **Self-Deafen Mode**: Bot automatically deafens itself (shows crossed-out headphone icon)
- **Beautiful Embeds**: All messages use Discord embeds with timestamps, avatars, and custom colors
- **Multi-Instance Support**: Run multiple bot instances simultaneously
- **Cluster Mode**: Primary/secondary bot architecture with automatic failover
- **Health Monitoring**: Automatic health checks every 30 seconds
- **Zero-Downtime**: Automatic failover when primary bot fails

### Commands

| Command | Slash | Message | Description |
|---------|-------|---------|-------------|
| `/ping` | ✅ | `m/ping` | Check bot response time and latency |
| `/lofi` | ✅ | `m/lofi` | Play lofi music 24/7 in your voice channel |
| `/leave` | ✅ | `m/leave` | Make the bot leave the voice channel |

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- One or more Discord Bot Tokens ([How to create a Discord bot](https://discord.com/developers/applications))

## Setup

1. **Clone or navigate to this project**

2. **Get your Discord Bot Token(s)**:
   - Go to [Discord Developer Portal](https://discord.com/developers/applications)
   - Create a new application or select an existing one
   - Go to the "Bot" section
   - Copy your bot token
   - For multi-instance mode, create multiple bots and get their tokens

3. **Configure your bot** (create `.env` file):
   ```bash
   # Copy the example file
   cp .env.example .env
   
   # Edit .env and add your token(s)
   ```

4. **Invite your bot(s) to a server**:
   - In Discord Developer Portal, go to OAuth2 > URL Generator
   - Select scopes: `bot` and `applications.commands`
   - Select bot permissions: `Send Messages`, `Connect`, `Speak`, `Use Voice Activity`
   - Copy the generated URL and open it in your browser
   - Select a server and authorize
   - Repeat for each bot instance (if using multi-instance mode)

## Building and Running

### Quick Start

**1. Set up your configuration** (create `.env` file):
```bash
# Copy the example file
cp .env.example .env

# Edit .env and add your Discord bot token(s)
```

**2. Build the project:**
```bash
mvn clean package
```

**3. Run the bot:**

**Option 1: Run with Maven (Recommended for development):**
```bash
mvn exec:java
```

**Option 2: Run the JAR file:**
```bash
java -jar target/discord-bot-java-1.0-SNAPSHOT.jar
```

**Option 3: Run with environment variable (if not using .env):**
```bash
# Windows PowerShell
$env:DISCORD_BOT_TOKEN="your_token"; mvn exec:java

# Windows CMD
set DISCORD_BOT_TOKEN=your_token && mvn exec:java

# Linux/Mac
DISCORD_BOT_TOKEN=your_token mvn exec:java
```

### Build Commands Summary

| Command | Description |
|---------|-------------|
| `mvn clean package` | Build the project and create JAR file |
| `mvn exec:java` | Run the bot directly (no JAR needed) |
| `java -jar target/discord-bot-java-1.0-SNAPSHOT.jar` | Run the built JAR file |
| `mvn clean` | Clean build artifacts |
| `mvn compile` | Compile the project only |

## Configuration

The bot uses a flexible configuration system that supports multiple sources:

### Configuration Priority
1. Environment variables (highest priority)
2. `.env` file
3. `application.properties` file
4. Default values

### Single Bot Mode

For a single bot instance, use:

```env
DISCORD_BOT_TOKEN=your_bot_token_here
DISCORD_BOT_ACTIVITY_TYPE=WATCHING
DISCORD_BOT_ACTIVITY_NAME=for commands
DISCORD_BOT_PREFIX=m/
```

### Multi-Instance Mode (Cluster Mode)

Run multiple bots with automatic failover:

**Option 1: Comma-separated tokens (shared config)**
```env
DISCORD_BOT_TOKENS=token1,token2,token3
DISCORD_BOT_ACTIVITY_TYPE=WATCHING
DISCORD_BOT_ACTIVITY_NAME=Cluster Mode
DISCORD_BOT_PREFIX=m/
```

**Option 2: Numbered tokens (individual configs)**
```env
# Bot Instance 1
DISCORD_BOT_TOKEN_1=your_first_bot_token
DISCORD_BOT_ACTIVITY_TYPE_1=WATCHING
DISCORD_BOT_ACTIVITY_NAME_1=Primary Bot
DISCORD_BOT_PREFIX_1=m1/

# Bot Instance 2
DISCORD_BOT_TOKEN_2=your_second_bot_token
DISCORD_BOT_ACTIVITY_TYPE_2=PLAYING
DISCORD_BOT_ACTIVITY_NAME_2=Standby Bot
DISCORD_BOT_PREFIX_2=m2/

# Bot Instance 3
DISCORD_BOT_TOKEN_3=your_third_bot_token
DISCORD_BOT_ACTIVITY_TYPE_3=LISTENING
DISCORD_BOT_ACTIVITY_NAME_3=Standby Bot 2
DISCORD_BOT_PREFIX_3=m3/
```

### Available Configuration Options

| Property | Environment Variable | Description | Default |
|----------|---------------------|-------------|---------|
| `discord.bot.token` | `DISCORD_BOT_TOKEN` | Discord bot token (required) | - |
| `discord.bot.tokens` | `DISCORD_BOT_TOKENS` | Comma-separated bot tokens | - |
| `discord.bot.token.<n>` | `DISCORD_BOT_TOKEN_<n>` | Token for bot instance n | - |
| `discord.bot.activity.type` | `DISCORD_BOT_ACTIVITY_TYPE` | Activity type (WATCHING, PLAYING, LISTENING, STREAMING, COMPETING) | WATCHING |
| `discord.bot.activity.name` | `DISCORD_BOT_ACTIVITY_NAME` | Activity name/status text | "for slash commands" |
| `discord.bot.prefix` | `DISCORD_BOT_PREFIX` | Message command prefix | "m/" |

### Cluster Mode Behavior

When multiple bots are configured:

- **Primary Bot**: Only ONE bot (primary) responds to commands in each server
- **Automatic Failover**: If primary bot fails, another bot automatically takes over
- **Health Checks**: Runs every 30 seconds to monitor bot health
- **DM Support**: All bots can respond to DMs (no primary/secondary for DMs)
- **Zero Redundancy**: Prevents duplicate responses from multiple bots

## Project Structure

```
discord-bot-java/
├── .env.example               # Example .env file template
├── pom.xml                    # Maven configuration
├── README.md                  # This file
└── src/
    └── main/
        ├── java/
        │   ├── Main.java                    # Bot entry point
        │   ├── Config.java                  # Configuration manager
        │   ├── managers/
        │   │   ├── CommandManager.java      # Manages all commands
        │   │   └── VoiceManager.java        # Manages voice connections and audio
        │   ├── listeners/
        │   │   ├── SlashCommandListener.java    # Handles slash commands
        │   │   └── MessageCommandListener.java  # Handles message commands
        │   ├── commands/
        │   │   ├── Command.java             # Abstract base class for commands
        │   │   ├── PingCommand.java         # Ping command implementation
        │   │   ├── LofiCommand.java         # Lofi music command implementation
        │   │   └── LeaveCommand.java        # Leave voice channel command
        │   ├── bot/
        │   │   ├── BotInitializer.java      # Initializes bot instances
        │   │   ├── BotInstance.java         # Represents a single bot instance
        │   │   ├── BotInstanceConfig.java   # Configuration for a bot instance
        │   │   ├── BotCluster.java          # Manages cluster with failover
        │   │   ├── BotConfigLoader.java     # Loads bot configurations
        │   │   ├── TokenValidator.java       # Validates bot tokens
        │   │   ├── ActivityFactory.java     # Creates bot activities
        │   │   ├── JdaBuilderHelper.java    # Helps build JDA instances
        │   │   └── CommandRegistrar.java    # Registers slash commands
        │   └── utils/
        │       └── EmbedUtils.java          # Utility for creating embeds
        └── resources/
            ├── application.properties       # Default configuration
            └── logback.xml                  # Logging configuration
```

## Adding New Commands

Commands support both slash commands and message commands automatically.

1. **Create a new command class** in the `commands` package that extends `Command`:

```java
package commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import utils.EmbedUtils;
import org.jetbrains.annotations.NotNull;

public class MyCommand extends Command {
    @Override
    @NotNull
    public SlashCommandData getCommandData() {
        return Commands.slash("mycommand", "Description of my command")
            .addOption(OptionType.STRING, "optionname", "Option description", true)
            .setGuildOnly(false);
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        String option = event.getOption("optionname", null, opt -> opt.getAsString());
        event.replyEmbeds(EmbedUtils.createSuccessEmbed(
            "Response: " + option, event.getUser())).queue();
    }

    @Override
    public void executeMessage(@NotNull MessageReceivedEvent event, @NotNull String args) {
        // args contains everything after the command name
        event.getMessage().replyEmbeds(EmbedUtils.createSuccessEmbed(
            "Response: " + args, event.getAuthor())).queue();
    }
}
```

2. **Register the command** in `CommandManager.java`:

```java
public CommandManager() {
    // ... existing commands ...
    registerCommand(new MyCommand());
}
```

That's it! The command will be automatically registered for both slash commands and message commands.

## Features in Detail

### Message Commands

The bot supports traditional prefix-based commands alongside slash commands:

- Default prefix: `m/` (configurable via `DISCORD_BOT_PREFIX`)
- Example: `m/ping`, `m/lofi`, `m/leave`
- Each bot instance can have its own prefix in multi-instance mode

### Audio Playback

- **Lofi Music**: Play continuous lofi music stream
- **Self-Deafen**: Bot automatically deafens itself (shows crossed-out headphone icon)
- **24/7 Playback**: Bot stays connected until manually disconnected
- **Loop Support**: Automatic looping for continuous playback

### Embed Messages

All bot messages use Discord embeds with:
- Custom colors (success, error, info, warning, music)
- Timestamps
- User avatars in footer
- Consistent formatting

### Cluster Mode & Failover

**How it works:**
1. Multiple bot instances connect to Discord
2. For each server, one bot is elected as "primary"
3. Only the primary bot responds to commands in that server
4. Health checks run every 30 seconds
5. If primary bot fails, another healthy bot automatically takes over
6. DMs work on all bots (no primary/secondary restriction)

**Benefits:**
- Zero downtime: Automatic failover ensures service continuity
- Load distribution: Can distribute bots across different servers
- Redundancy: Multiple bots provide backup
- No duplicate responses: Only one bot responds per server

## Resources

- [JDA Documentation](https://jda.wiki/)
- [JDA GitHub](https://github.com/discord-jda/JDA)
- [Discord Developer Portal](https://discord.com/developers/applications)
- [Discord API Documentation](https://discord.com/developers/docs/intro)
- [LavaPlayer Documentation](https://github.com/sedmelluq/lavaplayer)

## License

This project uses JDA which is licensed under the Apache-2.0 License.
