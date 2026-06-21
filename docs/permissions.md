# Permissions

Complete list of all permission nodes in RankedSMPX.

## Admin Permissions

These permissions grant access to administrative commands and features.

| Permission | Description | Default |
|------------|-------------|---------|
| `rankedsmp.admin` | Full admin access to all features | op |
| `rankedsmp.start` | Start the season and assign ranks | op |
| `rankedsmp.stop` | Stop the season and reset all ranks | op |
| `rankedsmp.reload` | Reload configuration files | op |
| `rankedsmp.set` | Set player ranks manually | op |
| `rankedsmp.remove` | Remove player ranks | op |
| `rankedsmp.blacklist` | Blacklist players from receiving ranks | op |
| `rankedsmp.historywipe` | Wipe rank history | op |
| `rankedsmp.export` | Export rank data to JSON | op |
| `rankedsmp.import` | Import rank data from JSON | op |
| `rankedsmp.gui` | Open rank management GUI | op |
| `rankedsmp.extrainvsee` | View other players' extra inventories | op |

## Player Permissions

These permissions grant access to player commands and features.

| Permission | Description | Default |
|------------|-------------|---------|
| `rankedsmp.info` | View rank information with `/rankinfo` | true |
| `rankedsmp.history` | View rank history with `/rankhistory` | true |

## Special Access

| Permission | Description | Default |
|------------|-------------|---------|
| `rankedsmp.extrainvsee` | View other players' extra inventories | op |

**Note:** The following commands do NOT require specific permissions (available to all players):
- `/ranktop` - View top players
- `/ranks` - View all ranks  
- `/extrainventory` (`/inv`) - Access extra inventory
- `/ranktransfer` - Transfer rank to another player
- `/linkdiscord` - Link Discord account
- `/unlinkdiscord` - Unlink Discord account
- `/rank` - View rank information

## Permission Groups

For easy setup, you can use these groups:

### Admin Group
```yaml
groups:
  rankedsmp-admin:
    permissions:
      - rankedsmp.admin
      - rankedsmp.start
      - rankedsmp.stop
      - rankedsmp.reload
      - rankedsmp.set
      - rankedsmp.remove
      - rankedsmp.blacklist
      - rankedsmp.unblacklist
      - rankedsmp.export
      - rankedsmp.import
      - rankedsmp.gui
      - rankedsmp.extrainvsee
```

### Player Group
```yaml
groups:
  rankedsmp-player:
    permissions:
      - rankedsmp.info
      - rankedsmp.top
      - rankedsmp.ranks
      - rankedsmp.history
      - rankedsmp.transfer
      - rankedsmp.inv
      - rankedsmp.linkdiscord
      - rankedsmp.unlinkdiscord
```

## Permission Plugin Examples

### LuckPerms

Grant admin permissions:
```
/lp group admin permission set rankedsmp.admin true
```

Grant player permissions:
```
/lp group default permission set rankedsmp.info true
/lp group default permission set rankedsmp.top true
```

### GroupManager

Grant admin permissions:
```yaml
groups:
  Admin:
    permissions:
      - rankedsmp.admin
```

### PermissionsEx

Grant admin permissions:
```
/pex group admin add rankedsmp.admin
```

## Default Permissions

By default, when no permission plugin is installed:
- **Operators (OP)**: Have all admin permissions
- **Players**: Have all player permissions (info, top, ranks, history, transfer, inv)
