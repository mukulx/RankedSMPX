# Features

RankedSMPX is packed with features designed for competitive survival gameplay.

## Core Gameplay

### Rank System
- **20 Limited Ranks** — Only 20 players can have ranks at once
- **Random Assignment** — All online players get random ranks when game starts
- **Rank Stealing** — Kill a higher-ranked player to steal their position
- **Manual Assignment** — Admins can set ranks with `/rsmp set <player> <rank>`
- **Blacklist System** — Exclude specific players from receiving ranks

### Progressive Perks

All perks scale based on rank position. Rank #1 gets maximum benefits, Rank #20 gets minimum.

**Health Scaling**
- Rank #1: 20 hearts (40 HP)
- Rank #20: 10.5 hearts (21 HP)
- Unranked: 10 hearts (20 HP)

**Potion Duration Multipliers**
- Rank #1: 2.0x duration (200%)
- Rank #20: 1.05x duration (105%)
- Only positive effects extended (poison/weakness not affected)

**Experience Multipliers**
- Rank #1: 3.0x XP (300%)
- Rank #20: 1.1x XP (110%)
- Applies to all XP sources (mining, killing, smelting)

**Extra Inventory**
- Rank #1-10: Get bonus inventory slots
- Rank #1: 54 extra slots (full double chest)
- Rank #10: 15 extra slots
- Persistent storage (survives death and logout)
- Access with `/inv` or `/extrainventory`

## Weapons & Items

### Hierarchy Hammer
Legendary mace weapon for Rank #1 player.

**Dash Ability** (Right-click)
- Launch forward to close gaps
- Must land before dashing again
- Configurable cooldown and distance

**Verdict Mode** (Combo System)
- Hit 3 consecutive attacks to charge
- 4th hit deals massive damage
- Creates explosion effect
- Resets on miss

**Mace Smash Bonus**
- Extra damage when falling with hammer
- Scales with fall distance
- Stacks with normal mace mechanics

### Diamond Spear (1.21.11+)
Special weapon with dash enchantment.

- Unbreakable by default
- Lunge III enchantment
- Prevent storage/dropping (configurable)
- Admin-only via `/rsmp give spear <player>`

### Dragon Egg Locator (1.21.6+)
Track Dragon Egg location using waypoint system.

- Real-time waypoint to egg holder
- Distance display
- Prevent storage/dropping (configurable)
- Updates automatically

## Integration Features

### TAB Plugin Integration
Automatic rank prefixes in tab list.

**With TAB Plugin:**
- Full RGB color support
- Gradient prefixes
- MiniMessage formatting
- Advanced customization

**Without TAB (Vanilla Fallback):**
- Uses Minecraft scoreboard teams
- Legacy color codes only
- Automatic prefix assignment
- No setup required

### PlaceholderAPI Support
8 placeholders for chat, scoreboards, holograms.

- `%rankedsmp_rank%` — Player's rank number
- `%rankedsmp_rank_formatted%` — Formatted rank (#1, #2, etc.)
- `%rankedsmp_hearts%` — Current max hearts
- `%rankedsmp_xp_multiplier%` — XP multiplier
- `%rankedsmp_potion_multiplier%` — Potion duration multiplier
- `%rankedsmp_extra_slots%` — Extra inventory slots
- `%rankedsmp_is_ranked%` — true/false
- `%rankedsmp_game_active%` — true/false

### Discord Bot Integration
Full-featured Discord bot for server integration.

**Features:**
- Account linking system (`/linkdiscord` in-game, `/link` in Discord)
- Live leaderboard updates every 5 minutes
- Automatic role assignment based on rank
- Rank notifications via DM
- Admin commands (set rank, import/export data)
- Server update announcements

## Database & Storage

### Database Options
**SQLite (Default)**
- Zero configuration required
- File-based storage
- Perfect for small/medium servers
- WAL mode for better performance

**MySQL (Optional)**
- Supports external databases
- Connection pooling (HikariCP)
- Better for large servers
- Configurable pool settings

### Data Persistence
- Ranks saved to database
- Extra inventories stored (JSON format)
- Rank history tracking (audit log)
- Potion effects persistence across restarts
- Discord account links

## Performance Features

### HikariCP Connection Pooling
- Optimized database connections
- Connection leak detection
- Health monitoring (5-minute intervals)
- Automatic retry on transient errors

### Async Operations
- Database operations run async
- Non-blocking rank saves
- Batch inserts for better performance
- Smart caching (in-memory rank storage)

### Folia Support
- Compatible with Folia (multithreaded Paper fork)
- Region-aware scheduling
- Thread-safe implementations
- No sync issues

## Admin Features

### Game Management
- `/rsmp start` — Start season and assign ranks
- `/rsmp stop` — End season and reset all ranks
- `/rsmp reload` — Reload configuration
- Enable/disable plugin without restart

### Rank Management
- Manual rank assignment
- Rank transfer between players
- Import/export rank data (JSON)
- Rank history viewing
- Database repair tools

### Migration Support
- Import from old RankedSMP plugin
- Preserves all ranks and inventories
- One-command migration process
- Confirmation required for safety

## Customization

### Configuration
- Fully customizable rank perks
- Adjustable multipliers per rank
- Custom rank prefixes (RGB colors)
- Toggle individual features
- Small caps message formatting

### Messages
- Full message customization (messages.yml)
- PlaceholderAPI support in messages
- Small caps font option
- Multi-language support ready

### Styling
- Modern gradient prefixes
- Original legacy colors
- Switch styles with commands
- Works with or without TAB plugin

## Quality of Life

### First-Time Setup
- Welcome message for new admins
- Quick start guide on first join
- Shows only once per admin
- Helpful command suggestions

### Update Checker
- Automatic update notifications
- Shows on admin join
- Links to download page
- Version comparison

### Debug Mode
- Detailed logging for troubleshooting
- Per-feature debug toggles
- Performance monitoring
- Event logging
