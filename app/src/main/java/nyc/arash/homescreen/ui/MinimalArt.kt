package nyc.arash.homescreen.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * A plain white version of an app's own logo, cut from its banner (or icon) for the minimal glass tiles.
 * The logo is whatever stands out from the artwork's border colour, lighter on a dark ground or darker on a light one;
 * the cut-off scales with the logo's own contrast, so soft glows and gradients drop out. Artwork whose logo can't be
 * separated cleanly (contrast under MIN_CONTRAST, e.g. a busy multi-colour badge) returns null, and the tile keeps the
 * original colours. Runs off the main thread; about 130k pixels per banner.
 */
internal object MinimalArt {
    private const val MIN_CONTRAST = 0.5f

    fun whiteLogo(art: Drawable, isBanner: Boolean): ImageBitmap? = runCatching {
        val w = if (isBanner) 480 else 256
        val h = if (isBanner) 270 else 256
        val bmp = art.toBitmap(w, h, Bitmap.Config.ARGB_8888)
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        fun lum(c: Int) = (0.2126f * Color.red(c) + 0.7152f * Color.green(c) + 0.0722f * Color.blue(c)) / 255f

        // The ground: a 2-pixel frame around the artwork.
        var sum = 0f
        var n = 0
        var clear = 0
        fun take(x: Int, y: Int) {
            val c = px[y * w + x]
            if (Color.alpha(c) < 32) clear++ else sum += lum(c)
            n++
        }
        for (x in 0 until w) for (y in intArrayOf(0, 1, h - 2, h - 1)) take(x, y)
        for (y in 2 until h - 2) for (x in intArrayOf(0, 1, w - 2, w - 1)) take(x, y)
        val transparentGround = clear > n / 2
        val ground = if (n > clear) sum / (n - clear) else 0f
        val lightLogo = ground < 0.55f

        // How far each pixel stands out from the ground, in the logo's direction (or its opacity on a clear ground).
        val stand = FloatArray(px.size)
        for (i in px.indices) {
            val c = px[i]
            val a = Color.alpha(c) / 255f
            stand[i] = if (transparentGround) a else ((if (lightLogo) lum(c) - ground else ground - lum(c)) * a).coerceAtLeast(0f)
        }
        // The logo's own contrast: 99.5th percentile, robust against a few stray pixels.
        val hist = IntArray(256)
        for (v in stand) hist[(v.coerceIn(0f, 1f) * 255).toInt()]++
        var seen = 0
        var top = 0f
        for (b in 0..255) {
            seen += hist[b]
            if (seen >= (stand.size * 0.995).toInt()) { top = b / 255f; break }
        }
        if (top < MIN_CONTRAST) return null
        // Strict enough that soft glows in the artwork (e.g. a gradient's bright corner) drop out.
        val lo = 0.70f * top
        val hi = 0.90f * top

        val out = IntArray(px.size)
        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (i in px.indices) {
            val t = ((stand[i] - lo) / (hi - lo)).coerceIn(0f, 1f)
            val alpha = (t * t * (3 - 2 * t) * 255).toInt()
            if (alpha == 0) continue
            out[i] = (alpha shl 24) or 0x00FFFFFF
            if (alpha > 24) {
                val x = i % w
                val y = i / w
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        if (maxX < 0) return null
        val full = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
        val pad = 2
        val x0 = (minX - pad).coerceAtLeast(0)
        val y0 = (minY - pad).coerceAtLeast(0)
        val x1 = (maxX + pad).coerceAtMost(w - 1)
        val y1 = (maxY + pad).coerceAtMost(h - 1)
        Bitmap.createBitmap(full, x0, y0, x1 - x0 + 1, y1 - y0 + 1).asImageBitmap()
    }.getOrNull()
}
