package com.troidworks.bukkit.favEx

import twitter4j._
import twitter4j.conf.ConfigurationBuilder

/**
 * Created with IntelliJ IDEA.
 * User: Karno
 * Date: 13/02/10
 * Time: 0:50
 */
class FavoriteWatcher(id: Long, token: String, tokenSecret: String,
                      favoriteHandler: () => Unit, unfavoriteHandler: () => Unit, retweetHandler: () => Unit) {
  var _watching = false
  var _stream: TwitterStream = null

  def isWatching = _watching

  def startWatch() {
    _watching = true
    tryConnect()
  }

  def tryConnect() {
    val builder = new ConfigurationBuilder()
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
    })
    _stream.addConnectionLifeCycleListener(new ConnectionLifeCycleListener {
      def onConnect() {
      }

      def onCleanUp() {}

      def onDisconnect() {
        if (_watching) {
          // unexpected disconnection
          Thread.sleep(5000)
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
