package com.troidworks.bukkit.favEx

import org.bukkit.plugin.java.JavaPlugin
import scalaPluginExtension.ScalaPluginExtension
import scala.collection.mutable
import org.bukkit.ChatColor
import org.bukkit.command.{Command, CommandSender}
import java.util

/**
 * Created with IntelliJ IDEA.
 * User: Karno
 * Date: 13/02/10
 * Time: 0:46
 * To change this template use File | Settings | File Templates.
 */
class FavEx extends JavaPlugin {
  var watchers = new mutable.HashMap[String, FavoriteWatcher]

  override def onEnable() {
    this.saveDefaultConfig()
    FavEx._instance = this;
    super.onEnable()
    FavEx.writeLog("Activated FavEx.")
    ScalaPluginExtension.registerListener(this, new EventHandlers(this));
  }

  override def onDisable() {
    super.onDisable()
    this.saveConfig()
  }

  override def onCommand(sender: CommandSender, command: Command,
                         label: String, args: Array[String]): Boolean = {
    command.getName.toLowerCase match {
      case "favex" =>
        processCommand(sender, args)
      case _ =>
        false
    }
  }

  def processCommand(sender: CommandSender, args: Array[String]): Boolean = {
    args.size match {
      case 2 =>
        List(args(0), args(1)) match {
          case List("auth", pin) => finishAuth(sender, pin); true
          case _ => false
        }
      case 1 =>
        args(0).toLowerCase match {
          case "auth" | "reauth" => startAuth(sender); true
          case "deauth" => deauthorize(sender); true
          case _ => false
        }
      case _ => false
    }
  }

  def startWatching(player: String) {
    stopWatching(player)
    val list = getConfig.getStringList(player);
    if (list == null || list.size != 3) return;
    val watcher = new FavoriteWatcher(list.get(0).toLong, list.get(1), list.get(2),
      () => onFavorited(player), () => onUnfavorited(player), () => onRetweeted(player));
    watchers.put(player, watcher)
    watcher.startWatch()
    ScalaPluginExtension.lookupPlayerFromServer(this, player).sendMessage(ChatColor.GREEN + "FavEx: Started.")
  }

  def onFavorited(player: String) {
    giveExp(player, "exp_per_favorite")
  }

  def onUnfavorited(player: String) {
    giveExp(player, "exp_per_unfavorite")
  }

  def onRetweeted(player: String) {
    giveExp(player, "exp_per_retweet")
  }

  def giveExp(player: String, key: String) {
    ScalaPluginExtension.lookupPlayerFromServer(this, player).giveExp(getConfig.getInt(key))
  }

  def stopWatching(player: String) {
    watchers.get(player) match {
      case Some(watcher) =>
        watcher.stopWatch()
        watchers.remove(player)
        ScalaPluginExtension.lookupPlayerFromServer(this, player).sendMessage(ChatColor.GREEN + "FavEx: Stopped.")
      case None => Unit
    }
  }

  private def startAuth(sender: CommandSender) {
    Authorizer.startAuthorize(sender)
  }

  private def finishAuth(sender: CommandSender, pin: String) {
    Authorizer.finishAuthorize(sender, pin) match {
      case Some(token) =>
        val save = new util.ArrayList[String]()
        save.add(token.getUserId.toString)
        save.add(token.getToken)
        save.add(token.getTokenSecret)
        getConfig.set(sender.getName, save)
        sender.sendMessage(ChatColor.GREEN + "Successfully authorized as " + ChatColor.BLUE + "@" + token.getScreenName)
        startWatching(sender.getName)
      case None =>
        sender.sendMessage(ChatColor.RED + "Please try again.")
    }
  }

  private def deauthorize(sender: CommandSender) {
    stopWatching(sender.getName)
    getConfig.set(sender.getName, null)
    sender.sendMessage(ChatColor.RED + "deauthorized.")
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
