package nyc.arash.homescreen.apps

import android.content.pm.ApplicationInfo

/** The groups All Apps is divided into, in the order they're shown. */
enum class AppCategory(val title: String) {
    STREAMING("Streaming"),
    LIVE("Live TV"),
    KIDS("Kids"),
    MUSIC("Music"),
    PHOTOS("Photos & video"),
    GAMES("Games"),
    NEWS("News"),
    OTHER("Other");

    companion object {
        /** Well-known TV apps (Fire TV and Android TV package names), where the app's own category is missing or vague. */
        private val BY_PACKAGE = mapOf(
            "com.netflix.ninja" to STREAMING, "com.amazon.firebat" to STREAMING, "com.amazon.avod" to STREAMING,
            "com.disney.disneyplus" to STREAMING, "com.cbs.ott" to STREAMING, "com.apple.atve.amazon.appletv" to STREAMING,
            "com.hulu.plus" to STREAMING, "com.wbd.stream" to STREAMING, "com.peacocktv.peacockandroid" to STREAMING,
            "com.amazon.firetv.youtube" to STREAMING, "tv.twitch.android.app" to STREAMING,
            "tv.twitch.android.viewer" to STREAMING, "com.tubitv.ott" to STREAMING,
            AppRepository.LIVE_TV_ID to LIVE, "com.amazon.firetv.youtube.tv" to LIVE, "tv.pluto.android" to LIVE,
            "com.fox.foxone" to LIVE, "com.espn.gtv" to LIVE, "com.mediatek.opapp" to LIVE,
            "com.xfinity.cloudtvr.tenfoot" to LIVE, "com.sling" to LIVE, "com.amazon.tv.livetv" to LIVE,
            "org.pbskids.video" to KIDS, "com.amazon.tahoe" to KIDS,
            "com.amazon.bueller.music" to MUSIC, "com.spotify.tv.android" to MUSIC, "com.pandora.android.atv" to MUSIC,
            "com.google.android.youtube.tvmusic" to MUSIC,
            "com.apple.airplay.amazon.airplay" to PHOTOS, "com.plexapp.android" to PHOTOS, "org.jellyfin.androidtv" to PHOTOS,
            "com.amazon.photos" to PHOTOS,
        )

        /** By package name first, else by the category the app declares (Android 8+), else Other. */
        fun of(packageName: String, declared: Int): AppCategory = BY_PACKAGE[packageName] ?: when (declared) {
            ApplicationInfo.CATEGORY_VIDEO -> STREAMING
            ApplicationInfo.CATEGORY_AUDIO -> MUSIC
            ApplicationInfo.CATEGORY_IMAGE -> PHOTOS
            ApplicationInfo.CATEGORY_GAME -> GAMES
            ApplicationInfo.CATEGORY_NEWS -> NEWS
            else -> OTHER
        }
    }
}
