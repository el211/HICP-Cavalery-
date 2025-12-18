package fr.oreo.hICPCavalry.util;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class EntityUtil {

    private EntityUtil() {}

    public static boolean isHorse(Entity e) {
        return e instanceof Horse;
    }

    public static boolean isCamel(Entity e) {
        return e != null && e.getType().name().equals("CAMEL");
    }

    public static Player firstPlayerPassenger(Entity mount) {
        for (Entity p : mount.getPassengers()) {
            if (p instanceof Player pl) return pl;
        }
        return null;
    }

    public static boolean isStorm(World w) {
        return w != null && (w.hasStorm() || w.isThundering());
    }

    public static Vector forwardProbe(Vector velocity) {
        Vector v = velocity == null ? new Vector(0, 0, 0) : velocity.clone();
        v.setY(0);
        if (v.lengthSquared() < 1.0E-6) return null;
        return v.normalize().multiply(0.75);
    }

    public static boolean isWater(Material m) { return m == Material.WATER; }
    public static boolean isSnowyBlock(Material m) { return m == Material.SNOW || m == Material.SNOW_BLOCK || m == Material.POWDER_SNOW; }
    public static boolean isMud(Material m) { return m == Material.MUD; }
    public static boolean isMuddyMangroveRoots(Material m) { return m == Material.MUDDY_MANGROVE_ROOTS; }
    public static boolean isLava(Material m) { return m == Material.LAVA; }
    public static boolean isMagma(Material m) { return m == Material.MAGMA_BLOCK; }

    public static Block blockAt(LivingEntity ent, Vector offset) {
        if (ent == null || offset == null) return null;
        return ent.getLocation().add(offset).getBlock();
    }
}
