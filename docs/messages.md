# Messages

Edit `plugins/RankedSMPX/messages.yml` to change in-game wording. Keep the YAML keys and indentation.

New messages added by an update are appended to your file automatically, with your edits and comments kept. A copy of the old file goes to `backups/`. A message that is missing uses the built-in text.

```yaml
settings:
  prefix: "&bRankedSMPX &8» &r"

general:
  reload-success: "{prefix}&aConfiguration reloaded successfully!"
  player-not-found: "{prefix}&cPlayer '{player}' not found!"
```

## Formatting

Messages accept legacy color codes such as `&a`, `&l`, and hex colors like `&#FF5555`. MiniMessage tags are also supported, for example `<green>Ready</green>` or `<gradient:#FFD700:#FF8C00>Champion</gradient>`.

`small-caps-messages` in `config.yml` applies small-cap characters while retaining supported colors and formatting. Set it to `false` to keep the text unchanged.

## Placeholders

Placeholders depend on the message. Common ones include `{prefix}`, `{player}`, `{rank}`, `{old_rank}`, `{rank_prefix}`, `{time}`, `{count}`, and `{total}`. Keep placeholders already present in a message when rewriting it; check the command's default message in `messages.yml` for the exact names supported there.

The prefix only shows when `message-prefix: true` is set in `config.yml`. Titles can be turned off for everyone with `titles: false` in `config.yml`.

Run `/rsmp reload` after editing. Back up `messages.yml` before large changes.
