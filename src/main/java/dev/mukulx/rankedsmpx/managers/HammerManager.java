package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class HammerManager {

  private final RankedSMPXPlugin plugin;
  private final ConfigManager configManager;
  private final MessageManager messageManager;

  private final Map<UUID, Boolean> sentencingActive = new ConcurrentHashMap<>();
  private final Map<UUID, Long> sentencingStartTime = new ConcurrentHashMap<>();
  private final Map<UUID, Integer> dashCharges = new ConcurrentHashMap<>();
  private final Map<UUID, Long> lastHitTime = new ConcurrentHashMap<>();
  private final Map<UUID, Integer> comboHits = new ConcurrentHashMap<>();
  private final Map<UUID, Long> dashCooldowns = new ConcurrentHashMap<>();
  private final Map<UUID, Long> missCooldowns = new ConcurrentHashMap<>();
  private final Map<UUID, Long> dashProtection = new ConcurrentHashMap<>();
  private final Map<UUID, SchedulerUtil.TaskHandle> dashTasks = new ConcurrentHashMap<>();

  public HammerManager(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull ConfigManager configManager,
      @NotNull MessageManager messageManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    this.messageManager = messageManager;
  }

  @NotNull
  public ItemStack createHammer() {
    ItemStack hammer = new ItemStack(Material.MACE);
    ItemMeta meta = hammer.getItemMeta();

    if (meta == null) {
      plugin
          .getLogger()
          .severe("Failed to get ItemMeta for hammer! Returning barrier as fallback.");
      return new ItemStack(Material.BARRIER);
    }

    String name =
        configManager
            .getConfig()
            .getString(
                "hierarchy-hammer.name",
                "§x§f§2§b§c§4§5§lʜ§x§f§2§b§f§4§b§lɪ§x§f§2§c§1§5§1§lᴇ§x§f§2§c§4§5§6§lʀ§x§f§2§c§6§5§c§lᴀ§x§f§2§c§9§6§2§lʀ§x§f§2§c§c§6§8§lᴄ§x§f§2§c§b§6§b§lʜ§x§f§2§d§1§7§3§lʏ §x§f§2§d§6§7§f§lʜ§x§f§2§d§9§8§5§lᴀ§x§f§2§d§b§8§b§lᴍ§x§f§2§d§b§9§0§lᴍ§x§f§2§b§0§9§6§lᴇ§x§f§2§b§3§9§c§lʀ");
    meta.displayName(messageManager.legacyToComponent(name));

    List<String> lore = configManager.getConfig().getStringList("hierarchy-hammer.lore");
    List<String> coloredLore = new ArrayList<>();
    for (String line : lore) {
      coloredLore.add(messageManager.colorize(line));
    }
    meta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(coloredLore));

    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "judgement_hammer");
    meta.getPersistentDataContainer()
        .set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

    int customModelData =
        configManager
            .getConfig()
            .getInt(
                "hierarchy-hammer.custom-model-data",
                configManager.getConfig().getInt("hierarchy-hammer.model-id", 0));
    if (customModelData > 0) {
      org.bukkit.inventory.meta.components.CustomModelDataComponent modelData =
          meta.getCustomModelDataComponent();
      modelData.setFloats(java.util.List.of((float) customModelData));
      meta.setCustomModelDataComponent(modelData);
    }

    String itemModel = configManager.getConfig().getString("hierarchy-hammer.item-model", "");
    if (itemModel != null && !itemModel.isBlank()) {
      org.bukkit.NamespacedKey modelKey = org.bukkit.NamespacedKey.fromString(itemModel.toLowerCase());
      if (modelKey != null) {
        meta.setItemModel(modelKey);
      }
    }

    meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    meta.setUnbreakable(true);
    meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

    hammer.setItemMeta(meta);

    return hammer;
  }

  public boolean isHammer(@NotNull ItemStack item) {
    if (item.getType() != Material.MACE) {
      return false;
    }

    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }

    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "judgement_hammer");
    if (meta.getPersistentDataContainer()
        .has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
      return true;
    }

    if (!meta.hasDisplayName()) {
      return false;
    }

    String name =
        configManager
            .getConfig()
            .getString(
                "hierarchy-hammer.name",
                "§x§f§2§b§c§4§5§lʜ§x§f§2§b§f§4§b§lɪ§x§f§2§c§1§5§1§lᴇ§x§f§2§c§4§5§6§lʀ§x§f§2§c§6§5§c§lᴀ§x§f§2§c§9§6§2§lʀ§x§f§2§c§c§6§8§lᴄ§x§f§2§c§b§6§b§lʜ§x§f§2§d§1§7§3§lʏ §x§f§2§d§6§7§f§lʜ§x§f§2§d§9§8§5§lᴀ§x§f§2§d§b§8§b§lᴍ§x§f§2§d§b§9§0§lᴍ§x§f§2§b§0§9§6§lᴇ§x§f§2§b§3§9§c§lʀ");
    net.kyori.adventure.text.Component expectedName = messageManager.legacyToComponent(name);
    net.kyori.adventure.text.Component actualName = meta.displayName();

    return actualName != null && actualName.equals(expectedName);
  }

  public void performDash(@NotNull Player player) {
    UUID uuid = player.getUniqueId();
    long now = System.currentTimeMillis();

    Long cooldownEnd = dashCooldowns.get(uuid);
    if (cooldownEnd != null && now < cooldownEnd) {
      long remainingMs = cooldownEnd - now;
      int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);
      plugin.getMessageManager().sendActionBar(player, "§cDash on cooldown: §b" + remainingSeconds + "s");
      return;
    }

    if (isOnMissCooldown(player)) {
      return;
    }

    if (sentencingActive.getOrDefault(uuid, false)) {
      int charges = dashCharges.getOrDefault(uuid, 0);
      if (charges <= 0) {
        plugin.getMessageManager().sendActionBar(player, "§cNo dash charges! Land smash attacks to earn charges.");
        return;
      }
      dashCharges.put(uuid, charges - 1);
      performMiniDash(player);
      return;
    }

    sentencingActive.put(uuid, true);
    sentencingStartTime.put(uuid, now);
    lastHitTime.put(uuid, now);
    comboHits.put(uuid, 0);
    dashCharges.put(uuid, 0);
    dashProtection.put(uuid, now + 5000L);

    int cooldownSeconds = configManager.getConfig().getInt("hierarchy-hammer.dash-cooldown", 0);
    if (cooldownSeconds > 0) {
      dashCooldowns.put(uuid, now + (cooldownSeconds * 1000L));
      player.setCooldown(Material.MACE, cooldownSeconds * 20);
    }

    player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BREEZE_DEFLECT, 0.75f, 0.7f);
    SchedulerUtil.runForEntity(plugin, player, () -> {
      if (player.isOnline()) {
        player.setVelocity(player.getEyeLocation().getDirection().multiply(2.2 * getDashSpeed()).setY(0.75));
      }
    });
    startDashTrail(player);
  }

  public void performMiniDash(@NotNull Player player) {
    UUID uuid = player.getUniqueId();
    lastHitTime.put(uuid, System.currentTimeMillis());
    dashProtection.put(uuid, System.currentTimeMillis() + 5000L);

    Location loc = player.getLocation();
    player.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_BREEZE_DEFLECT, 0.75f, 0.7f);
    SchedulerUtil.runForEntity(plugin, player, () -> {
      if (player.isOnline()) {
        player.setVelocity(player.getEyeLocation().getDirection().multiply(2.2 * getDashSpeed()).setY(0.75));
      }
    });
    spawnRotatingCircle(loc.clone().add(0.0, 0.1, 0.0), 0.0);
    startDashTrail(player);
  }

  private double getDashSpeed() {
    return Math.max(0.1, Math.min(2.5, configManager.getConfig().getDouble("hierarchy-hammer.dash-speed", 1.0)));
  }

  private boolean particlesEnabled() {
    return configManager.getConfig().getBoolean("hierarchy-hammer.particles.enabled", true);
  }

  public void startDashTrail(@NotNull Player player) {
    if (!particlesEnabled()) {
      return;
    }
    UUID uuid = player.getUniqueId();
    SchedulerUtil.TaskHandle old = dashTasks.remove(uuid);
    if (old != null) {
      old.cancel();
    }

    final double[] angle = {0.0};
    final int[] ticks = {0};
    final int[] circleCount = {0};
    final Location[] lastCircleLoc = {null};

    SchedulerUtil.TaskHandle task =
        SchedulerUtil.runTimerForEntity(
            plugin,
            player,
            handle -> {
              if (isStandingOnGround(player) && ticks[0] > 5) {
                handle.cancel();
                dashTasks.remove(uuid);
                return;
              }
              Location currentLoc = player.getLocation();
              org.bukkit.World world = player.getWorld();
              if (world == null || !player.isOnline()) {
                handle.cancel();
                dashTasks.remove(uuid);
                return;
              }

              world.spawnParticle(
                  org.bukkit.Particle.DUST,
                  currentLoc.clone().add(0.0, 1.0, 0.0),
                  3,
                  0.2,
                  0.2,
                  0.2,
                  new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(242, 188, 69), 1.2f));

              if (ticks[0] % 3 == 0 && circleCount[0] < 4) {
                lastCircleLoc[0] = currentLoc.clone();
                circleCount[0]++;
                spawnRotatingCircle(lastCircleLoc[0], angle[0]);
              }

              if (lastCircleLoc[0] != null && lastCircleLoc[0].distance(currentLoc) < 3.0) {
                spawnRotatingCircle(lastCircleLoc[0], angle[0]);
              }

              angle[0] += 0.19634954084936207;
              ticks[0]++;
            },
            0L,
            1L);

    dashTasks.put(uuid, task);
  }

  public void spawnRotatingCircle(@NotNull Location center, double angle) {
    org.bukkit.World world = center.getWorld();
    if (world == null || !particlesEnabled()) return;

    for (int i = 0; i < 16; i++) {
      double offsetAngle = angle + (double) i * Math.PI / 8.0;
      Location pLoc =
          center
              .clone()
              .add(Math.cos(offsetAngle) * 0.8, 0.1, Math.sin(offsetAngle) * 0.8);
      world.spawnParticle(
          org.bukkit.Particle.DUST,
          pLoc,
          1,
          0.0,
          0.0,
          0.0,
          0.0,
          new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(242, 188, 69), 1.0f));
    }
  }

  public static boolean isStandingOnGround(@NotNull Player player) {
    org.bukkit.util.BoundingBox box = player.getBoundingBox();
    org.bukkit.util.BoundingBox feet =
        new org.bukkit.util.BoundingBox(
            box.getMinX(), box.getMinY() - 0.05, box.getMinZ(),
            box.getMaxX(), box.getMinY(), box.getMaxZ());
    return player.getWorld().hasCollisionsIn(feet);
  }

  public void applyMissCooldown(@NotNull Player player) {
    UUID uuid = player.getUniqueId();
    int missCooldownSec = configManager.getConfig().getInt("hierarchy-hammer.miss-cooldown", 10);
    dashCooldowns.put(uuid, System.currentTimeMillis() + (missCooldownSec * 1000L));
    missCooldowns.put(uuid, System.currentTimeMillis() + (missCooldownSec * 1000L));
    resetSentencing(uuid);

    org.bukkit.World world = player.getWorld();
    if (world != null) {
      world.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.5f, 1.1f);
    }
    plugin.getMessageManager().sendActionBar(player, "§6You missed, now on cooldown for §b" + missCooldownSec + "s");
    player.setCooldown(Material.MACE, missCooldownSec * 20);
  }

  public void executeVerdict(
      @NotNull Player attacker,
      @NotNull Player victim,
      @NotNull org.bukkit.event.entity.EntityDamageByEntityEvent event) {
    event.setDamage(0.0);
    Location victimLoc = victim.getLocation().clone();
    org.bukkit.World world = victimLoc.getWorld();
    if (world == null) return;

    double baseDamage =
        configManager.getConfig().getDouble("hierarchy-hammer.verdict-damage", 40.0)
            * Math.max(0.0, configManager.getConfig().getDouble("hierarchy-hammer.combo-damage-multiplier", 1.0));
    org.bukkit.potion.PotionEffect resistance = victim.getPotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE);
    if (resistance != null) {
      baseDamage = Math.max(0.0, baseDamage * (1.0 - (double) (resistance.getAmplifier() + 1) * 0.2));
    }
    double finalDamage = baseDamage;

    SchedulerUtil.runForEntity(plugin, victim, () -> {
      if (!victim.isOnline()) return;
      victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION, 60, 0, false, false));
    });

    world.playSound(victimLoc, org.bukkit.Sound.BLOCK_ANVIL_LAND, 1.0f, 0.6f);
    world.playSound(victimLoc, org.bukkit.Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1.2f);

    SchedulerUtil.runForEntityLater(
        plugin,
        victim,
        () -> {
          if (!victim.isOnline()) return;
          victim.removePotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION);
          victim.setGravity(false);
          try {
          victim.setVelocity(new Vector(0, 0, 0));

          Location raisedLoc = victim.getLocation();
          org.bukkit.World raisedWorld = raisedLoc.getWorld();
          if (raisedWorld == null) return;

          spawnVerdictTerrainDebris(raisedLoc);

          raisedWorld.spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, raisedLoc, 2, 0.0, 0.0, 0.0, 0.0);
          raisedWorld.spawnParticle(org.bukkit.Particle.GUST_EMITTER_LARGE, raisedLoc, 2, 0.0, 0.0, 0.0, 0.0);
          raisedWorld.playSound(raisedLoc, org.bukkit.Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
          raisedWorld.playSound(raisedLoc, org.bukkit.Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.4f, 1.0f);

          for (int j = 0; j < 48; j++) {
            double phi = Math.acos(1.0 - 2.0 * ((double) j + 0.5) / 48.0);
            double theta = Math.PI * (1.0 + Math.sqrt(5.0)) * (double) j;
            double dx = Math.sin(phi) * Math.cos(theta);
            double dy = Math.cos(phi);
            double dz = Math.sin(phi) * Math.sin(theta);
            raisedWorld.spawnParticle(
                org.bukkit.Particle.ELECTRIC_SPARK,
                raisedLoc.getX(),
                raisedLoc.getY() + 0.5,
                raisedLoc.getZ(),
                0,
                dx,
                dy,
                dz,
                0.8);
          }

          for (int level = 0; level < 10; level++) {
            double h = (double) level / 10.0 * 2.0;
            double r = 0.5 + h * 0.8;
            for (int p = 0; p < 8; p++) {
              double ang = (level * 0.5) + (p * Math.PI * 2 / 8.0);
              raisedWorld.spawnParticle(
                  org.bukkit.Particle.DUST,
                  raisedLoc.clone().add(Math.cos(ang) * r, h, Math.sin(ang) * r),
                  1,
                  0,
                  0,
                  0,
                  0,
                  new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(242, 188, 69), 1.5f));
            }
          }

          victim.setNoDamageTicks(0);
          victim.setHealth(Math.max(0.0, victim.getHealth() - finalDamage));

          for (int i = 0; i < 36; i++) {
            double circleAngle = Math.PI * 2 * (double) i / 36.0;
            raisedWorld.spawnParticle(
                org.bukkit.Particle.TRIAL_SPAWNER_DETECTION,
                raisedLoc.getX() + 2.5 * Math.cos(circleAngle),
                raisedLoc.getY(),
                raisedLoc.getZ() + 2.5 * Math.sin(circleAngle),
                1,
                0.0,
                0.0,
                0.0,
                0.0);
          }

          } finally {
            // Gravity is saved with the player; never leave them frozen in the air
            victim.setGravity(true);
          }
        },
        60L);

    float explosionPower =
        (float) configManager.getConfig().getDouble("hierarchy-hammer.combo-explosion-power", 0.0);
    if (explosionPower > 0) {
      SchedulerUtil.runAtLocationLater(
          plugin,
          victimLoc,
          () -> victimLoc.getWorld().createExplosion(victimLoc, explosionPower, false, true),
          60L);
    }

    UUID uuid = attacker.getUniqueId();
    resetSentencing(uuid);
    int verdictCooldownSec = configManager.getConfig().getInt("hierarchy-hammer.verdict-cooldown", 30);
    dashCooldowns.put(uuid, System.currentTimeMillis() + (verdictCooldownSec * 1000L));
    attacker.setCooldown(Material.MACE, verdictCooldownSec * 20);
  }

  public void spawnVerdictTerrainDebris(@NotNull Location impactLoc) {
    org.bukkit.World world = impactLoc.getWorld();
    if (world == null) return;

    java.util.List<org.bukkit.Material> blockTypes = new java.util.ArrayList<>();
    Location groundLoc = impactLoc.clone();
    groundLoc.setY(impactLoc.getY() - 1);

    for (int x = -2; x <= 2; x++) {
      for (int z = -2; z <= 2; z++) {
        org.bukkit.block.Block block = groundLoc.clone().add(x, 0, z).getBlock();
        if (block.getType() != org.bukkit.Material.AIR && block.getType().isSolid()) {
          blockTypes.add(block.getType());
        }
      }
    }

    if (blockTypes.isEmpty()) {
      blockTypes.add(org.bukkit.Material.STONE);
      blockTypes.add(org.bukkit.Material.COBBLESTONE);
      blockTypes.add(org.bukkit.Material.DIRT);
    }

    world.playSound(impactLoc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
    world.playSound(impactLoc, org.bukkit.Sound.ENTITY_WITHER_BREAK_BLOCK, 1.5f, 0.5f);

    int totalBlocks = 24;
    for (int i = 0; i < totalBlocks; i++) {
      double angle = (i * 2.0 * Math.PI) / totalBlocks;
      org.bukkit.Material blockType = blockTypes.get((int) (Math.random() * blockTypes.size()));
      Location spawnLoc = impactLoc.clone().add(0, 0.2, 0);

      org.bukkit.util.Vector direction =
          new org.bukkit.util.Vector(Math.cos(angle), 1.1 + (Math.random() * 0.4), Math.sin(angle));
      direction.normalize().multiply(0.65 + (Math.random() * 0.3));

      org.bukkit.entity.BlockDisplay blockDisplay =
          world.spawn(
              spawnLoc,
              org.bukkit.entity.BlockDisplay.class,
              display -> {
                display.setBlock(blockType.createBlockData());
                display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
                display.setTeleportDuration(1);
              });

      final int blockIndex = i;
      SchedulerUtil.runForEntityLater(
          plugin,
          blockDisplay,
          () -> {
            if (blockDisplay.isValid()) {
              world.playSound(
                  blockDisplay.getLocation(),
                  org.bukkit.Sound.BLOCK_STONE_BREAK,
                  0.8f,
                  0.7f + (float) (Math.random() * 0.4));
            }
          },
          blockIndex / 4);

      final int maxTicks = 55;
      final java.util.concurrent.atomic.AtomicInteger tickCounter =
          new java.util.concurrent.atomic.AtomicInteger(0);

      SchedulerUtil.runTimerForEntity(
          plugin,
          blockDisplay,
          handle -> {
            if (!blockDisplay.isValid() || tickCounter.get() >= maxTicks) {
              if (blockDisplay.isValid()) {
                blockDisplay.remove();
              }
              handle.cancel();
              return;
            }

            int currentTick = tickCounter.getAndIncrement();
            double progress = (double) currentTick / maxTicks;

            direction.setY(direction.getY() - 0.045);
            Location newLoc = blockDisplay.getLocation().add(direction);
            blockDisplay.teleportAsync(newLoc);

            org.joml.Quaternionf rotation = new org.joml.Quaternionf();
            rotation.rotateXYZ(
                (float) (currentTick * 0.25),
                (float) (currentTick * 0.2),
                (float) (currentTick * 0.15));

            float scale = 1.0f;
            if (progress > 0.55) {
              float fadeProgress = (float) ((progress - 0.55) / 0.45);
              scale = Math.max(0.0f, 1.0f - (fadeProgress * fadeProgress));
            }
            org.joml.Vector3f scaleVec = new org.joml.Vector3f(scale, scale, scale);

            blockDisplay.setTransformation(
                new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(0, 0, 0),
                    rotation,
                    scaleVec,
                    new org.joml.Quaternionf()));

            if (currentTick % 3 == 0) {
              blockDisplay
                  .getWorld()
                  .spawnParticle(
                      org.bukkit.Particle.BLOCK,
                      blockDisplay.getLocation(),
                      2,
                      0.1,
                      0.1,
                      0.1,
                      0.02,
                      blockType.createBlockData());
            }
          },
          0L,
          1L);
    }
  }

  public void resetSentencing(@NotNull UUID uuid) {
    sentencingActive.remove(uuid);
    sentencingStartTime.remove(uuid);
    dashCharges.remove(uuid);
    lastHitTime.remove(uuid);
    comboHits.remove(uuid);
    SchedulerUtil.TaskHandle task = dashTasks.remove(uuid);
    if (task != null) {
      task.cancel();
    }
  }

  public boolean isSentencingActive(@NotNull UUID uuid) {
    return sentencingActive.getOrDefault(uuid, false);
  }

  public long getLastHitTime(@NotNull UUID uuid) {
    return lastHitTime.getOrDefault(uuid, 0L);
  }

  public void setLastHitTime(@NotNull UUID uuid, long time) {
    lastHitTime.put(uuid, time);
  }

  public void addDashCharge(@NotNull UUID uuid) {
    dashCharges.merge(uuid, 1, Integer::sum);
  }

  public void onPlayerLand(@NotNull Player player) {
  }

  public void registerMiss(@NotNull Player player) {
    applyMissCooldown(player);
  }

  public boolean isOnMissCooldown(@NotNull Player player) {
    Long cooldownUntil = missCooldowns.get(player.getUniqueId());
    if (cooldownUntil == null) {
      return false;
    }
    if (System.currentTimeMillis() >= cooldownUntil) {
      missCooldowns.remove(player.getUniqueId());
      return false;
    }
    return true;
  }

  public boolean isOnDashCooldown(@NotNull Player player) {
    Long cooldownUntil = dashCooldowns.get(player.getUniqueId());
    if (cooldownUntil == null) {
      return false;
    }
    if (System.currentTimeMillis() >= cooldownUntil) {
      dashCooldowns.remove(player.getUniqueId());
      return false;
    }
    return true;
  }

  public boolean isOnCooldown(@NotNull Player player) {
    return isOnMissCooldown(player) || isOnDashCooldown(player);
  }

  public boolean hasFallProtection(@NotNull UUID uuid) {
    Long protectionUntil = dashProtection.get(uuid);
    if (protectionUntil == null) {
      return false;
    }
    if (System.currentTimeMillis() >= protectionUntil) {
      dashProtection.remove(uuid);
      return false;
    }
    return true;
  }

  public int getComboRequired() {
    return Math.max(1, configManager.getConfig().getInt("hierarchy-hammer.combo-required", 3));
  }

  public long getComboResetMillis() {
    return Math.max(0, configManager.getConfig().getInt("hierarchy-hammer.combo-reset-time", 5))
        * 1000L;
  }

  public int getComboHits(@NotNull UUID uuid) {
    return comboHits.getOrDefault(uuid, 0);
  }

  public void setComboHits(@NotNull UUID uuid, int hits) {
    comboHits.put(uuid, hits);
  }

}
