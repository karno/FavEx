package com.troidworks.bukkit.favEx.scalaPluginExtension;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created with IntelliJ IDEA.
 * User: Karno
 * Date: 13/02/10
 * Time: 1:09
 * To change this template use File | Settings | File Templates.
 */
public final class ScalaPluginExtension {
    public static Player lookupPlayerFromServer(JavaPlugin plugin, String name) {
        return lookupPlayerFromServer(plugin, name, false);
    }

    public static Player lookupPlayerFromServer(JavaPlugin plugin, String name, boolean exact) {
        if (exact)
            return plugin.getServer().getPlayerExact(name);
        else
            return plugin.getServer().getPlayer(name);
    }

    public static void registerListener(JavaPlugin plugin, Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }
}
