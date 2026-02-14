package fr.oreo.hICPCavalry.listener;

import fr.oreo.hICPCavalry.config.CavalryConfig;
import fr.oreo.hICPCavalry.util.EntityUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class VehicleMoveListener implements Listener {

    private final Plugin plugin;
    private final Logger logger;
    private final CavalryConfig cfg;
    private int debugCounter = 0;

    private final Map<UUID, Long> lastWarningTime = new HashMap<>();
    private final Map<UUID, Long> lastRearTime = new HashMap<>();
    private static final long WARNING_COOLDOWN = 500;
    private static final long REAR_COOLDOWN = 1000;

    private final Map<UUID, Location> lastSafeLocation = new HashMap<>();

    private static final boolean ENABLE_BOUNCE_BACK = true;
    private static final double BOUNCE_BACK_STRENGTH = 0.25;
    public VehicleMoveListener(Plugin plugin, CavalryConfig cfg) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.cfg = cfg;
        logger.info("[VehicleMoveListener] Listener initialized!");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerMove(PlayerMoveEvent e) {
        debugCounter++;
        if (debugCounter % 100 == 0) {
        }

        if (!cfg.traversalEnabled) return;

        Player player = e.getPlayer();
        if (!player.isInsideVehicle()) return;

        Entity v = player.getVehicle();
        if (v == null) return;

        boolean isHorse = EntityUtil.isHorse(v);
        boolean isCamel = EntityUtil.isCamel(v) || v.getType().name().equals("CAMEL");
        if (!isHorse && !isCamel) return;

        if (!(v instanceof LivingEntity le)) return;

        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;


        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalMovement = Math.sqrt(dx * dx + dz * dz);

        if (cfg.debugEnabled) {
            logger.info("[Debug] TRAVERSAL: Player " + player.getName() + " on " + v.getType());
            logger.info("[Debug] TRAVERSAL: Horizontal movement: " + horizontalMovement + " (threshold: 0.001)");
        }

        if (horizontalMovement < 0.001) return;


        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        Vector probe = new Vector(direction.getX(), 0, direction.getZ()).normalize();

        if (probe.lengthSquared() < 0.01) {
            if (cfg.debugEnabled) {
                logger.info("[Debug] TRAVERSAL: Invalid probe direction");
            }
            return;
        }

        Block frontBody = EntityUtil.blockAt(le, probe);
        if (frontBody == null) {
            if (cfg.debugEnabled) {
                logger.info("[Debug] TRAVERSAL: Front body block is null");
            }
            return;
        }

        Block frontFeet = frontBody.getRelative(0, -1, 0);

        if (cfg.debugEnabled) {
            logger.info("[Debug] TRAVERSAL CHECK: Player " + player.getName() + " on " + v.getType() +
                    ", frontBody: " + frontBody.getType() + " @ " + frontBody.getLocation() +
                    ", frontFeet: " + frontFeet.getType() + " @ " + frontFeet.getLocation());
        }


        if (cfg.waterEnabled && EntityUtil.isWater(frontFeet.getType())) {
            if (cfg.debugEnabled) {
                logger.info("[Debug] WATER DETECTED ahead (feet)!");
            }

            if (shouldRefuseWater(frontFeet, le)) {
                blockMovement(e, v, player, "§9Deep water ahead!", Sound.ENTITY_HORSE_BREATHE, probe);
                return;
            } else if (cfg.debugEnabled) {
                logger.info("[Debug] Water entry allowed (shallow / edge / anti-stuck)");
            }
        }


        if (cfg.cliffDropBlocks > 0) {
            if (cfg.debugEnabled) {
                logger.info("[Debug] CHECKING FOR CLIFF (limit: " + cfg.cliffDropBlocks + " blocks)");
            }

            if (isCliffDrop(frontFeet, cfg.cliffDropBlocks)) {
                blockMovement(e, v, player, "§c⚠ Cliff ahead!", Sound.ENTITY_HORSE_ANGRY, probe);
                return;
            } else if (cfg.debugEnabled) {
                logger.info("[Debug] No cliff detected");
            }
        }

        if (cfg.hazardScanDepth > 0 && (cfg.hazardLava || cfg.hazardMagma)) {
            if (hasHazardBelow(frontFeet, cfg.hazardScanDepth)) {
                blockMovement(e, v, player, "§6Hazard below!", Sound.BLOCK_FIRE_AMBIENT, probe);
                return;
            }
        }

        lastSafeLocation.put(v.getUniqueId(), v.getLocation().clone());
    }


    private void blockMovement(PlayerMoveEvent e, Entity vehicle, Player player, String message, Sound sound, Vector probe) {
        e.setCancelled(true);

        Location safe = lastSafeLocation.get(vehicle.getUniqueId());
        if (safe != null) {
            vehicle.teleport(safe);
        }

        vehicle.setVelocity(new Vector(0, 0, 0));

        if (ENABLE_BOUNCE_BACK && probe != null && probe.lengthSquared() > 0.0001) {
            Vector back = probe.clone().multiply(-BOUNCE_BACK_STRENGTH);
            back.setY(0.05);
            vehicle.setVelocity(back);
        }

        long now = System.currentTimeMillis();
        UUID vehicleId = vehicle.getUniqueId();

        Long lastRear = lastRearTime.get(vehicleId);
        boolean canRear = (lastRear == null || (now - lastRear) > REAR_COOLDOWN);

        if (canRear) {
            if (vehicle instanceof AbstractHorse) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (vehicle.isValid() && !vehicle.isDead()) {
                            // small upward velocity to simulate rearing (keep small)
                            vehicle.setVelocity(new Vector(0, 0.25, 0));
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }

            lastRearTime.put(vehicleId, now);

            if (cfg.debugEnabled) {
                logger.info("[Debug] Horse reared at " + now);
            }
        } else if (cfg.debugEnabled) {
            logger.info("[Debug] Rear on cooldown, skipping to prevent rocket launch");
        }

        UUID playerId = player.getUniqueId();
        Long lastWarning = lastWarningTime.get(playerId);

        if (lastWarning == null || (now - lastWarning) > WARNING_COOLDOWN) {
            vehicle.getWorld().playSound(vehicle.getLocation(), sound, 1.0f, 1.0f);
            player.sendActionBar(message);

            lastWarningTime.put(playerId, now);

            if (cfg.debugEnabled) {
                logger.info("[Debug] Movement BLOCKED: " + message.replaceAll("§.", ""));
            }
        }
    }


    private boolean shouldRefuseWater(Block waterStartFeet, LivingEntity vehicle) {
        double depthN = Math.max(0.5, cfg.waterRefuseDepthAtLeast);

        int depth = 0;
        Block cursor = waterStartFeet;
        for (int i = 0; i < depthN; i++) {
            if (!EntityUtil.isWater(cursor.getType())) break;
            depth++;
            cursor = cursor.getRelative(0, -1, 0);
        }

        boolean deep = depth >= depthN;

        if (cfg.debugEnabled) {
            logger.info("[Debug] Water depth check - depth=" + depth + ", min=" + depthN + ", deep=" + deep);
        }

        if (!deep) return false;

        if (cfg.waterAntiStuck) {
            Block below = vehicle.getLocation().getBlock().getRelative(0, -1, 0);
            boolean horseIsInWater = EntityUtil.isWater(below.getType());

            if (cfg.debugEnabled) {
                logger.info("[Debug] Anti-stuck check - Horse currently in water: " + horseIsInWater);
            }

            if (horseIsInWater) return false;
            return true;
        }

        return true;
    }


    private boolean isCliffDrop(Block stepBlockFeet, double dropLimit) {
        Material typeHere = stepBlockFeet.getType();
        if (!stepBlockFeet.isPassable() && typeHere != Material.AIR && typeHere != Material.WATER) {
            if (cfg.debugEnabled) {
                logger.info("[Debug] Cliff check - Step block not passable (ground exists): " + typeHere);
            }
            return false;
        }

        int maxScan = (int) Math.ceil(dropLimit + 3.0);
        Block cursor = stepBlockFeet;
        int drop = 0;

        for (int i = 0; i < maxScan; i++) {
            cursor = cursor.getRelative(0, -1, 0);
            Material type = cursor.getType();

            if (!cursor.isPassable() && type != Material.AIR && type != Material.WATER) {
                break;
            }

            drop++;
        }

        boolean isCliff = drop > dropLimit;

        if (cfg.debugEnabled) {
            logger.info("[Debug] Cliff check - Drop distance: " + drop + " blocks, Limit: " + dropLimit +
                    ", Is cliff: " + isCliff);
        }

        return isCliff;
    }

    private boolean hasHazardBelow(Block startFeet, double depth) {
        int scan = (int) Math.ceil(depth);
        Block cursor = startFeet;

        for (int i = 0; i <= scan; i++) {
            Material t = cursor.getType();

            if (cfg.hazardLava && EntityUtil.isLava(t)) {
                if (cfg.debugEnabled) {
                    logger.info("[Debug] Hazard detected - Lava at depth " + i + " blocks");
                }
                return true;
            }

            if (cfg.hazardMagma && EntityUtil.isMagma(t)) {
                if (cfg.debugEnabled) {
                    logger.info("[Debug] Hazard detected - Magma at depth " + i + " blocks");
                }
                return true;
            }

            cursor = cursor.getRelative(0, -1, 0);
        }

        return false;
    }
}
