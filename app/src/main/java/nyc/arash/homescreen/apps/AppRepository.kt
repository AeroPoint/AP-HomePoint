package nyc.arash.homescreen.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Build
import androidx.core.content.ContextCompat
import nyc.arash.homescreen.R
import java.util.Locale

data class TvApp(
    val id: String,           // package name
    val name: String,         // display label
    val banner: Drawable?,    // wide banner (preferred for TV)
    val icon: Drawable,       // fallback square icon
    val launchIntent: Intent,
    val isUtility: Boolean = AppRepository.isUtility(id),
    val category: AppCategory = AppCategory.OTHER,
) {
    val hasBanner: Boolean get() = banner != null
}

class AppRepository(private val context: Context) {

    /** Enumerate installed leanback (TV) apps, plus phone apps as a fallback. */
    fun listApps(): List<TvApp> {
        val pm = context.packageManager
        val seen = mutableSetOf<String>()
        val result = mutableListOf<TvApp>()

        fun addFromIntent(category: String) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            @Suppress("DEPRECATION")
            val infos = pm.queryIntentActivities(intent, 0)
            for (info in infos) {
                val pkg = info.activityInfo.packageName ?: continue
                if (pkg == context.packageName) continue
                if (!seen.add(pkg)) continue
                val launch = pm.getLeanbackLaunchIntentForPackage(pkg)
                    ?: pm.getLaunchIntentForPackage(pkg)
                    ?: continue
                val appInfo = info.activityInfo.applicationInfo
                val label = info.loadLabel(pm)?.toString()
                    ?: appInfo.loadLabel(pm).toString()
                val banner = info.activityInfo.loadBanner(pm) ?: appInfo.loadBanner(pm)
                val icon = appInfo.loadIcon(pm) ?: appInfo.loadLogo(pm) ?: continue
                val declared = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appInfo.category else -1
                result += TvApp(
                    id = pkg,
                    name = label,
                    banner = banner,
                    icon = icon,
                    launchIntent = launch.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                    category = AppCategory.of(pkg, declared),
                )
            }
        }

        addFromIntent(Intent.CATEGORY_LEANBACK_LAUNCHER)
        addFromIntent(Intent.CATEGORY_LAUNCHER) // pick up phone apps that work on Fire TV (e.g. Plexamp)
        liveTv()?.let { result += it }

        // Stable, slightly opinionated sort: banner apps first (look better as tiles), then alpha.
        return result.sortedWith(compareByDescending<TvApp> { it.hasBanner }.thenBy { it.name.lowercase(Locale.ROOT) })
    }

    /**
     * The antenna: the TV's own Live TV player on its built-in tuner. Fire OS shows no app icon for it, so this is a
     * tile of our own. On Fire TV smart TVs, ACTION_VIEW on the hardware tuner's channel list opens Amazon's Live TV
     * player (com.amazon.tv.livetv). TVs without a hardware tuner get no tile.
     */
    private fun liveTv(): TvApp? {
        val tvInputs = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager ?: return null
        val inputs = runCatching { tvInputs.tvInputList }.getOrDefault(emptyList())
        // Hardware tuners carry a "/HW" suffix; streaming apps also register TYPE_TUNER inputs, so skip those.
        val tuner = inputs
            .filter { it.type == TvInputInfo.TYPE_TUNER && !it.isPassthroughInput && it.id.contains("/HW") }
            .minByOrNull { it.id }
            ?: return null
        val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelsUriForInput(tuner.id))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val player = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        intent.setPackage(player.activityInfo.packageName)
        val banner = ContextCompat.getDrawable(context, R.drawable.livetv_banner) ?: return null
        return TvApp(id = LIVE_TV_ID, name = "Live TV", banner = banner, icon = banner, launchIntent = intent, category = AppCategory.LIVE)
    }

    companion object {
        /** Our own tile for the antenna (not a package). */
        const val LIVE_TV_ID = "homepoint.livetv"

        /** Utilities remain available under All Apps instead of taking streaming shelf positions. */
        fun isUtility(packageName: String): Boolean {
            val id = packageName.lowercase(Locale.ROOT)
            return id in UTILITY_PACKAGES || UTILITY_PREFIXES.any(id::startsWith) ||
                (id.startsWith("com.amazon.") && AMAZON_UTILITY_NAMES.any(id::contains))
        }

        private val UTILITY_PACKAGES = setOf(
            "tv.dumbtv.home",
            "io.github.toolicious.homeonfire",
            "nyc.arash.homescreen",
            "nyc.arash.homescreen.debug",
            "com.aeropoint.homepoint",
            "com.aeropoint.homepoint.debug",
            "io.privado.android",
            "com.windscribe.vpn",
            "com.amazon.smarthomemapviewapp",
            "com.amazon.shoptv.firetv.client",
            "com.amazon.whasettings",
            "com.amazon.tv.launcher",
            "com.amazon.tv.settings",
            "com.amazon.tv.nimh",
            "com.amazon.venezia",
            "com.esaba.downloader",
            "com.sween.laancher",
            "com.markus.firetools",
            "com.toolicious.homeonfire",
            "com.toolicious.home.on.fire",
            "com.humblemuzzu.dumbtv",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.android.settings",
        )
        private val AMAZON_UTILITY_NAMES = listOf(
            "minitv", "gamehub", "spiderpork", "earlyaccess", "troubleshoot", "profilepicker", "screensaver", "ambient",
        )
        private val UTILITY_PREFIXES = listOf(
            "com.wolf.",
            "com.wolfapps.",
            "com.hal9000.",
            "com.toolicious.",
            "com.humblemuzzu.",
            "com.nordvpn.",
            "com.expressvpn.",
            "com.surfshark.",
            "com.privateinternetaccess.",
            "de.blinkt.openvpn",
            "net.openvpn.",
            "com.wireguard.",
        )
    }
}
