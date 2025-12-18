package fr.oreo.hICPCavalry.listener;

import fr.oreo.hICPCavalry.config.CavalryConfig;
import fr.oreo.hICPCavalry.service.MountStatService;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public final class SpawnListener implements Listener {

    private final Plugin plugin;
    private final Logger logger;
    private final CavalryConfig cfg;
    private final MountStatService statService;

    public SpawnListener(Plugin plugin, CavalryConfig cfg, MountStatService statService) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.cfg = cfg;
        this.statService = statService;
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        if (!cfg.horsesEnabled) return;
        if (e.getEntityType() != EntityType.HORSE) return;

        Horse horse = (Horse) e.getEntity();

        if (cfg.debugEnabled && cfg.debugHorseNormalization) {
            logger.info("[Debug] Horse spawned via " + e.getSpawnReason() + " at " +
                    e.getLocation().getBlockX() + "," + e.getLocation().getBlockY() + "," + e.getLocation().getBlockZ());
        }

        statService.normalizeHorseOnSpawn(horse);
    }
}