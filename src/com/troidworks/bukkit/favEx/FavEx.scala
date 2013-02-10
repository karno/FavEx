package com.troidworks.bukkit.favEx

import org.bukkit.plugin.java.JavaPlugin
import playerLookup.PlayerLookup
import scala.collection.mutable
import org.bukkit.ChatColor
import org.bukkit.command.{Command, CommandSender}

/**
 * Created with IntelliJ IDEA.
 * User: Karno
 * Date: 13/02/10
 * Time: 0:46
 * To change this template use File | Settings | File Templates.
 */
class FavEx extends JavaPlugin {
  var watchers = new mutable.HashMap[String, FavoriteWatcher]
  var tokens = new mutable.HashMap[String, String]()

  override def onEnable() {
    FavEx._instance = this;
    super.onEnable()
    FavEx.writeLog("Activated FavEx.")
  }

  override def onDisable() {
    super.onDisable()
  }

  override def onCommand(sender: CommandSender, command: Command,
                         label: String, args: Array[String]): Boolean = {
    command.getName.toLowerCase match {
      case "favex" =>
        processCommand(sender, args)
        true

      case _ =>
        false
    }
  }

  def processCommand(sender: CommandSender, args: Array[String]) {
    args.size match {
      case 2 =>
        List(args(0), args(1)) match {
          case List("auth", pin) => updateAuthInfo(sender, pin)
          case _ => printUsage(sender)
        }
      case 1 =>
        args(0).toLowerCase match {
          case "auth" | "reauth" => auth(sender)
          case "deauth" => deauthorize(sender)
          case "help" | _ => printUsage(sender)
        }
      case _ => printUsage(sender)
    }
  }

  def startWatching(player: String) {
    stopWatching(player)
    val list = getConfig.getStringList(player);
    if (list == null || list.size != 2) return;
    val watcher = new FavoriteWatcher(list.get(0), list.get(1),
      () => onFavorited(player));
    watchers.put(player, watcher)
    watcher.startWatch()
    PlayerLookup.lookupPlayerFromServer(this, player).sendMessage(ChatColor.GREEN + "FavEx: Started.")
  }

  def onFavorited(player: String) {

  }

  def stopWatching(player: String) {
    val watcher = watchers.get(player)
    if (watcher.isEmpty) return;
    watcher.get.stopWatch();
    watchers.remove(player)
    PlayerLookup.lookupPlayerFromServer(this, player).sendMessage(ChatColor.GREEN + "FavEx: Stopped.")
  }

  private def auth(sender: CommandSender) {
    startWatching(sender.getName)
  }

  private def updateAuthInfo(sender: CommandSender, pin: String) {

  }

  private def deauthorize(sender: CommandSender) {
    stopWatching(sender.getName)
    getConfig.set(sender.getName, null)
    sender.sendMessage(ChatColor.RED + "deauthorized.")
  }

  private def printUsage(sender: CommandSender) {
    sender.sendMessage(
      Array(
        ChatColor.GREEN + "HOW TO USE FavEx:",
        ChatColor.BLUE + "/favex auth" +
          ChatColor.WHITE + ": authorize you and start watching favorites.",
        ChatColor.BLUE + "/favex deauth" +
          ChatColor.WHITE + ": remove your authorize.",
        ChatColor.BLUE + "/favex reauth" +
          ChatColor.WHITE + ": update your authorize information."
      ))
  }
}

object FavEx {
  val CONSUMER_KEY = "FPLesLh4ug3g3AeXfZ0PUg"
  val CONSUMER_SECRET = "7e4ySsWFgeAozri9c47sSFyZM95JtasmLNg4FAA8"

  private var _instance: FavEx = null;

  def writeLog(log: String) {
    if (_instance != null) {
      _instance.getLogger().info(log)
    }
    else {
      System.out.println(log)
    }
  }
}
