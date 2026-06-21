# RankedSMPX

**A highly configurable competitive ranked SMP plugin for Minecraft. Players compete for 20 limited rank slots — kill to climb, survive to stay on top.**

<div style="display: flex; gap: 8px; flex-wrap: wrap; margin: 20px 0;">
  <a href="https://modrinth.com/plugin/rankedsmpx" target="_blank"><img src="https://img.shields.io/badge/Modrinth-1bd96a?style=for-the-badge&logo=modrinth&logoColor=white" alt="Modrinth" /></a>
  <a href="https://discord.com/invite/QGsJYht2vy" target="_blank"><img src="https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord" /></a>
  <a href="https://github.com/mukulx/RankedSMPX/issues" target="_blank"><img src="https://img.shields.io/badge/Issues-181717?style=for-the-badge&logo=github&logoColor=white" alt="Issues" /></a>
</div>

## 📖 Quick Navigation

<div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; margin: 20px 0;">
  <a href="#installation.md" style="padding: 12px 16px; background: var(--bg-secondary); border: 1px solid var(--border); border-radius: 8px; text-decoration: none; color: var(--accent-green); font-weight: 600; display: block;">📦 Installation</a>
  <a href="#commands.md" style="padding: 12px 16px; background: var(--bg-secondary); border: 1px solid var(--border); border-radius: 8px; text-decoration: none; color: var(--accent-green); font-weight: 600; display: block;">⚙️ Commands</a>
  <a href="#configuration.md" style="padding: 12px 16px; background: var(--bg-secondary); border: 1px solid var(--border); border-radius: 8px; text-decoration: none; color: var(--accent-green); font-weight: 600; display: block;">🔧 Configuration</a>
  <a href="#discord.md" style="padding: 12px 16px; background: var(--bg-secondary); border: 1px solid var(--border); border-radius: 8px; text-decoration: none; color: var(--accent-green); font-weight: 600; display: block;">🤖 Discord Bot</a>
  <a href="#faq.md" style="padding: 12px 16px; background: var(--bg-secondary); border: 1px solid var(--border); border-radius: 8px; text-decoration: none; color: var(--accent-green); font-weight: 600; display: block;">❓ FAQ</a>
  <a href="#support.md" style="padding: 12px 16px; background: var(--bg-secondary); border: 1px solid var(--border); border-radius: 8px; text-decoration: none; color: var(--accent-green); font-weight: 600; display: block;">💬 Support</a>
</div>

---

## What is RankedSMPX?

RankedSMPX is a competitive Minecraft plugin where players fight for ranks 1-20. Higher ranks get better perks like more hearts, longer potions, bonus XP, and extra inventory space. Kill someone with a better rank to steal their spot!

## Requirements

- **Minecraft:** 1.21.1+
- **Java:** 21+
- **Server:** Paper, Purpur, Pufferfish, Folia, Leaf, Spigot, or Bukkit

## Quick Start

1. [Download from Modrinth](https://modrinth.com/plugin/rankedsmpx)
2. Drop the `.jar` in your `plugins/` folder
3. Run `/rsmp start` to assign ranks
4. Fight, steal ranks, climb to #1

## Core Features

- **Random Rank Assignment** — All online players get a random rank (1-20) when the game starts
- **Rank Stealing** — Kill higher-ranked players to steal their position
- **Progressive Perks** — Hearts, potion duration, XP multiplier, and extra inventory scale by rank
- **Hierarchy Hammer** — Legendary mace weapon with dash and combo abilities
- **TAB Integration** — Automatic rank prefixes in tab list (TAB plugin or vanilla fallback)
- **PlaceholderAPI** — 8 placeholders for chat, scoreboards, and holograms
- **Discord Bot** — Account linking, live leaderboard, role sync, and admin commands
- **Database** — SQLite (zero-config) or MySQL with HikariCP connection pooling

## Links

- [Download on Modrinth](https://modrinth.com/plugin/rankedsmpx)
- [Installation Guide](installation.md)
- [All Commands](commands.md)
- [Discord Support](https://discord.com/invite/QGsJYht2vy)
- [Report Issues](https://github.com/mukulx/RankedSMPX/issues)
