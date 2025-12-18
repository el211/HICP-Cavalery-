package fr.oreo.hICPCavalry.keys;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {
    public final NamespacedKey BASE_SPEED;
    public final NamespacedKey BASE_JUMP;
    public final NamespacedKey HORSE_NORMALIZED;

    public Keys(Plugin plugin) {
        BASE_SPEED = new NamespacedKey(plugin, "base_speed");
        BASE_JUMP = new NamespacedKey(plugin, "base_jump");
        HORSE_NORMALIZED = new NamespacedKey(plugin, "horse_normalized");
    }
}
