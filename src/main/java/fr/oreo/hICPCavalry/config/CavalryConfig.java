package fr.oreo.hICPCavalry.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

public final class CavalryConfig {

    public final boolean debugEnabled;
    public final boolean debugHorseNormalization;
    public final boolean debugHorseStatCalculations;
    public final boolean debugHorsePdcStorage;
    public final boolean debugCamelPenaltyApplication;
    public final boolean debugCamelProcessing;
    public final boolean debugArmorPoints;
    public final boolean debugArmorPenaltyCalculations;
    public final boolean debugEnvironmentBlockDetection;
    public final boolean debugEnvironmentPenaltyCalculations;
    public final boolean debugEnvironmentWeatherChecks;
    public final boolean debugCombatReachBonus;
    public final boolean debugCombatModifierChanges;
    public final boolean debugTraversalCliffDetection;
    public final boolean debugTraversalWaterChecks;
    public final boolean debugTraversalHazardDetection;
    public final boolean debugTraversalMovementBlocked;
    public final boolean debugPerformanceTickTiming;
    public final boolean debugPerformanceMountState;
    public final double horseArmorPointMultiplier;

    public final boolean horsesEnabled;
    public final double horseSpawnMaxHealth;
    public final boolean horseFillHealth;
    public final double horseCapBps;
    public final double speedToBpsFactor;
    public final boolean leadSinkEnabled;
    public final int leadSinkStartHorseArmorPoints;
    public final double leadSinkDownVelocityPerTick;
    public final int leadSinkTaskPeriodTicks;

    public final double horseBaseJumpBlocks;
    public final double jumpStrengthToBlocksFactor;

    public final double clampMin;
    public final double clampMax;
    public final boolean horseStoreBasePdc;

    public final boolean camelsEnabled;
    public final boolean camelStoreBasePdc;

    /**
     * If false, camels remain vanilla (no speed/jump penalties applied).
     * Traversal rules and mounted combat (reach) can still apply to camels.
     *
     * Config key: camels.apply_penalties (default: false)
     */
    public final boolean camelApplyPenalties;

    public final boolean armorEnabled;
    public final double speedPenaltyPerPointPct;
    public final double jumpPenaltyPerPointPct;
    public final double maxTotalReductionPct;
    public final boolean leatherCountsAsZero;
    public final Map<Material, Integer> horseArmorPoints;

    public final boolean envEnabled;
    public final double envExtraReductionPct;
    public final boolean envMud;
    public final boolean envMuddyRoots;
    public final boolean envSnow;
    public final boolean requireStormMud;
    public final boolean requireStormSnow;

    public final boolean traversalEnabled;
    public final double cliffDropBlocks;
    public final double hazardScanDepth;
    public final boolean hazardLava;
    public final boolean hazardMagma;

    public final boolean waterEnabled;
    public final double waterRefuseDepthAtLeast;
    public final boolean waterAntiStuck;

    public final boolean combatEnabled;
    public final double swordReachBonus;
    public final boolean onlyVanillaSwords;

    public final int refreshPeriodTicks;

