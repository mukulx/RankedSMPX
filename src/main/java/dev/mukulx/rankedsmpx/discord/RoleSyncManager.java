package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RoleSyncManager {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;
  private final JDA jda;
  private final Guild guild;

  private final Map<Integer, String> rankRoleIds = new ConcurrentHashMap<>();
  private String unrankedRoleId;
  private String modRoleId;

  private final Set<String> pendingUpdates = ConcurrentHashMap.newKeySet();

  private volatile boolean isShuttingDown = false;

  public RoleSyncManager(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull DiscordManager discordManager,
      @NotNull JDA jda,
      @NotNull Guild guild) {
    this.plugin = plugin;
    this.discordManager = discordManager;
    this.jda = jda;
    this.guild = guild;
  }

  public void initialize() {
    if (!plugin.getDiscordConfig().getBoolean("role-sync.enabled", false)) {
      plugin.getLogger().info("Discord role sync is disabled in config");
      return;
    }

    plugin.getLogger().info("Initializing Discord role sync...");

    loadRoleIdsFromConfig();

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          try {
            createOrFindRoles();
            plugin.getLogger().info("Discord role sync initialized successfully!");

            syncAllRanks();
          } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize role sync: " + e.getMessage());
            e.printStackTrace();
          }
        });
  }

  private void createOrFindRoles() {
    boolean autoCreate = plugin.getDiscordConfig().getBoolean("role-sync.auto-create-roles", true);
    int totalRanks = plugin.getConfigManager().getTotalRanks();

    createOrFindUnrankedRole(autoCreate);

    for (int rank = 1; rank <= totalRanks; rank++) {
      createOrFindRankRole(rank, autoCreate);
    }

    saveRoleIdsToConfig();
  }

  private void createOrFindUnrankedRole(boolean autoCreate) {
    String roleName = plugin.getDiscordConfig().getString("role-sync.unranked-role", "Unranked");

    if (unrankedRoleId != null && !unrankedRoleId.isEmpty()) {
      Role role = guild.getRoleById(unrankedRoleId);
      if (role != null) {
        plugin.getLogger().info("Found existing Unranked role: " + role.getName());
        return;
      } else {
        unrankedRoleId = null;
      }
    }

    List<Role> existingRoles = guild.getRolesByName(roleName, true);
    if (!existingRoles.isEmpty()) {
      Role role = existingRoles.get(0);
      unrankedRoleId = role.getId();
      plugin.getLogger().info("Found existing Unranked role: " + role.getName());
      return;
    }

    if (!autoCreate) {
      plugin
          .getLogger()
          .warning(
              "Unranked role not found and auto-create is disabled. Please create role manually: "
                  + roleName);
      return;
    }

    try {
      plugin.getLogger().info("Creating Unranked role: " + roleName);

      String colorHex = plugin.getDiscordConfig().getString("role-sync.colors.unranked", "#696969");
      Color color = parseColor(colorHex);

      guild
          .createRole()
          .setName(roleName)
          .setColor(color)
          .setMentionable(false)
          .setHoisted(false)
          .complete();

      List<Role> roles = guild.getRolesByName(roleName, true);
      if (!roles.isEmpty()) {
        unrankedRoleId = roles.get(0).getId();
        plugin.getLogger().info("Created Unranked role");
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to create Unranked role: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void createOrFindRankRole(int rank, boolean autoCreate) {
    String roleName = "Rank #" + rank;

    if (rankRoleIds.containsKey(rank)) {
      String roleId = rankRoleIds.get(rank);
      if (roleId != null && !roleId.isEmpty()) {
        Role role = guild.getRoleById(roleId);
        if (role != null) {
          plugin.getLogger().info("Found existing role: " + role.getName());
          return;
        } else {
          rankRoleIds.remove(rank);
        }
      }
    }

    List<Role> existingRoles = guild.getRolesByName(roleName, true);
    if (!existingRoles.isEmpty()) {
      Role role = existingRoles.get(0);
      rankRoleIds.put(rank, role.getId());
      plugin.getLogger().info("Found existing role: " + role.getName());
      return;
    }

    if (!autoCreate) {
      plugin
          .getLogger()
          .warning(
              "Rank role not found and auto-create is disabled. Please create role manually: "
                  + roleName);
      return;
    }

    try {
      plugin.getLogger().info("Creating rank role: " + roleName);

      Color roleColor = getRankColorFromConfig(rank);

      guild
          .createRole()
          .setName(roleName)
          .setColor(roleColor)
          .setMentionable(false)
          .setHoisted(false)
          .complete();

      List<Role> roles = guild.getRolesByName(roleName, true);
      if (!roles.isEmpty()) {
        rankRoleIds.put(rank, roles.get(0).getId());
        plugin.getLogger().info("Created rank role: " + roleName);
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to create role " + roleName + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void syncPlayerRole(@NotNull UUID uuid, @Nullable Integer rank) {
    if (!plugin.getDiscordConfig().getBoolean("role-sync.enabled", false)) {
      return;
    }

    if (isShuttingDown) {
      return;
    }

    String discordId = plugin.getDiscordManager().getLinkManager().getDiscordId(uuid);
    if (discordId == null) {
      return;
    }

    // Atomic check-and-add to prevent race condition
    String updateKey = discordId + ":" + rank;
    if (!pendingUpdates.add(updateKey)) {
      return;
    }

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          if (isShuttingDown) {
            pendingUpdates.remove(updateKey);
            return;
          }

          try {
            guild
                .retrieveMemberById(discordId)
                .queue(
                    member -> {
                      if (!isShuttingDown) {
                        try {
                          updateMemberRoles(member, rank);
                        } catch (Exception e) {
                          plugin
                              .getLogger()
                              .warning(
                                  "Error updating roles for "
                                      + member.getEffectiveName()
                                      + ": "
                                      + e.getMessage());
                        } finally {
                          SchedulerUtil.runLaterAsync(
                              plugin,
                              () -> {
                                pendingUpdates.remove(updateKey);
                              },
                              20L); // 1 second cooldown
                        }
                      } else {
                        pendingUpdates.remove(updateKey);
                      }
                    },
                    error -> {
                      if (error.getMessage().contains("10007")) {
                        plugin
                            .getLogger()
                            .info("Player " + discordId + " is not in the Discord server");
                      } else if (!error.getMessage().contains("interrupt")) {
                        plugin
                            .getLogger()
                            .warning(
                                "Failed to retrieve Discord member "
                                    + discordId
                                    + ": "
                                    + error.getMessage());
                      }
                      pendingUpdates.remove(updateKey);
                    });
          } catch (Exception e) {
            if (!e.getMessage().contains("interrupt")) {
              plugin
                  .getLogger()
                  .warning("Failed to sync role for " + discordId + ": " + e.getMessage());
            }
            pendingUpdates.remove(updateKey);
          }
        });
  }

  private void updateMemberRoles(@NotNull Member member, @Nullable Integer rank) {
    if (isShuttingDown) {
      return;
    }

    List<Role> rolesToAdd = new ArrayList<>();
    List<Role> rolesToRemove = new ArrayList<>();

    for (String roleId : rankRoleIds.values()) {
      if (roleId != null && !roleId.isEmpty()) {
        Role role = guild.getRoleById(roleId);
        if (role != null && member.getRoles().contains(role)) {
          rolesToRemove.add(role);
        }
      }
    }

    if (unrankedRoleId != null && !unrankedRoleId.isEmpty()) {
      Role unrankedRole = guild.getRoleById(unrankedRoleId);
      if (unrankedRole != null && member.getRoles().contains(unrankedRole)) {
        rolesToRemove.add(unrankedRole);
      }
    }

    if (rank == null) {
      if (unrankedRoleId != null && !unrankedRoleId.isEmpty()) {
        Role unrankedRole = guild.getRoleById(unrankedRoleId);
        if (unrankedRole != null) {
          rolesToAdd.add(unrankedRole);
        } else {
          plugin.getLogger().warning("Unranked role not found! ID: " + unrankedRoleId);
        }
      }
    } else {
      String roleId = rankRoleIds.get(rank);
      if (roleId != null && !roleId.isEmpty()) {
        Role rankRole = guild.getRoleById(roleId);
        if (rankRole != null) {
          rolesToAdd.add(rankRole);
        } else {
          plugin.getLogger().warning("Rank role not found! Rank: " + rank + ", ID: " + roleId);
          boolean autoCreate =
              plugin.getDiscordConfig().getBoolean("role-sync.auto-create-roles", true);
          createOrFindRankRole(rank, autoCreate);
        }
      } else {
        plugin.getLogger().warning("Rank role ID missing for rank: " + rank + " - creating role");
        boolean autoCreate =
            plugin.getDiscordConfig().getBoolean("role-sync.auto-create-roles", true);
        createOrFindRankRole(rank, autoCreate);
      }
    }

    try {
      if (!rolesToRemove.isEmpty() || !rolesToAdd.isEmpty()) {
        guild
            .modifyMemberRoles(member, rolesToAdd, rolesToRemove)
            .queue(
                success -> {
                  String roleInfo =
                      !rolesToAdd.isEmpty() ? rolesToAdd.get(0).getName() : "removed rank roles";
                  plugin
                      .getLogger()
                      .info("Updated " + member.getEffectiveName() + " to " + roleInfo);
                },
                error -> {
                  String errorMsg = error.getMessage();
                  if (errorMsg.contains("50013")) {
                    plugin
                        .getLogger()
                        .severe(
                            "Missing Permissions! Bot needs 'Manage Roles' permission and must be above rank roles in the role hierarchy!");
                  } else if (errorMsg.contains("50001")) {
                    plugin
                        .getLogger()
                        .warning(
                            "Bot lacks access to modify roles for " + member.getEffectiveName());
                  } else if (errorMsg.contains("10011")) {
                    plugin
                        .getLogger()
                        .warning("Role not found - may have been deleted. Recreating roles...");
                    createOrFindRoles();
                  } else if (!errorMsg.contains("interrupt")) {
                    plugin
                        .getLogger()
                        .warning(
                            "Failed to update roles for "
                                + member.getEffectiveName()
                                + ": "
                                + errorMsg);
                  }
                });
      }
    } catch (Exception e) {
      if (!e.getMessage().contains("interrupt")) {
        plugin.getLogger().severe("Exception while modifying roles: " + e.getMessage());
      }
    }
  }

  public void syncAllRanks() {
    if (!plugin.getDiscordConfig().getBoolean("role-sync.enabled", false)) {
      return;
    }

    plugin.getLogger().info("Syncing all player ranks to Discord roles...");

    Map<UUID, Integer> allRanks = plugin.getRankManager().getAllRanks();

    long delayTicks = 0;
    for (Map.Entry<UUID, Integer> entry : allRanks.entrySet()) {
      final UUID uuid = entry.getKey();
      final Integer rank = entry.getValue();

      SchedulerUtil.runLaterAsync(
          plugin,
          () -> {
            syncPlayerRole(uuid, rank);
          },
          delayTicks);

      delayTicks += 10L; // 0.5 second delay between each sync (faster than before)
    }

    plugin
        .getLogger()
        .info(
            "Queued "
                + allRanks.size()
                + " player ranks for sync (ETA: "
                + (delayTicks / 20)
                + " seconds)");
  }

  private void loadRoleIdsFromConfig() {
    String unrankedId = plugin.getDiscordConfig().getString("role-sync.unranked-role-id", "");
    if (unrankedId != null && !unrankedId.isEmpty()) {
      unrankedRoleId = unrankedId;
    }

    String modId = plugin.getDiscordConfig().getString("role-sync.mod-role-id", "");
    if (modId != null && !modId.isEmpty()) {
      modRoleId = modId;
    }

    var roleIdsSection =
        plugin.getDiscordConfig().getConfigurationSection("role-sync.rank-role-ids");
    if (roleIdsSection != null) {
      for (String key : roleIdsSection.getKeys(false)) {
        try {
          int rank = Integer.parseInt(key);
          String roleId = roleIdsSection.getString(key);
          if (roleId != null && !roleId.isEmpty()) {
            rankRoleIds.put(rank, roleId);
          }
        } catch (NumberFormatException e) {
          plugin.getLogger().warning("Invalid rank in config: " + key);
        }
      }
    }
  }

  private void saveRoleIdsToConfig() {
    if (unrankedRoleId != null) {
      plugin.getDiscordConfig().set("role-sync.unranked-role-id", unrankedRoleId);
    }

    for (Map.Entry<Integer, String> entry : rankRoleIds.entrySet()) {
      plugin.getDiscordConfig().set("role-sync.rank-role-ids." + entry.getKey(), entry.getValue());
    }

    plugin.saveDiscordConfig();
    plugin.getLogger().info("Saved role IDs to discord.yml");
  }

  private Color getRankColorFromConfig(int rank) {
    String colorHex;

    if (rank == 1) {
      colorHex = plugin.getDiscordConfig().getString("role-sync.colors.rank-1", "#FFD700");
    } else if (rank >= 2 && rank <= 3) {
      colorHex = plugin.getDiscordConfig().getString("role-sync.colors.rank-2-3", "#C0C0C0");
    } else if (rank >= 4 && rank <= 10) {
      colorHex = plugin.getDiscordConfig().getString("role-sync.colors.rank-4-10", "#CD7F32");
    } else {
      colorHex = plugin.getDiscordConfig().getString("role-sync.colors.rank-11-plus", "#808080");
    }

    return parseColor(colorHex);
  }

  private Color parseColor(String hex) {
    try {
      if (hex.startsWith("#")) {
        hex = hex.substring(1);
      }

      int rgb = Integer.parseInt(hex, 16);
      return new Color(rgb);
    } catch (Exception e) {
      plugin.getLogger().warning("Invalid color hex: " + hex + ", using default gray");
      return new Color(128, 128, 128);
    }
  }

  public void shutdown() {
    isShuttingDown = true;
    pendingUpdates.clear();
    plugin.getLogger().info("Role sync shutdown - cancelled pending operations");
  }

  public static final String MOD_PERMISSION = "rankedsmp.discord.mod";

  /**
   * Sync the Discord mod role (role-sync.mod-role-id) with the player's rankedsmp.discord.mod
   * permission. Gives the role to linked players who have the permission; takes it away from those
   * who don't only when role-sync.remove-mod-role-without-permission is true, so moderators
   * assigned by hand in Discord are left alone by default.
   */
  public void syncModRole(@NotNull UUID uuid, boolean hasModPermission) {
    if (modRoleId == null || modRoleId.isEmpty()) {
      return;
    }
    if (hasModPermission) {
      giveModRole(uuid);
    } else if (plugin
        .getDiscordConfig()
        .getBoolean("role-sync.remove-mod-role-without-permission", false)) {
      removeModRole(uuid);
    }
  }

  public void syncModRoleIfOnline(@NotNull UUID uuid) {
    org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
    if (player != null) {
      syncModRole(uuid, player.hasPermission(MOD_PERMISSION));
    }
  }

  public void giveModRole(@NotNull UUID uuid) {
    if (!plugin.getDiscordConfig().getBoolean("role-sync.enabled", false)) {
      return;
    }

    if (modRoleId == null || modRoleId.isEmpty()) {
      plugin.getLogger().warning("Mod role ID not configured in discord.yml");
      return;
    }

    String discordId = plugin.getDiscordManager().getLinkManager().getDiscordId(uuid);
    if (discordId == null) {
      return;
    }

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          try {
            Role modRole = guild.getRoleById(modRoleId);
            if (modRole == null) {
              plugin.getLogger().warning("Mod role not found: " + modRoleId);
              return;
            }

            guild
                .retrieveMemberById(discordId)
                .queue(
                    member -> {
                      if (!member.getRoles().contains(modRole)) {
                        guild
                            .addRoleToMember(member, modRole)
                            .queue(
                                success ->
                                    plugin
                                        .getLogger()
                                        .info("Gave mod role to " + member.getEffectiveName()),
                                error ->
                                    plugin
                                        .getLogger()
                                        .warning("Failed to give mod role: " + error.getMessage()));
                      }
                    },
                    error -> {
                      plugin
                          .getLogger()
                          .warning("Failed to retrieve member for mod role: " + error.getMessage());
                    });
          } catch (Exception e) {
            plugin.getLogger().warning("Failed to give mod role: " + e.getMessage());
          }
        });
  }

  public void removeModRole(@NotNull UUID uuid) {
    if (!plugin.getDiscordConfig().getBoolean("role-sync.enabled", false)) {
      return;
    }

    if (modRoleId == null || modRoleId.isEmpty()) {
      plugin.getLogger().warning("Mod role ID not configured in discord.yml");
      return;
    }

    String discordId = plugin.getDiscordManager().getLinkManager().getDiscordId(uuid);
    if (discordId == null) {
      return;
    }

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          try {
            Role modRole = guild.getRoleById(modRoleId);
            if (modRole == null) {
              plugin.getLogger().warning("Mod role not found: " + modRoleId);
              return;
            }

            guild
                .retrieveMemberById(discordId)
                .queue(
                    member -> {
                      if (member.getRoles().contains(modRole)) {
                        guild
                            .removeRoleFromMember(member, modRole)
                            .queue(
                                success ->
                                    plugin
                                        .getLogger()
                                        .info(
                                            "Removed mod role from " + member.getEffectiveName()),
                                error ->
                                    plugin
                                        .getLogger()
                                        .warning(
                                            "Failed to remove mod role: " + error.getMessage()));
                      }
                    },
                    error -> {
                      plugin
                          .getLogger()
                          .warning(
                              "Failed to retrieve member for mod role removal: "
                                  + error.getMessage());
                    });
          } catch (Exception e) {
            plugin.getLogger().warning("Failed to remove mod role: " + e.getMessage());
          }
        });
  }

  @Nullable
  public Integer getRankFromRoleId(@NotNull String roleId) {
    for (Map.Entry<Integer, String> entry : rankRoleIds.entrySet()) {
      if (entry.getValue().equals(roleId)) {
        return entry.getKey();
      }
    }
    return null;
  }
}
