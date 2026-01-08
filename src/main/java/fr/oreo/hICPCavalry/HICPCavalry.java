package fr.oreo.hICPCavalry;

import fr.oreo.hICPCavalry.config.CavalryConfig;
import fr.oreo.hICPCavalry.listener.MountListener;
import fr.oreo.hICPCavalry.listener.SpawnListener;
import fr.oreo.hICPCavalry.listener.VehicleMoveListener;
import fr.oreo.hICPCavalry.service.LeashSinkingService;
import fr.oreo.hICPCavalry.service.MountStatService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class HICPCavalry extends JavaPlugin {

    private CavalryConfig cfg;
    private MountStatService statService;
    private LeashSinkingService leashSinkingService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.cfg = new CavalryConfig(getConfig());
        this.statService = new MountStatService(this, cfg);
        leashSinkingService = new LeashSinkingService(this, cfg);
        getServer().getPluginManager().registerEvents(leashSinkingService, this);
        leashSinkingService.start();

        Bukkit.getPluginManager().registerEvents(new SpawnListener(this, cfg, statService), this);
        Bukkit.getPluginManager().registerEvents(new MountListener(this, cfg, statService), this);
        Bukkit.getPluginManager().registerEvents(new VehicleMoveListener(this, cfg), this);

        statService.start();

        if (cfg.debugEnabled) {
            getLogger().info("HICP_Cavalry enabled with DEBUG MODE active!");
            getLogger().info("Debug categories enabled:");
            if (cfg.debugHorseNormalization || cfg.debugHorseStatCalculations || cfg.debugHorsePdcStorage)
                getLogger().info("  - Horses");
            if (cfg.debugCamelPenaltyApplication || cfg.debugCamelProcessing)
                getLogger().info("  - Camels");
            if (cfg.debugArmorPoints || cfg.debugArmorPenaltyCalculations)
                getLogger().info("  - Armor");
            if (cfg.debugEnvironmentBlockDetection || cfg.debugEnvironmentPenaltyCalculations || cfg.debugEnvironmentWeatherChecks)
                getLogger().info("  - Environment");
            if (cfg.debugCombatReachBonus || cfg.debugCombatModifierChanges)
                getLogger().info("  - Combat");
            if (cfg.debugTraversalCliffDetection || cfg.debugTraversalWaterChecks || cfg.debugTraversalHazardDetection || cfg.debugTraversalMovementBlocked)
                getLogger().info("  - Traversal");
            if (cfg.debugPerformanceTickTiming || cfg.debugPerformanceMountState)
                getLogger().info("  - Performance");
        } else {
            getLogger().info("HICP_Cavalry enabled.");
        }
    }

    @Override
    public void onDisable() {
        if (leashSinkingService != null) leashSinkingService.stop();

        if (statService != null) statService.stop();
        getLogger().info("HICP_Cavalry disabled.");
    }
}