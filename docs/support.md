# Support

Need help with RankedSMPX? Here's how to get support.

## Quick Links

- 💬 **Discord Server:** https://discord.com/invite/QGsJYht2vy
- 📝 **GitHub Issues:** https://github.com/mukulx/RankedSMPX/issues
- 📦 **Modrinth:** https://modrinth.com/project/rankedsmpx

## Before Asking for Help

### 1. Check Documentation

Most questions are answered in the docs:
- [Installation Guide](installation.md)
- [Configuration Guide](configuration.md)
- [FAQ](faq.md)
- [Commands](commands.md)

### 2. Search Existing Issues

Someone may have had the same problem:
- Search GitHub Issues
- Check Discord #support channel
- Read FAQ

### 3. Verify Your Setup

Common issues:
- ✅ Correct Minecraft version (1.21.1+)
- ✅ Java 21 installed
- ✅ Latest plugin version
- ✅ No conflicting plugins
- ✅ Correct permissions

### 4. Check Console Logs

Look for errors in:
- Server startup logs
- Latest.log
- RankedSMPX folder logs (if debug enabled)

## Getting Support

### Discord Server (Fastest)

**Best for:**
- Quick questions
- General help
- Setup assistance
- Feature discussions

**Join:** https://discord.com/invite/QGsJYht2vy

**Channels:**
- `#support` - Ask questions
- `#announcements` - Updates
- `#showcase` - Share your server

**Before posting:**
1. Read pinned messages
2. Use search to find similar questions
3. Provide server version and plugin version

### GitHub Issues (Bug Reports & Features)

**Best for:**
- Bug reports
- Feature requests
- Technical issues
- Plugin development

**Link:** https://github.com/mukulx/RankedSMPX/issues

**How to report a bug:**
1. Click "New Issue"
2. Choose "Bug Report" template
3. Fill in all sections:
   - Description
   - Steps to reproduce
   - Expected behavior
   - Actual behavior
   - Environment (server version, plugin version, Java version)
   - Error logs (if any)
4. Submit

**How to request a feature:**
1. Click "New Issue"
2. Choose "Feature Request" template
3. Describe:
   - What you want
   - Why it's useful
   - How it should work
   - Example use case
4. Submit

## Information to Include

When asking for help, always provide:

### Essential Information
```
- Server Software: Paper 1.21.1
- Plugin Version: 2.1.6
- Java Version: Java 21
- Other Plugins: (list)
```

### For Bug Reports
```
- What you tried to do
- What you expected
- What actually happened
- Error messages (full stack trace)
- Configuration (if relevant)
```

### For Configuration Help
```
- What you're trying to achieve
- Current config (relevant sections)
- What isn't working as expected
```

## Common Issues

### "NoSuchMethodError"

**Cause:** Outdated server version or conflicting plugin

**Fix:**
1. Update to Paper 1.21.1+
2. Update Java to 21+
3. Remove conflicting plugins
4. Update RankedSMPX to latest

### "Could not load plugin"

**Cause:** Wrong server version or missing dependencies

**Fix:**
1. Check server is 1.21.1+
2. Verify Java 21+
3. Re-download plugin
4. Check file isn't corrupted

### "Plugin disabled"

**Cause:** Manually disabled or error during startup

**Fix:**
```
/rsmp enable
```

Or check console for startup errors.

### Ranks don't show

**Cause:** Game not started or TAB conflict

**Fix:**
1. `/rsmp start` to assign ranks
2. `/rsmp reload` to refresh
3. Check TAB config if installed
4. Verify `tab-integration.enabled: true`

### Discord bot offline

**Cause:** Wrong token or guild ID

**Fix:**
1. Verify bot token in `discord.yml`
2. Check guild ID is correct
3. Ensure bot invited to server
4. Check bot permissions
5. Look for errors in console

## Response Times

**Discord:**
- Usually within a few hours
- Community members often help quickly
- Developer responds daily

**GitHub Issues:**
- Usually within 1-3 days
- Bug reports prioritized
- Feature requests evaluated

## Contributing

Want to help improve the plugin?

### Report Bugs
- Test the plugin thoroughly
- Document steps to reproduce
- Provide logs and configs

### Suggest Features
- Describe clear use cases
- Explain benefits
- Consider implementation

### Share Configs
- Post interesting configs in Discord
- Share creative uses
- Help other users

## Developer Contact

**Developer:** mukulx

**Contact:**
- Discord: Join server and @mention or DM
- GitHub: Create issue or discussion
- Email: (see GitHub profile)

## Additional Resources

### Minecraft Server Hosting
- PebbleHost: https://pebblehost.com/
- Apex Hosting: https://apexminecrafthosting.com/
- Shockbyte: https://shockbyte.com/

### Plugin Development
- Paper API Docs: https://docs.papermc.io/
- SpigotMC Wiki: https://www.spigotmc.org/wiki/
- Bukkit Forums: https://bukkit.org/

### Related Plugins
- TAB: https://github.com/NEZNAMY/TAB
- PlaceholderAPI: https://github.com/PlaceholderAPI/PlaceholderAPI

## Thank You

Thank you for using RankedSMPX! Your support and feedback help improve the plugin for everyone.

**Ways to support:**
- 💬 Join the Discord community
- 📦 Follow the project on Modrinth
- 🎥 Create content about the plugin
- ☕ Donate on Ko-fi

---

**Last Updated:** 2026-06-21  
**Plugin Version:** 2.1.6
