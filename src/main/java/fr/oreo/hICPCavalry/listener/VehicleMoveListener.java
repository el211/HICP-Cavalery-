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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public final class VehicleMoveListener implements Listener {

    private final Plugin plugin;
    private final Logger logger;
    private final CavalryConfig cfg;
    private int debugCounter = 0;

    // Track last warning time AND last rear time to avoid spam/stacking
    private final Map<UUID, Long> lastWarningTime = new HashMap<>();
    private final Map<UUID, Long> lastRearTime = new HashMap<>();
    private static final long WARNING_COOLDOWN = 500; // 500ms between warnings
    private static final long REAR_COOLDOWN = 1000; // 1 second between rears

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
            logger.info("[Debug] PlayerMoveEvent is firing! (counter: " + debugCounter + ")");
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

        // Check if player actually moved
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;

        // Calculate horizontal movement
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalMovement = Math.sqrt(dx * dx + dz * dz);

        if (cfg.debugEnabled) {
            logger.info("[Debug] TRAVERSAL: Player " + player.getName() + " on " + v.getType());
            logger.info("[Debug] TRAVERSAL: Horizontal movement: " + horizontalMovement + " (threshold: 0.001)");
        }

        if (horizontalMovement < 0.001) return;

        // Calculate movement direction
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        Vector probe = new Vector(direction.getX(), 0, direction.getZ()).normalize();

        if (probe.lengthSquared() < 0.01) {
            if (cfg.debugEnabled) {
                logger.info("[Debug] TRAVERSAL: Invalid probe direction");
            }
            return;
        }

        Block front = EntityUtil.blockAt(le, probe);
        if (front == null) {
            if (cfg.debugEnabled) {
                logger.info("[Debug] TRAVERSAL: Front block is null");
            }
            return;
        }

        if (cfg.debugEnabled) {
            logger.info("[Debug] TRAVERSAL CHECK: Player " + player.getName() + " on " + v.getType() +
                    ", front block: " + front.getType() + " at " + front.getLocation());
        }

        // Water checks
        if (cfg.waterEnabled && EntityUtil.isWater(front.getType())) {
            if (cfg.debugEnabled) {
                logger.info("[Debug] WATER DETECTED ahead!");
            }

            if (shouldRefuseWater(front, le)) {
                blockMovement(e, v, player, "§9Deep water ahead!", Sound.ENTITY_HORSE_BREATHE);
                return;
            } else {
                if (cfg.debugEnabled) {
                    logger.info("[Debug] Water entry allowed (shallow or at water edge)");
                }
            }
        }

        // Cliff detection
        if (cfg.cliffDropBlocks > 0) {
            if (cfg.debugEnabled) {
                logger.info("[Debug] CHECKING FOR CLIFF (limit: " + cfg.cliffDropBlocks + " blocks)");
            }

            if (isCliffDrop(front, cfg.cliffDropBlocks)) {
                blockMovement(e, v, player, "§c⚠ Cliff ahead!", Sound.ENTITY_HORSE_ANGRY);
                return;
            } else {
                if (cfg.debugEnabled) {
                    logger.info("[Debug] No cliff detected");
                }
            }
        }

        // Hazard detection
        if (cfg.hazardScanDepth > 0 && (cfg.hazardLava || cfg.hazardMagma)) {
            if (hasHazardBelow(front, cfg.hazardScanDepth)) {
                blockMovement(e, v, player, "§6Hazard below!", Sound.BLOCK_FIRE_AMBIENT);
            }
        }
    }

    /**
     * FIXED: Horse rears ONCE with proper cooldown to prevent velocity stacking
     */
    private void blockMovement(PlayerMoveEvent e, Entity vehicle, Player player, String message, Sound sound) {
        // ALWAYS cancel the movement event to stop forward motion
        e.setCancelled(true);

        // ALWAYS zero out velocity
        vehicle.setVelocity(new Vector(0, 0, 0));

        long now = System.currentTimeMillis();
        UUID vehicleId = vehicle.getUniqueId();

        // Check if we can rear (prevent velocity stacking)
        Long lastRear = lastRearTime.get(vehicleId);
        boolean canRear = (lastRear == null || (now - lastRear) > REAR_COOLDOWN);

        if (canRear) {
            // Make the horse rear ONCE
            if (vehicle instanceof AbstractHorse) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (vehicle.isValid() && !vehicle.isDead()) {
                            // Small upward velocity to simulate rearing
                            vehicle.setVelocity(new Vector(0, 0.3, 0));
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }

            // Mark that we reared
            lastRearTime.put(vehicleId, now);

            if (cfg.debugEnabled) {
                logger.info("[Debug] Horse reared at " + now);
            }
        } else {
            if (cfg.debugEnabled) {
                logger.info("[Debug] Rear on cooldown, skipping to prevent rocket launch");
            }
        }

        // Sound and message feedback (separate cooldown)
        UUID playerId = player.getUniqueId();
        Long lastWarning = lastWarningTime.get(playerId);

        if (lastWarning == null || (now - lastWarning) > WARNING_COOLDOWN) {
            // Play sound at horse location
            vehicle.getWorld().playSound(vehicle.getLocation(), sound, 1.0f, 1.0f);

            // Show action bar message
            player.sendActionBar(message);

            lastWarningTime.put(playerId, now);

            if (cfg.debugEnabled) {
                logger.info("[Debug] Movement BLOCKED: " + message.replaceAll("§.", ""));
            }
        }
    }

    /**
     * Fixed anti-stuck logic: only allow movement when horse is already in water
     */
    private boolean shouldRefuseWater(Block front, LivingEntity vehicle) {
        int depthN = Math.max(2, cfg.waterRefuseDepthAtLeast);
        if (depthN <= 1) return false;

        // Check if water is deep (at least 2 blocks)
        boolean deep = EntityUtil.isWater(front.getType()) &&
                EntityUtil.isWater(front.getRelative(0, -1, 0).getType());

        if (cfg.debugEnabled) {
            logger.info("[Debug] Water depth check - Deep water: " + deep);
        }

        if (!deep) return false;

        // Anti-stuck: allow movement ONLY if horse is already in water
        if (cfg.waterAntiStuck) {
            Block blockBelow = vehicle.getLocation().getBlock().getRelative(0, -1, 0);
            boolean horseIsInWater = EntityUtil.isWater(blockBelow.getType());

            if (cfg.debugEnabled) {
                logger.info("[Debug] Anti-stuck check - Horse currently in water: " + horseIsInWater);
            }

            // Horse in water = allow movement (can swim out)
            if (horseIsInWater) {
                return false;
            }

            // Horse on land = block deep water entry
            if (cfg.debugEnabled) {
                logger.info("[Debug] Blocking water entry from land");
            }
            return true;
        }

        return true;
    }

    private boolean isCliffDrop(Block front, double dropLimit) {
        if (!front.isPassable() && front.getType() != Material.AIR && front.getType() != Material.WATER) {
            if (cfg.debugEnabled) {
                logger.info("[Debug] Cliff check - Block not passable: " + front.getType());
            }
            return false;
        }

        int maxScan = (int) Math.ceil(dropLimit + 3.0);
        Block cursor = front;
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

    private boolean hasHazardBelow(Block front, double depth) {
        int scan = (int) Math.ceil(depth);
        Block cursor = front;

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
                    logger.info("[Debug] Hazard detected - Magma block at depth " + i + " blocks");
                }
                return true;
            }

            cursor = cursor.getRelative(0, -1, 0);
        }

        return false;
    }
}