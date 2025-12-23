-- Create tables for statistics tracking

-- Table to track music playback time per guild
CREATE TABLE IF NOT EXISTS guild_music_stats (
    guild_id TEXT NOT NULL PRIMARY KEY,
    total_playback_seconds INTEGER NOT NULL DEFAULT 0,
    last_updated INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

-- Table to track command usage per guild
CREATE TABLE IF NOT EXISTS guild_command_stats (
    guild_id TEXT NOT NULL,
    command_name TEXT NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0,
    last_used INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    PRIMARY KEY (guild_id, command_name)
);

-- Table to track user listening time per guild
CREATE TABLE IF NOT EXISTS user_listening_stats (
    guild_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    total_listening_seconds INTEGER NOT NULL DEFAULT 0,
    last_updated INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    PRIMARY KEY (guild_id, user_id)
);

-- Table to track active playback sessions
CREATE TABLE IF NOT EXISTS playback_sessions (
    session_id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    start_time INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    end_time INTEGER,
    duration_seconds INTEGER
);

-- Indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_guild_command_stats_guild ON guild_command_stats(guild_id);
CREATE INDEX IF NOT EXISTS idx_user_listening_stats_guild ON user_listening_stats(guild_id);
CREATE INDEX IF NOT EXISTS idx_user_listening_stats_user ON user_listening_stats(user_id);
CREATE INDEX IF NOT EXISTS idx_playback_sessions_guild ON playback_sessions(guild_id);
CREATE INDEX IF NOT EXISTS idx_playback_sessions_user ON playback_sessions(user_id);

