package nyc.arash.homescreen.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLayoutTest {
    @Test fun favoritesFileIgnoresBlanksCommentsAndRepeats() {
        assertEquals(listOf("com.example.tv", "org.example.kids", "homepoint.livetv"),
            LauncherLayout.parseFavoritesFile("# my apps\ncom.example.tv\n\n  org.example.kids  # kids\ncom.example.tv\nhomepoint.livetv\n"))
        assertEquals(emptyList<String>(), LauncherLayout.parseFavoritesFile(""))
    }

    @Test fun recentAppsAreNewestFirstWithoutDuplicates() {
        assertEquals(listOf("b"), LauncherLayout.pushRecent(emptyList(), "b"))
        assertEquals(listOf("a", "c", "b"), LauncherLayout.pushRecent(listOf("c", "a", "b"), "a"))
        val full = (1..LauncherLayout.MAX_RECENT).map { "app$it" }
        val next = LauncherLayout.pushRecent(full, "new")
        assertEquals(LauncherLayout.MAX_RECENT, next.size)
        assertEquals("new", next.first())
        assertFalse(next.contains("app${LauncherLayout.MAX_RECENT}"))
    }

    @Test fun normalizationPreservesOrderAndUnavailablePackages() {
        assertEquals(listOf("plex", "uninstalled.app", "spotify"),
            LauncherLayout.normalizeFavorites(listOf(" plex ", "", "uninstalled.app", "plex", "spotify", "  ")))
    }

    @Test fun moveRetainsOtherAppsInRelativeOrder() {
        assertEquals(listOf("spotify", "youtube", "plex", "mlb"),
            LauncherLayout.moveFavorite(listOf("plex", "spotify", "youtube", "mlb"), 0, 2))
        assertEquals(listOf("mlb", "plex", "spotify", "youtube"),
            LauncherLayout.moveFavorite(listOf("plex", "spotify", "youtube", "mlb"), 3, 0))
    }

    @Test fun invalidMovesLeaveOrderIntact() {
        val ids = listOf("plex", "spotify")
        assertEquals(ids, LauncherLayout.moveFavorite(ids, -1, 0))
        assertEquals(ids, LauncherLayout.moveFavorite(ids, 0, 2))
        assertEquals(ids, LauncherLayout.moveFavorite(ids, 1, 1))
        assertEquals(emptyList<String>(), LauncherLayout.moveFavorite(emptyList(), 0, 0))
    }

    @Test fun movesSkipFavoritesThatAreNotShown() {
        val ids = listOf("a", "hidden1", "b", "hidden2", "c")
        val shown = setOf("a", "b", "c")
        // b moves past the hidden entry to swap with a; hidden entries stay where they were.
        assertEquals(listOf("b", "hidden1", "a", "hidden2", "c"), LauncherLayout.moveAmongShown(ids, shown, "b", -1))
        assertEquals(listOf("a", "hidden1", "c", "hidden2", "b"), LauncherLayout.moveAmongShown(ids, shown, "b", +1))
        // Already first or last among the shown: nothing changes.
        assertEquals(ids, LauncherLayout.moveAmongShown(ids, shown, "a", -1))
        assertEquals(ids, LauncherLayout.moveAmongShown(ids, shown, "c", +1))
        // Unknown or hidden ids and bad steps are ignored.
        assertEquals(ids, LauncherLayout.moveAmongShown(ids, shown, "hidden1", +1))
        assertEquals(ids, LauncherLayout.moveAmongShown(ids, shown, "zzz", +1))
        assertEquals(ids, LauncherLayout.moveAmongShown(ids, shown, "b", 2))
    }

    @Test fun helpersAndVpnsAreUtilitiesButStreamingAppsAreNot() {
        listOf("tv.dumbtv.home", "io.github.toolicious.homeonfire", "com.aeropoint.homepoint.debug",
            "com.nordvpn.android", "io.privado.android", "com.windscribe.vpn", "com.esaba.downloader",
            "com.amazon.whasettings", "com.amazon.tv.ambient").forEach { assertTrue(it, AppRepository.isUtility(it)) }
        listOf("com.netflix.ninja", "com.amazon.firebat", "com.disney.disneyplus", "com.amazon.firetv.youtube",
            "com.cbs.ott", "org.pbskids.video", AppRepository.LIVE_TV_ID, "tv.twitch.android.app", "com.amazon.avod")
            .forEach { assertFalse(it, AppRepository.isUtility(it)) }
    }
}
