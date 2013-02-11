package com.troidworks.bukkit.favEx

import collection.mutable
import org.bukkit.command.CommandSender
import org.bukkit.ChatColor
import java.util.concurrent.Executors
import twitter4j.{TwitterException, TwitterFactory}
import twitter4j.auth.{AccessToken, RequestToken}
import java.net.{HttpURLConnection, URL}
import java.io.BufferedInputStream

/**
 * Created with IntelliJ IDEA.
 * User: Karno
 * Date: 13/02/11
 * Time: 0:20
 * To change this template use File | Settings | File Templates.
 */
object Authorizer {
  private val executor = Executors.newCachedThreadPool()
  private val twitter = TwitterFactory.getSingleton
  private val tokens = new mutable.HashMap[String, RequestToken]()

  twitter.setOAuthConsumer(FavEx.CONSUMER_KEY, FavEx.CONSUMER_SECRET)

  def startAuthorize(sender: CommandSender) {
    sender.sendMessage(ChatColor.GRAY + "Starting your authorization...")
    executor.execute(new Runnable {
      def run() {
        try {
          twitter.setOAuthAccessToken(null)
          val token = twitter.getOAuthRequestToken
          tokens.put(sender.getName, token)
          sender.sendMessage(Array(ChatColor.YELLOW + "Open the following URL and grant access to your account.",
            "Click THIS -> " + ChatColor.BLUE + ShortenUrl(token.getAuthorizationURL),
            "And enter command " + ChatColor.YELLOW + "/favex auth [PIN]" + ChatColor.WHITE + " ."))
        } catch {
          case ex: TwitterException =>
            sender.sendMessage(ChatColor.RED + "Twitter returns error: " +
              ex.getStatusCode + ":" + ex.getMessage)
          case ex: Exception =>
            sender.sendMessage(ChatColor.RED + "Exception has occured: " + ex.getMessage)
        }
      }
    })
  }

  private def ShortenUrl(url: String): String = {
    val con = new URL("http://is.gd/create.php?format=simple&url=" + url)
      .openConnection()
      .asInstanceOf[HttpURLConnection]
    try {
      con.connect()
      val bis = new BufferedInputStream(con.getInputStream)

      lazy val reader: () => Stream[Char] = () => bis.read() match {
        case -1 => Stream.empty[Char]
        case ch => Stream.cons(ch.asInstanceOf[Char], reader())
      }

      reader()
        .map(_.toString)
        .reduceLeft(_ + _)
    } finally {
      con.disconnect()
    }
  }

  def finishAuthorize(sender: CommandSender, pin: String): Option[AccessToken] = {
    tokens.get(sender.getName) match {
      case Some(token) =>
        try {
          val accessToken = twitter.getOAuthAccessToken(token, pin)
          tokens.remove(sender.getName)
          return Some(accessToken)
        } catch {
          case ex: TwitterException =>
            sender.sendMessage(ChatColor.RED + "Twitter returns error: " +
              ex.getStatusCode + ":" + ex.getMessage)
          case ex: Exception =>
            sender.sendMessage(ChatColor.RED + "Exception has occured: " + ex.getMessage)
        }
        sender.sendMessage(ChatColor.RED + "Please try again a few seconds later.")
        None
      case None =>
        sender.sendMessage(Array(
          ChatColor.RED + "Your authorization token is not found.",
          ChatColor.RED + "Please try again from the start."))
        None
    }
  }
}
