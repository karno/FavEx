package com.troidworks.bukkit.favEx

import twitter4j._
import twitter4j.conf.ConfigurationBuilder
import java.net.SocketException
import java.io.IOException
import org.bukkit.ChatColor

/**
 * Created with IntelliJ IDEA.
 * User: Karno
 * Date: 13/02/10
 * Time: 0:50
 */
class FavoriteWatcher(id: Long, token: String, tokenSecret: String,
                      favoriteHandler: () => Unit, unfavoriteHandler: () => Unit,
                      retweetHandler: () => Unit, userNotifyHandler: String => Unit) {
  private var _watching = false
  private var _stream: TwitterStream = null
  private var _backstate = ConnectWaitingState.Unexpected

  def userId = id

  def isWatching = _watching

  def startWatch() {
    _watching = true
    FavEx.writeLog("starting connection ID: " + id.toString)
    tryConnect()
  }

  private def tryConnect() {
    val builder = new ConfigurationBuilder()
    builder.setDebugEnabled(false)
    builder.setOAuthConsumerKey(FavEx.CONSUMER_KEY)
    builder.setOAuthConsumerSecret(FavEx.CONSUMER_SECRET)
    builder.setOAuthAccessToken(token)
    builder.setOAuthAccessTokenSecret(tokenSecret)
    builder.setUserStreamBaseURL("https://userstream.twitter.com/1.1/")
    val conf = builder.build()
    val factory = new TwitterStreamFactory(conf)
    _stream = factory.getInstance()
    _stream.addListener(new UserStreamAdapter {
      override def onFavorite(source: User, target: User, favoritedStatus: Status) {
        if (source.getId == target.getId) return
        favoriteHandler()
      }

      override def onUnfavorite(source: User, target: User, unfavoritedStatus: Status) {
        if (source.getId == target.getId) return
        unfavoriteHandler()
      }

      override def onStatus(status: Status) {
        if (!status.isRetweet || !(status.getRetweetedStatus.getUser.getId == id)) return
        retweetHandler()
      }

      override def onException(ex: Exception) {
        FavEx.writeLog("Twitter throws exception: " + ex.getMessage)
        ex match {
          case te: TwitterException => te.getErrorCode match {
            case ec if ec >= 400 =>
              // error
              FavEx.writeLog("Twitter returns error code " + ec)
              _backstate = ConnectWaitingState.WithServerError
            case _ =>
              _backstate = ConnectWaitingState.WithException
          }
          case _: SocketException | _: IOException =>
            _backstate = ConnectWaitingState.WithException
          case _ => ConnectWaitingState.Unexpected
        }
        super.onException(ex)
      }
    })
    _stream.addConnectionLifeCycleListener(new ConnectionLifeCycleListener {
      def onConnect() {
        _backstate = ConnectWaitingState.Unexpected
      }

      def onCleanUp() {}

      def onDisconnect() {
        if (_watching) {
          // unexpected disconnection
          FavEx.writeLog("<!>A User Streams connection is unexpectedly disconnected. ID: " + id)
          _backstate match {
            case ConnectWaitingState.Unexpected =>
              Thread.sleep(5000)
            case ConnectWaitingState.WithException =>
              userNotifyHandler(ChatColor.RED + "FavEx: Your stargazing is interrupted.")
              userNotifyHandler(ChatColor.RED + "Stargazing will be retry 30 seconds later.")
              Thread.sleep(30000)
            case ConnectWaitingState.WithServerError =>
              userNotifyHandler(ChatColor.RED + "FavEx: Your stargazing is interrupted with Twitter Error.")
              userNotifyHandler(ChatColor.RED + "Stargazing will be retry a few minutes later.")
              Thread.sleep(180000)
          }
          FavEx.writeLog("Trying reconnect. ID: " + id)
          tryConnect()
        }
      }
    })
    _stream.user()
  }

  def stopWatch() {
    _watching = false
    _stream.shutdown()
  }
}

object ConnectWaitingState extends Enumeration {
  val Unexpected, WithException, WithServerError = Value

}