package fr.oreo.hICPCavalry.util;

import fr.oreo.hICPCavalry.config.CavalryConfig;
import org.bukkit.Material;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.logging.Logger;

public final class ArmorPoints {

    private ArmorPoints() {}

    public static int getPlayerArmorPoints(Player p, boolean leatherCountsAsZero) {
        if (p == null || p.getInventory() == null) return 0;

        int pts = 0;
        pts += armorPiecePoints(p.getInventory().getHelmet(), leatherCountsAsZero);
        pts += armorPiecePoints(p.getInventory().getChestplate(), leatherCountsAsZero);
        pts += armorPiecePoints(p.getInventory().getLeggings(), leatherCountsAsZero);
        pts += armorPiecePoints(p.getInventory().getBoots(), leatherCountsAsZero);
        return pts;
    }

    public static int getPlayerArmorPointsWithDebug(Player p, boolean leatherCountsAsZero, Logger logger, boolean debugEnabled) {
        if (p == null || p.getInventory() == null) return 0;

        int helmet = armorPiecePoints(p.getInventory().getHelmet(), leatherCountsAsZero);
        int chest = armorPiecePoints(p.getInventory().getChestplate(), leatherCountsAsZero);
        int legs = armorPiecePoints(p.getInventory().getLeggings(), leatherCountsAsZero);
        int boots = armorPiecePoints(p.getInventory().getBoots(), leatherCountsAsZero);

        int total = helmet + chest + legs + boots;

        if (debugEnabled && logger != null) {
            logger.info("[Debug] Player " + p.getName() + " armor breakdown:");
            logger.info("[Debug]   Helmet: " + helmet + " (" + getMaterialName(p.getInventory().getHelmet()) + ")");
            logger.info("[Debug]   Chestplate: " + chest + " (" + getMaterialName(p.getInventory().getChestplate()) + ")");
            logger.info("[Debug]   Leggings: " + legs + " (" + getMaterialName(p.getInventory().getLeggings()) + ")");
            logger.info("[Debug]   Boots: " + boots + " (" + getMaterialName(p.getInventory().getBoots()) + ")");
            logger.info("[Debug]   Total: " + total + " armor points");
        }

        return total;
    }

    public static int getHorseArmorPoints(Horse h, Map<Material, Integer> mapping, boolean leatherCountsAsZero) {
        if (h == null || h.getInventory() == null) return 0;
        ItemStack armor = h.getInventory().getArmor();
        if (armor == null) return 0;

        if (leatherCountsAsZero && armor.getType() == Material.LEATHER_HORSE_ARMOR) return 0;
        return mapping.getOrDefault(armor.getType(), 0);
    }

    public static int getHorseArmorPointsWithDebug(Horse h, Map<Material, Integer> mapping, boolean leatherCountsAsZero, Logger logger, boolean debugEnabled) {
        if (h == null || h.getInventory() == null) return 0;
        ItemStack armor = h.getInventory().getArmor();

        if (armor == null) {
            if (debugEnabled && logger != null) {
                logger.info("[Debug] Horse armor: NONE (0 points)");
            }
            return 0;
        }

        int points = 0;
        if (leatherCountsAsZero && armor.getType() == Material.LEATHER_HORSE_ARMOR) {
            points = 0;
        } else {
            points = mapping.getOrDefault(armor.getType(), 0);
        }

        if (debugEnabled && logger != null) {
            logger.info("[Debug] Horse armor: " + armor.getType() + " = " + points + " points");
        }

        return points;
    }

    private static int armorPiecePoints(ItemStack it, boolean leatherCountsAsZero) {
        if (it == null) return 0;
        Material m = it.getType();

        if (leatherCountsAsZero) {
            if (m == Material.LEATHER_HELMET ||
                    m == Material.LEATHER_CHESTPLATE ||
                    m == Material.LEATHER_LEGGINGS ||
                    m == Material.LEATHER_BOOTS) {
                return 0;
            }
        }

        if (m == Material.LEATHER_HELMET) return 1;
        if (m == Material.LEATHER_CHESTPLATE) return 3;
        if (m == Material.LEATHER_LEGGINGS) return 2;
        if (m == Material.LEATHER_BOOTS) return 1;

        if (m == Material.CHAINMAIL_HELMET) return 2;
        if (m == Material.CHAINMAIL_CHESTPLATE) return 5;
        if (m == Material.CHAINMAIL_LEGGINGS) return 4;
        if (m == Material.CHAINMAIL_BOOTS) return 1;

        if (m == Material.IRON_HELMET) return 2;
        if (m == Material.IRON_CHESTPLATE) return 6;
        if (m == Material.IRON_LEGGINGS) return 5;
        if (m == Material.IRON_BOOTS) return 2;

        if (m == Material.GOLDEN_HELMET) return 2;
        if (m == Material.GOLDEN_CHESTPLATE) return 5;
        if (m == Material.GOLDEN_LEGGINGS) return 3;
        if (m == Material.GOLDEN_BOOTS) return 1;

        if (m == Material.DIAMOND_HELMET) return 3;
        if (m == Material.DIAMOND_CHESTPLATE) return 8;
        if (m == Material.DIAMOND_LEGGINGS) return 6;
        if (m == Material.DIAMOND_BOOTS) return 3;

        if (m == Material.NETHERITE_HELMET) return 3;
        if (m == Material.NETHERITE_CHESTPLATE) return 8;
        if (m == Material.NETHERITE_LEGGINGS) return 6;
        if (m == Material.NETHERITE_BOOTS) return 3;

        if (m == Material.TURTLE_HELMET) return 2;

        return 0;
    }

    private static String getMaterialName(ItemStack it) {
        if (it == null || it.getType() == Material.AIR) return "NONE";
        return it.getType().toString();
    }
}