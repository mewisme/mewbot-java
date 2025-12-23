-- Add volume settings table

CREATE TABLE IF NOT EXISTS guild_volume_settings (
    guild_id TEXT NOT NULL PRIMARY KEY,
    volume INTEGER NOT NULL DEFAULT 50,
    last_updated INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

