# Permissions

Paper grants OP by default the plugin's administrative permissions. `rankedsmp.admin` is the broad admin permission; use the narrower nodes below when delegating individual tasks.

| Permission | Purpose | Default |
|---|---|---|
| `rankedsmp.admin` | Administrative command access | OP |
| `rankedsmp.start` | Start a season | OP |
| `rankedsmp.stop` | Stop a season | OP |
| `rankedsmp.reload` | Reload plugin configuration | OP |
| `rankedsmp.gui` | Open admin rank menu | OP |
| `rankedsmp.set` | Set ranks | OP |
| `rankedsmp.remove` | Remove ranks | OP |
| `rankedsmp.blacklist` | Manage rank blacklist | OP |
| `rankedsmp.historywipe` | Delete rank history | OP |
| `rankedsmp.export` | Export rank data | OP |
| `rankedsmp.import` | Import rank data | OP |
| `rankedsmp.extrainvsee` | View and edit another player's extra inventory | OP |
| `rankedsmp.bypass.steal` | Bypass rank stealing restrictions | Nobody |
| `rankedsmp.discord.mod` | Receive the configured Discord moderator role | Nobody |
| `rankedsmp.info` | View rank information | Everyone |
| `rankedsmp.history` | View rank history | Everyone |

For example, with LuckPerms:

```text
/lp group moderator permission set rankedsmp.admin false
/lp group moderator permission set rankedsmp.history true
```

Grant only the specific nodes a group needs. See [Commands](commands.md) for command names and [LuckPerms integration](configuration.md) for rank contexts.
