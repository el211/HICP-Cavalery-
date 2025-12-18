package fr.oreo.hICPCavalry.listener;

import fr.oreo.hICPCavalry.config.CavalryConfig;
import fr.oreo.hICPCavalry.service.MountStatService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public final class MountListener implements Listener {

    private final Plugin plugin;
    private final Logger logger;
    private final CavalryConfig cfg;
    private final MountStatService statService;

    public MountListener(Plugin plugin, CavalryConfig cfg, MountStatService statService) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.cfg = cfg;
        this.statService = statService;
    }

    @EventHandler
    public void onEnter(VehicleEnterEvent e) {
        if (!(e.getEntered() instanceof Player player)) return;

        Entity v = e.getVehicle();
        if (v instanceof Horse h && cfg.horsesEnabled) {
            if (cfg.debugEnabled && cfg.debugHorseNormalization) {
                logger.info("[Debug] Player " + player.getName() + " mounting horse " + h.getUniqueId());
            }

            statService.normalizeHorseOnSpawn(h);
        }
    }
}