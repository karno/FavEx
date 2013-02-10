package com.troidworks.bukkit.favEx

import org.bukkit.event.{EventHandler, EventPriority, Listener}
import org.bukkit.event.player.{PlayerLoginEvent, PlayerQuitEvent}

/**
 * Created with IntelliJ IDEA.
 * User: Karno
 * Date: 13/02/10
 * Time: 0:49
 * To change this template use File | Settings | File Templates.
 */
class EventHandlers(plugin: FavEx) extends Listener {
  @EventHandler(priority = EventPriority.HIGH)
  def onPlayerLogin(e: PlayerLoginEvent) {
    plugin.startWatching(e.getPlayer.getName)
  }

  @EventHandler(priority = EventPriority.HIGH)
  def onPlayerLogout(e: PlayerQuitEvent) {
    plugin.stopWatching(e.getPlayer.getName)
  }
}
