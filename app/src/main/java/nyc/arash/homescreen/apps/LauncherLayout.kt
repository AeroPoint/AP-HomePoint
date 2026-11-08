package nyc.arash.homescreen.apps

import android.content.Context
import java.io.File

/**
 * Device-local favorites and recently used apps. Missing packages keep their place if installed again later.
 *
 * First run: favorites come from an optional private list, files/favorites.txt in the app's own external folder
 * (/sdcard/Android/data/<app id>/files/favorites.txt, one package name per line, put there with `adb push`), so a
 * household's own picks never have to live in the public code. Without that file the row starts empty and the home
 * screen offers to choose favorites in All Apps. Once arranged on the TV, the saved order wins.
 */
class LauncherLayout(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("launcher_layout", Context.MODE_PRIVATE)

    fun favoriteIds(): List<String> {
        if (preferences.contains(KEY_FAVORITES)) {
            return normalizeFavorites(preferences.getString(KEY_FAVORITES, "").orEmpty().split('\n'))
        }
        return privateDefaults()
    }

    fun saveFavorites(ids: List<String>) {
        preferences.edit().putString(KEY_FAVORITES, normalizeFavorites(ids).joinToString("\n")).apply()
    }

    fun recentIds(): List<String> = normalizeFavorites(preferences.getString(KEY_RECENT, "").orEmpty().split('\n'))

    fun recordLaunch(id: String) {
        preferences.edit().putString(KEY_RECENT, pushRecent(recentIds(), id).joinToString("\n")).apply()
    }

    private fun privateDefaults(): List<String> = runCatching {
        val file = File(appContext.getExternalFilesDir(null) ?: return emptyList(), FAVORITES_FILE)
        if (file.isFile && file.length() < 64 * 1024) parseFavoritesFile(file.readText()) else emptyList()
    }.getOrDefault(emptyList())

    companion object {
        private const val KEY_FAVORITES = "favorite_packages_v1"
        private const val KEY_RECENT = "recent_packages_v1"
        const val FAVORITES_FILE = "favorites.txt"
        const val MAX_RECENT = 12

        /** favorites.txt: one package name per line; blank lines and lines starting with # are ignored. */
        fun parseFavoritesFile(text: String): List<String> =
            normalizeFavorites(text.lines().map { it.substringBefore('#') })

        /** Keep the first occurrence; never discard an ID merely because it is not installed. */
        fun normalizeFavorites(ids: List<String>): List<String> = ids
            .map(String::trim)
            .filter { it.isNotEmpty() }
            .distinct()

        /** Move an item without changing any other item's relative order. Invalid moves are ignored. */
        fun moveFavorite(ids: List<String>, fromIndex: Int, toIndex: Int): List<String> {
            val ordered = normalizeFavorites(ids)
            if (fromIndex !in ordered.indices || toIndex !in ordered.indices || fromIndex == toIndex) {
                return ordered
            }
            return ordered.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        }

        /**
         * Move [id] one step earlier (delta -1) or later (+1) among the favorites that are shown, swapping it with the
         * next shown neighbor. Hidden IDs (apps not installed right now) keep their places.
         */
        fun moveAmongShown(ids: List<String>, shown: Set<String>, id: String, delta: Int): List<String> {
            val ordered = normalizeFavorites(ids).toMutableList()
            val from = ordered.indexOf(id)
            if (from < 0 || id !in shown || (delta != 1 && delta != -1)) return ordered
            var to = from + delta
            while (to in ordered.indices && ordered[to] !in shown) to += delta
            if (to !in ordered.indices) return ordered
            ordered[from] = ordered[to]
            ordered[to] = id
            return ordered
        }

        /** Most recent first, no duplicates, at most [MAX_RECENT]. */
        fun pushRecent(ids: List<String>, id: String): List<String> =
            (listOf(id) + normalizeFavorites(ids).filterNot { it == id }).take(MAX_RECENT)
    }
}
