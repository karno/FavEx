package com.troidworks.bukkit.favEx

import org.bukkit.plugin.java.JavaPlugin
import scalaPluginExtension.ScalaPluginExtension
import scala.collection.mutable
import org.bukkit.ChatColor
import org.bukkit.command.{Command, CommandSender}
import org.bukkit.entity.Player
import java.util

/**
 * Created with IntelliJ IDEA.
 * User: Karno
 * Date: 13/02/10
 * Time: 0:46
 */
class FavEx extends JavaPlugin {
  var watchers = new mutable.HashMap[String, FavoriteWatcher]

  override def onEnable() {
    this.saveDefaultConfig()
    FavEx._instance = this
    super.onEnable()
    FavEx.writeLog("Activated FavEx.")
    ScalaPluginExtension.registerListener(this, new EventHandlers(this))
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

  def startWatching(player: CommandSender) {
    stopWatching(player)
    val name = player.getName
    val list = getConfig.getStringList(name)
    if (list == null || list.size != 3) return
    val watcher = new FavoriteWatcher(list.get(0).toLong, list.get(1), list.get(2),
      () => onFavorited(name), () => onUnfavorited(name), () => onRetweeted(name),
      s => notifyUser(name, s))
    if (watchers.exists(t => t._2.userId == watcher.userId)) {
      player.sendMessage(ChatColor.RED + "FavEx: Your account is already authenticated and connected by other user.")
    }
    else {
      watchers.put(name, watcher)
      watcher.startWatch()
      player.sendMessage(ChatColor.YELLOW + "FavEx: Started stargazing!")
    }
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
    lookupUser(player) match {
      case None => FavEx.writeLog("not found player " + player)
      case Some(p) => p.giveExp(getConfig.getInt(key))
    }
  }

  def notifyUser(player: String, msg: String) {
    lookupUser(player) match {
      case None => FavEx.writeLog("player " + player + " not found. message: " + msg)
      case Some(p) => p.sendMessage(msg)
    }
  }

  def stopWatching(player: CommandSender) {
    val name = player.getName
    watchers.get(name) match {
      case Some(watcher) =>
        watchers.remove(name)
        watcher.stopWatch()
        player.sendMessage(ChatColor.GRAY + "FavEx: Stopped observing.")
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
        startWatching(sender)
      case None =>
        sender.sendMessage(ChatColor.RED + "Please try again.")
    }
  }

  private def deauthorize(sender: CommandSender) {
    stopWatching(sender)
    getConfig.set(sender.getName, null)
    sender.sendMessage(ChatColor.RED + "deauthorized.")
  }

  def lookupUser(user: String, exact: Boolean = true): Option[Player] = {
    ScalaPluginExtension.lookupPlayerFromServer(this, user, exact) match {
      case null => None
      case user => Some(user)
    }
  }
}

object FavEx {
  val CONSUMER_KEY = "FPLesLh4ug3g3AeXfZ0PUg"
  val CONSUMER_SECRET = "7e4ySsWFgeAozri9c47sSFyZM95JtasmLNg4FAA8"

  private var _instance: FavEx = null

  def writeLog(log: String) {
    if (_instance != null) {
      _instance.getLogger.info(log)
    }
    else {
      System.out.println(log)
    }
  }
}
