package nyc.arash.homescreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nyc.arash.homescreen.apps.AppRepository
import nyc.arash.homescreen.apps.LauncherLayout
import nyc.arash.homescreen.apps.TvApp
import nyc.arash.homescreen.ui.HomeScreen
import nyc.arash.homescreen.ui.NavigationSoundProvider
import nyc.arash.homescreen.ui.Wallpaper
import nyc.arash.homescreen.ui.theme.HomeScreenTheme

class MainActivity : ComponentActivity() {
    private var apps by mutableStateOf<List<TvApp>>(emptyList())
    private var favoriteIds by mutableStateOf<List<String>>(emptyList())
    private var recentIds by mutableStateOf<List<String>>(emptyList())
    private var wallpaper by mutableStateOf<ImageBitmap?>(null)
    private var wallpaperKey: String? = null
    private var appsJob: Job? = null
    private var wallpaperJob: Job? = null
    private var resumeGeneration by mutableIntStateOf(0)
    private var pendingFocusRestore = false
    private val layout by lazy { LauncherLayout(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeScreenTheme {
                NavigationSoundProvider {
                    HomeScreen(
                        apps = apps,
                        favoriteIds = favoriteIds,
                        recentIds = recentIds,
                        wallpaper = wallpaper,
                        resumeGeneration = resumeGeneration,
                        onLaunchApp = ::launch,
                        onSaveFavorites = ::saveFavorites,
                    )
                }
            }
        }
    }

    private fun launch(app: TvApp) {
        if (runCatching { startActivity(app.launchIntent) }.isSuccess) {
            layout.recordLaunch(app.id)
            recentIds = layout.recentIds()
        }
    }

    private fun saveFavorites(ids: List<String>) {
        layout.saveFavorites(ids)
        favoriteIds = layout.favoriteIds()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) restoreFocusWhenReady()
    }

    private fun restoreFocusWhenReady() {
        if (pendingFocusRestore && hasWindowFocus()) {
            pendingFocusRestore = false
            resumeGeneration++
        }
    }

    override fun onResume() {
        super.onResume()
        pendingFocusRestore = true
        restoreFocusWhenReady()
        recentIds = layout.recentIds()
        appsJob?.cancel()
        appsJob = lifecycleScope.launch {
            // favoriteIds may read the private favorites file on first run, so it is off the main thread too.
            val (loadedApps, loadedFavorites) = withContext(Dispatchers.IO) {
                AppRepository(applicationContext).listApps() to layout.favoriteIds()
            }
            favoriteIds = loadedFavorites
            apps = loadedApps
        }
        // The photo is decoded only when the chosen file changed (a Home press must not re-decode it).
        wallpaperJob?.cancel()
        wallpaperJob = lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) { Wallpaper.candidates(applicationContext) }
            val key = Wallpaper.cacheKey(files)
            if (key != wallpaperKey) {
                wallpaper = withContext(Dispatchers.IO) { Wallpaper.load(files) }
                wallpaperKey = key
            }
        }
    }
}
