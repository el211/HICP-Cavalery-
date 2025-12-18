package fr.oreo.hICPCavalry.service;

import fr.oreo.hICPCavalry.config.CavalryConfig;
import fr.oreo.hICPCavalry.keys.Keys;
import fr.oreo.hICPCavalry.util.ArmorPoints;
import fr.oreo.hICPCavalry.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.logging.Logger;

public final class MountStatService {

    private final Plugin plugin;
    private final Logger logger;
    private final CavalryConfig cfg;
    private final Keys keys;
    private final NamespacedKey reachKey;

    private BukkitTask task;

    public MountStatService(Plugin plugin, CavalryConfig cfg) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.cfg = cfg;
        this.keys = new Keys(plugin);
        this.reachKey = new NamespacedKey(plugin, "sword_reach");
    }

    public void start() {
        int period = Math.max(1, cfg.refreshPeriodTicks);
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);

        if (cfg.debugEnabled) {
            logger.info("[Debug] MountStatService started with refresh period: " + period + " ticks");
        }
    }

    public void stop() {
        if (task != null) task.cancel();

        if (cfg.debugEnabled) {
            logger.info("[Debug] MountStatService stopped");
        }
    }

    public void normalizeHorseOnSpawn(Horse h) {
        if (!cfg.horsesEnabled || h == null) return;

        if (cfg.debugEnabled && cfg.debugHorseNormalization) {
            logger.info("[Debug] Normalizing horse: " + h.getUniqueId());
        }

        // (1) Max health
        AttributeInstance maxHealth = h.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            double oldHealth = maxHealth.getBaseValue();
            maxHealth.setBaseValue(cfg.horseSpawnMaxHealth);

            if (cfg.debugEnabled && cfg.debugHorseStatCalculations) {
                logger.info("[Debug]   Max Health: " + oldHealth + " -> " + cfg.horseSpawnMaxHealth);
            }

            if (cfg.horseFillHealth) {
                double newHealth = Math.min(cfg.horseSpawnMaxHealth, h.getMaxHealth());
                h.setHealth(newHealth);

                if (cfg.debugEnabled && cfg.debugHorseStatCalculations) {
                    logger.info("[Debug]   Health filled to: " + newHealth);
                }
            }
        }

        // (2) Speed cap
        AttributeInstance ms = h.getAttribute(Attribute.MOVEMENT_SPEED);
        if (ms != null) {
            double oldSpeed = ms.getBaseValue();
            double desiredAttr = cfg.horseCapBps / Math.max(1e-6, cfg.speedToBpsFactor);
            double newSpeed = clamp(desiredAttr, cfg.clampMin, cfg.clampMax);
            ms.setBaseValue(newSpeed);

            if (cfg.debugEnabled && cfg.debugHorseStatCalculations) {
                logger.info("[Debug]   Speed: " + oldSpeed + " -> " + newSpeed +
                        " (target BPS: " + cfg.horseCapBps + ")");
            }
        }

        // (3) Universal base jump height
        AttributeInstance js = h.getAttribute(Attribute.JUMP_STRENGTH);
        if (js != null) {
            double oldJump = js.getBaseValue();
            double desiredJumpStrength = cfg.horseBaseJumpBlocks / Math.max(1e-6, cfg.jumpStrengthToBlocksFactor);
            double newJump = Math.max(0.05, desiredJumpStrength);
            js.setBaseValue(newJump);

            if (cfg.debugEnabled && cfg.debugHorseStatCalculations) {
                logger.info("[Debug]   Jump: " + oldJump + " -> " + newJump +
                        " (target blocks: " + cfg.horseBaseJumpBlocks + ")");
            }
        }

        if (cfg.horseStoreBasePdc) {
            storeBaseStatsIfMissing(h);
        }

        h.getPersistentDataContainer().set(keys.HORSE_NORMALIZED, PersistentDataType.BYTE, (byte) 1);

        if (cfg.debugEnabled && cfg.debugHorseNormalization) {
            logger.info("[Debug] Horse normalization complete: " + h.getUniqueId());
        }
    }

    private void tick() {
        long startTime = cfg.debugEnabled && cfg.debugPerformanceTickTiming ? System.nanoTime() : 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            Entity mount = p.getVehicle();

            if (!(mount instanceof LivingEntity le)) {
                applySwordReach(p, false);
                continue;
            }

            // Respect enable flags
            boolean horse = cfg.horsesEnabled && (mount instanceof Horse);
            boolean camel = cfg.camelsEnabled && ((mount instanceof Camel) || EntityUtil.isCamel(mount));

            if (!horse && !camel) {
                applySwordReach(p, false);
                continue;
            }

            // Only apply speed/jump penalties if allowed:
            // - horses: yes (when horses enabled)
            // - camels: ONLY if camels.apply_penalties = true
            boolean penaltiesAllowed = horse || (camel && cfg.camelApplyPenalties);

            if (cfg.debugEnabled && cfg.debugCamelProcessing && camel) {
                logger.info("[Debug] Camel processing - penalties allowed: " + penaltiesAllowed +
                        " (apply_penalties setting: " + cfg.camelApplyPenalties + ")");
            }

            // Only store base stats if we might modify them
            if (penaltiesAllowed) {
                if (horse && cfg.horseStoreBasePdc) storeBaseStatsIfMissing(le);
                if (camel && cfg.camelStoreBasePdc) storeBaseStatsIfMissing(le);

                if (cfg.armorEnabled || cfg.envEnabled) {
                    applyMountMultipliers(p, le);
                }
            }

            // Mounted combat reach: applies while on an allowed mount type
            if (cfg.combatEnabled) {
                boolean hasSword = isSword(p.getInventory().getItemInMainHand());
                applySwordReach(p, hasSword);
            } else {
                applySwordReach(p, false);
            }
        }

        if (cfg.debugEnabled && cfg.debugPerformanceTickTiming) {
            long elapsed = System.nanoTime() - startTime;
            logger.info("[Debug] Tick execution time: " + (elapsed / 1000000.0) + "ms");
        }
    }

    private void applyMountMultipliers(Player rider, LivingEntity mount) {
        double baseSpeed = getBaseSpeed(mount);
        double baseJump = getBaseJump(mount);

        if (baseSpeed <= 0) baseSpeed = readAttributeBase(mount, Attribute.MOVEMENT_SPEED);
        if (baseJump <= 0) baseJump = readAttributeBase(mount, Attribute.JUMP_STRENGTH);

        int playerPts = cfg.debugEnabled && cfg.debugArmorPoints ?
                ArmorPoints.getPlayerArmorPointsWithDebug(rider, cfg.leatherCountsAsZero, logger, true) :
                ArmorPoints.getPlayerArmorPoints(rider, cfg.leatherCountsAsZero);

        int mountPts = 0;

        if (mount instanceof Horse h) {
            mountPts = cfg.debugEnabled && cfg.debugArmorPoints ?
                    ArmorPoints.getHorseArmorPointsWithDebug(h, cfg.horseArmorPoints, cfg.leatherCountsAsZero, logger, true) :
                    ArmorPoints.getHorseArmorPoints(h, cfg.horseArmorPoints, cfg.leatherCountsAsZero);
        }

        if (cfg.debugEnabled && cfg.debugArmorPoints) {
            logger.info("[Debug] Total armor points: " + (playerPts + mountPts));
        }

        // Separate reductions so config matches behavior 100%
        double speedPct = 0.0;
        double jumpPct = 0.0;

        if (cfg.armorEnabled) {
            int totalPts = playerPts + mountPts;
            double armorSpeedPct = totalPts * cfg.speedPenaltyPerPointPct;
            double armorJumpPct = totalPts * cfg.jumpPenaltyPerPointPct;
            speedPct += armorSpeedPct;
            jumpPct += armorJumpPct;

            if (cfg.debugEnabled && cfg.debugArmorPenaltyCalculations) {
                logger.info("[Debug] Armor penalties - Speed: " + armorSpeedPct + "%, Jump: " + armorJumpPct + "%");
            }
        }

        if (cfg.envEnabled) {
            double env = environmentExtraPct(mount);
            // Environment penalty applies to both speed & jump equally
            speedPct += env;
            jumpPct += env;

            if (cfg.debugEnabled && cfg.debugEnvironmentPenaltyCalculations && env > 0) {
                logger.info("[Debug] Environment penalty: " + env + "% (added to both speed and jump)");
            }
        }

        speedPct = Math.min(speedPct, cfg.maxTotalReductionPct);
        jumpPct = Math.min(jumpPct, cfg.maxTotalReductionPct);

        double speedMult = 1.0 - (speedPct / 100.0);
        double jumpMult = 1.0 - (jumpPct / 100.0);

        if (speedMult < 0) speedMult = 0;
        if (jumpMult < 0) jumpMult = 0;

        double finalSpeed = clamp(baseSpeed * speedMult, cfg.clampMin, cfg.clampMax);
        double finalJump = Math.max(0.05, baseJump * jumpMult);

        if (cfg.debugEnabled && cfg.debugHorseStatCalculations) {
            logger.info("[Debug] Applying multipliers - Base speed: " + baseSpeed + ", Base jump: " + baseJump);
            logger.info("[Debug] Final multipliers - Speed: " + speedMult + " (" + speedPct + "% penalty), " +
                    "Jump: " + jumpMult + " (" + jumpPct + "% penalty)");
            logger.info("[Debug] Final values - Speed: " + finalSpeed + ", Jump: " + finalJump);
        }

        setAttributeBase(mount, Attribute.MOVEMENT_SPEED, finalSpeed);
        setAttributeBase(mount, Attribute.JUMP_STRENGTH, finalJump);
    }

    private double environmentExtraPct(LivingEntity mount) {
        boolean storm = EntityUtil.isStorm(mount.getWorld());
        Material below = mount.getLocation().subtract(0, 0.1, 0).getBlock().getType();

        if (cfg.debugEnabled && cfg.debugEnvironmentWeatherChecks) {
            logger.info("[Debug] Environment check - Storm: " + storm + ", Block below: " + below);
        }

        double extra = 0.0;

        if (cfg.envMud && EntityUtil.isMud(below)) {
            if (!cfg.requireStormMud || storm) {
                extra += cfg.envExtraReductionPct;

                if (cfg.debugEnabled && cfg.debugEnvironmentBlockDetection) {
                    logger.info("[Debug] Mud detected - Adding " + cfg.envExtraReductionPct + "% penalty");
                }
            }
        }

        if (cfg.envMuddyRoots && EntityUtil.isMuddyMangroveRoots(below)) {
            if (!cfg.requireStormMud || storm) {
                extra += cfg.envExtraReductionPct;

                if (cfg.debugEnabled && cfg.debugEnvironmentBlockDetection) {
                    logger.info("[Debug] Muddy mangrove roots detected - Adding " + cfg.envExtraReductionPct + "% penalty");
                }
            }
        }

        if (cfg.envSnow && EntityUtil.isSnowyBlock(below)) {
            if (!cfg.requireStormSnow || storm) {
                extra += cfg.envExtraReductionPct;

                if (cfg.debugEnabled && cfg.debugEnvironmentBlockDetection) {
                    logger.info("[Debug] Snow detected - Adding " + cfg.envExtraReductionPct + "% penalty");
                }
            }
        }

        return extra;
    }

    private void storeBaseStatsIfMissing(LivingEntity e) {
        PersistentDataContainer pdc = e.getPersistentDataContainer();

        if (!pdc.has(keys.BASE_SPEED, PersistentDataType.DOUBLE)) {
            double speed = readAttributeBase(e, Attribute.MOVEMENT_SPEED);
            pdc.set(keys.BASE_SPEED, PersistentDataType.DOUBLE, speed);

            if (cfg.debugEnabled && cfg.debugHorsePdcStorage) {
                logger.info("[Debug] Stored base speed in PDC: " + speed + " for " + e.getUniqueId());
            }
        }

        if (!pdc.has(keys.BASE_JUMP, PersistentDataType.DOUBLE)) {
            double jump = readAttributeBase(e, Attribute.JUMP_STRENGTH);
            pdc.set(keys.BASE_JUMP, PersistentDataType.DOUBLE, jump);

            if (cfg.debugEnabled && cfg.debugHorsePdcStorage) {
                logger.info("[Debug] Stored base jump in PDC: " + jump + " for " + e.getUniqueId());
            }
        }
    }

    private double getBaseSpeed(LivingEntity e) {
        Double v = e.getPersistentDataContainer().get(keys.BASE_SPEED, PersistentDataType.DOUBLE);
        return v == null ? -1 : v;
    }

    private double getBaseJump(LivingEntity e) {
        Double v = e.getPersistentDataContainer().get(keys.BASE_JUMP, PersistentDataType.DOUBLE);
        return v == null ? -1 : v;
    }

    private static double readAttributeBase(LivingEntity e, Attribute a) {
        AttributeInstance ai = e.getAttribute(a);
        return ai == null ? 0.0 : ai.getBaseValue();
    }

    private static void setAttributeBase(LivingEntity e, Attribute a, double v) {
        AttributeInstance ai = e.getAttribute(a);
        if (ai != null) ai.setBaseValue(v);
    }

    /**
     * 1.21+: use keyed AttributeModifier API (no UUID constructor, no getUniqueId()).
     */
    private void applySwordReach(Player p, boolean enable) {
        AttributeInstance reach = p.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (reach == null) return;

        // Remove previous modifier by key
        boolean hadModifier = false;
        for (AttributeModifier m : reach.getModifiers()) {
            if (reachKey.equals(m.getKey())) {
                reach.removeModifier(m);
                hadModifier = true;

                if (cfg.debugEnabled && cfg.debugCombatModifierChanges) {
                    logger.info("[Debug] Removed reach modifier from player " + p.getName());
                }
                break;
            }
        }

        if (enable && cfg.swordReachBonus > 0.0) {
            AttributeModifier mod = new AttributeModifier(
                    reachKey,
                    cfg.swordReachBonus,
                    AttributeModifier.Operation.ADD_NUMBER
            );
            reach.addModifier(mod);

            if (cfg.debugEnabled && cfg.debugCombatModifierChanges) {
                logger.info("[Debug] Added reach modifier +" + cfg.swordReachBonus + " to player " + p.getName());
            }
        } else if (cfg.debugEnabled && cfg.debugCombatModifierChanges && hadModifier) {
            logger.info("[Debug] Reach modifier removed (no sword held) for player " + p.getName());
        }
    }

    private boolean isSword(ItemStack it) {
        if (it == null) return false;
        Material m = it.getType();
        String n = m.name().toLowerCase(Locale.ROOT);
        if (!n.endsWith("_sword")) return false;
        if (!cfg.onlyVanillaSwords) return true;

        return switch (m) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}