    public CavalryConfig(FileConfiguration c) {
        // Debug settings
        debugEnabled = c.getBoolean("debug.enabled", false);
        debugHorseNormalization = c.getBoolean("debug.horses.normalization", true);
        debugHorseStatCalculations = c.getBoolean("debug.horses.stat_calculations", true);
        debugHorsePdcStorage = c.getBoolean("debug.horses.pdc_storage", true);
        debugCamelPenaltyApplication = c.getBoolean("debug.camels.penalty_application", true);
        debugCamelProcessing = c.getBoolean("debug.camels.processing", true);
        debugArmorPoints = c.getBoolean("debug.armor.armor_points", true);
        debugArmorPenaltyCalculations = c.getBoolean("debug.armor.penalty_calculations", true);
        debugEnvironmentBlockDetection = c.getBoolean("debug.environment.block_detection", true);
        debugEnvironmentPenaltyCalculations = c.getBoolean("debug.environment.penalty_calculations", true);
        debugEnvironmentWeatherChecks = c.getBoolean("debug.environment.weather_checks", true);
        debugCombatReachBonus = c.getBoolean("debug.combat.reach_bonus", true);
        debugCombatModifierChanges = c.getBoolean("debug.combat.modifier_changes", true);
        debugTraversalCliffDetection = c.getBoolean("debug.traversal.cliff_detection", true);
        debugTraversalWaterChecks = c.getBoolean("debug.traversal.water_checks", true);
        debugTraversalHazardDetection = c.getBoolean("debug.traversal.hazard_detection", true);
        debugTraversalMovementBlocked = c.getBoolean("debug.traversal.movement_blocked", true);
        debugPerformanceTickTiming = c.getBoolean("debug.performance.tick_timing", false);
        debugPerformanceMountState = c.getBoolean("debug.performance.mount_state", false);

        horsesEnabled = c.getBoolean("horses.enabled", true);
        horseSpawnMaxHealth = c.getDouble("horses.spawn_max_health", 30.0);
        horseFillHealth = c.getBoolean("horses.spawn_health_fill", true);
        horseCapBps = c.getDouble("horses.cap_speed_blocks_per_second", 7.127);
        speedToBpsFactor = c.getDouble("horses.movement_speed_to_bps_factor", 20.0);

        horseBaseJumpBlocks = c.getDouble("horses.base_jump_height_blocks", 3.0);
        jumpStrengthToBlocksFactor = c.getDouble("horses.jump_strength_to_blocks_factor", 5.0);

        clampMin = c.getDouble("horses.clamp_attribute_min", 0.05);
        clampMax = c.getDouble("horses.clamp_attribute_max", 0.60);
        horseStoreBasePdc = c.getBoolean("horses.store_base_stats_in_pdc", true);

        camelsEnabled = c.getBoolean("camels.enabled", true);
        camelStoreBasePdc = c.getBoolean("camels.store_base_stats_in_pdc", true);

        // camels stay vanilla unless explicitly allowed
        camelApplyPenalties = c.getBoolean("camels.apply_penalties", false);

        armorEnabled = c.getBoolean("armor_penalties.enabled", true);
        speedPenaltyPerPointPct = c.getDouble("armor_penalties.speed_reduction_per_armor_point_percent", 1.0);
        jumpPenaltyPerPointPct = c.getDouble("armor_penalties.jump_reduction_per_armor_point_percent", 1.0);
        maxTotalReductionPct = c.getDouble("armor_penalties.max_total_reduction_percent", 60.0);
        leatherCountsAsZero = c.getBoolean("armor_penalties.leather_counts_as_zero", true);
        horseArmorPointMultiplier = c.getDouble("armor_penalties.horse_armor_point_multiplier", 1.0);
        leadSinkEnabled = c.getBoolean("traversal_rules.water.sinking_on_lead.enabled", true);
        leadSinkStartHorseArmorPoints = c.getInt("traversal_rules.water.sinking_on_lead.start_sinking_at_horse_armor_points", 7);
        leadSinkDownVelocityPerTick = c.getDouble("traversal_rules.water.sinking_on_lead.down_velocity_per_tick", 0.08);
        leadSinkTaskPeriodTicks = c.getInt("traversal_rules.water.sinking_on_lead.task_period_ticks", 1);

        horseArmorPoints = new EnumMap<>(Material.class);
        ConfigurationSection hap = c.getConfigurationSection("armor_penalties.horse_armor_points");
        if (hap != null) {
            for (String k : hap.getKeys(false)) {
                Material m = Material.matchMaterial(k);
                if (m != null) horseArmorPoints.put(m, hap.getInt(k));
            }
        }

        envEnabled = c.getBoolean("environment_penalties.enabled", true);
        envExtraReductionPct = c.getDouble("environment_penalties.extra_reduction_percent", 5.0);

        envMud = c.getBoolean("environment_penalties.blocks.mud", true);
        envMuddyRoots = c.getBoolean("environment_penalties.blocks.muddy_mangrove_roots", true);
        envSnow = c.getBoolean("environment_penalties.blocks.snow", true);

        requireStormMud = c.getBoolean("environment_penalties.require_storm_for_mud", true);
        requireStormSnow = c.getBoolean("environment_penalties.require_storm_for_snow", true);

        traversalEnabled = c.getBoolean("traversal_rules.enabled", true);
        cliffDropBlocks = c.getDouble("traversal_rules.cliff_drop_blocks", 2.5);
        hazardScanDepth = c.getDouble("traversal_rules.hazard_scan_depth_blocks", 2.5);
        hazardLava = c.getBoolean("traversal_rules.hazards.lava", true);
        hazardMagma = c.getBoolean("traversal_rules.hazards.magma_block", true);

        waterEnabled = c.getBoolean("traversal_rules.water.enabled", true);
        waterRefuseDepthAtLeast = c.getDouble(
                "traversal_rules.water.refuse_if_depth_at_least_blocks",
                2.0
        );
        waterAntiStuck = c.getBoolean("traversal_rules.water.anti_stuck_allow_if_all_neighbors_water", true);

        combatEnabled = c.getBoolean("mounted_combat.enabled", true);
        swordReachBonus = c.getDouble("mounted_combat.sword_reach_bonus_blocks", 1.0);
        onlyVanillaSwords = c.getBoolean("mounted_combat.apply_only_to_vanilla_swords", true);

        refreshPeriodTicks = c.getInt("performance.stat_refresh_period_ticks", 2);
    }
}