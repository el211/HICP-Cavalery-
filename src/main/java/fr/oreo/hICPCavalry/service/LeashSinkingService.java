package fr.oreo.hICPCavalry.service;

import fr.oreo.hICPCavalry.config.CavalryConfig;
import fr.oreo.hICPCavalry.util.ArmorPoints;
import fr.oreo.hICPCavalry.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public final class LeashSinkingService implements Listener {

    private final Plugin plugin;
    private final Logger logger;
    private final CavalryConfig cfg;

    private final Set<UUID> tracked = new HashSet<>();
    private BukkitTask task;

    public LeashSinkingService(Plugin plugin, CavalryConfig cfg) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.cfg = cfg;
    }

    public void start() {
        int period = Math.max(1, cfg.leadSinkTaskPeriodTicks);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);

        if (cfg.debugEnabled) {
            logger.info("[Debug] LeashSinkingService started (period=" + period + ")");
        }
    }

    public void stop() {
        if (task != null) task.cancel();
        tracked.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent e) {
        if (!cfg.leadSinkEnabled) return;
        Entity ent = e.getEntity();
        if (!(ent instanceof Horse)) return;
        tracked.add(ent.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnleash(PlayerUnleashEntityEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof Horse)) return;
        tracked.remove(ent.getUniqueId());
    }

    private void tick() {
        if (!cfg.leadSinkEnabled) return;
        if (tracked.isEmpty()) return;

        Set<UUID> snapshot = new HashSet<>(tracked);

        for (UUID id : snapshot) {
            Entity ent = Bukkit.getEntity(id);
            if (!(ent instanceof Horse horse) || !ent.isValid() || ent.isDead()) {
                tracked.remove(id);
                continue;
            }

            // Only when being led (no rider + leashed)
            if (!horse.getPassengers().isEmpty()) continue;

            if (!horse.isLeashed()) {
                tracked.remove(id);
                continue;
            }

            // In water check (feet/below)
            Material feet = horse.getLocation().getBlock().getType();
            Material below = horse.getLocation().getBlock().getRelative(0, -1, 0).getType();
            if (!EntityUtil.isWater(feet) && !EntityUtil.isWater(below)) continue;

            int pts = ArmorPoints.getHorseArmorPoints(horse, cfg.horseArmorPoints, cfg.leatherCountsAsZero);

            // Leather should be leadable, copper+ should sink
            if (pts < cfg.leadSinkStartHorseArmorPoints) continue;

            Vector vel = horse.getVelocity();
            double down = -Math.abs(cfg.leadSinkDownVelocityPerTick);

            // Don’t fight stronger downward movement
            double newY = Math.min(vel.getY(), down);

            horse.setVelocity(new Vector(vel.getX(), newY, vel.getZ()));

            if (cfg.debugEnabled && cfg.debugTraversalWaterChecks) {
                logger.info("[Debug] Lead sink applied: horsePts=" + pts + ", yVel=" + newY);
            }
        }
    }
}
