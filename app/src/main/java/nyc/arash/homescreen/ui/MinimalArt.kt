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
 * separated that way (contrast under MIN_CONTRAST) gets the badge treatment instead (see badge()); if that fails too,
 * null, and the tile keeps the original colours. Runs off the main thread; about 130k pixels per banner.
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
        val out = if (top >= MIN_CONTRAST) {
            // Strict enough that soft glows in the artwork (e.g. a gradient's bright corner) drop out.
            val lo = 0.70f * top
            val hi = 0.90f * top
            IntArray(px.size) { i ->
                val t = ((stand[i] - lo) / (hi - lo)).coerceIn(0f, 1f)
                val alpha = (t * t * (3 - 2 * t) * 255).toInt()
                if (alpha == 0) 0 else (alpha shl 24) or 0x00FFFFFF
            }
        } else {
            badge(px, w, h) ?: return null
        }
        return crop(out, w, h)
    }.getOrNull()

    /**
     * A logo inside a solid badge on a busy ground (e.g. letters on a coloured disc over rays): the badge's edge as a
     * white ring and everything inside it (the letters) in white. The badge is the largest connected area of the most
     * common colour in the middle of the artwork; it must not touch the edge and must cover 5-60% of the artwork.
     */
    private fun badge(px: IntArray, w: Int, h: Int): IntArray? {
        val x0 = (w * 0.35f).toInt(); val x1 = (w * 0.65f).toInt()
        val y0 = (h * 0.25f).toInt(); val y1 = (h * 0.75f).toInt()
        val counts = HashMap<Int, Int>()
        for (y in y0 until y1) for (x in x0 until x1) {
            val c = px[y * w + x]
            if (Color.alpha(c) < 128) continue
            val key = ((Color.red(c) shr 4) shl 8) or ((Color.green(c) shr 4) shl 4) or (Color.blue(c) shr 4)
            counts[key] = (counts[key] ?: 0) + 1
        }
        val seedKey = counts.maxByOrNull { it.value }?.key ?: return null
        var seed = 0
        loop@ for (y in y0 until y1) for (x in x0 until x1) {
            val c = px[y * w + x]
            if ((((Color.red(c) shr 4) shl 8) or ((Color.green(c) shr 4) shl 4) or (Color.blue(c) shr 4)) == seedKey) { seed = c; break@loop }
        }
        fun near(c: Int): Boolean {
            if (Color.alpha(c) < 128) return false
            val dr = Color.red(c) - Color.red(seed)
            val dg = Color.green(c) - Color.green(seed)
            val db = Color.blue(c) - Color.blue(seed)
            return (dr * dr + dg * dg + db * db) < (0.22f * 441.7f).let { (it * it).toInt() }
        }
        // The largest connected area of that colour starting in the middle (a pixel nearest the centre could sit
        // inside a letter).
        val seen = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var best: IntArray? = null
        var bestSize = 0
        for (y in y0 until y1) for (x in x0 until x1) {
            val start = y * w + x
            if (seen[start] || !near(px[start])) continue
            var head = 0; var tail = 0
            queue[tail++] = start; seen[start] = true
            while (head < tail) {
                val i = queue[head++]
                val cx = i % w; val cy = i / w
                if (cx > 0 && !seen[i - 1] && near(px[i - 1])) { seen[i - 1] = true; queue[tail++] = i - 1 }
                if (cx < w - 1 && !seen[i + 1] && near(px[i + 1])) { seen[i + 1] = true; queue[tail++] = i + 1 }
                if (cy > 0 && !seen[i - w] && near(px[i - w])) { seen[i - w] = true; queue[tail++] = i - w }
                if (cy < h - 1 && !seen[i + w] && near(px[i + w])) { seen[i + w] = true; queue[tail++] = i + w }
            }
            if (tail > bestSize) { bestSize = tail; best = queue.copyOf(tail) }
        }
        val comp = best ?: return null
        if (bestSize < w * h * 0.05f || bestSize > w * h * 0.60f) return null
        val region = BooleanArray(w * h)
        for (i in comp) {
            val cx = i % w; val cy = i / w
            if (cx == 0 || cy == 0 || cx == w - 1 || cy == h - 1) return null  // touches the edge: not a badge
            region[i] = true
        }
        // Outside: everything reachable from the edge without crossing the badge. Inside but not badge: the letters.
        val outside = BooleanArray(w * h)
        var head = 0; var tail = 0
        for (i in 0 until w * h) {
            val cx = i % w; val cy = i / w
            if ((cx == 0 || cy == 0 || cx == w - 1 || cy == h - 1) && !region[i]) { outside[i] = true; queue[tail++] = i }
        }
        while (head < tail) {
            val i = queue[head++]
            val cx = i % w; val cy = i / w
            for (n in intArrayOf(if (cx > 0) i - 1 else -1, if (cx < w - 1) i + 1 else -1, if (cy > 0) i - w else -1, if (cy < h - 1) i + w else -1)) {
                if (n >= 0 && !outside[n] && !region[n]) { outside[n] = true; queue[tail++] = n }
            }
        }
        // The ring: badge pixels within `ring` steps of the outside.
        val ring = maxOf(2, (0.07f * kotlin.math.sqrt(bestSize / Math.PI.toFloat())).toInt())
        val dist = IntArray(w * h) { if (outside[it]) 0 else Int.MAX_VALUE }
        head = 0; tail = 0
        for (i in 0 until w * h) if (outside[i]) queue[tail++] = i
        while (head < tail) {
            val i = queue[head++]
            if (dist[i] >= ring) continue
            val cx = i % w; val cy = i / w
            for (n in intArrayOf(if (cx > 0) i - 1 else -1, if (cx < w - 1) i + 1 else -1, if (cy > 0) i - w else -1, if (cy < h - 1) i + w else -1)) {
                if (n >= 0 && region[n] && dist[n] == Int.MAX_VALUE) { dist[n] = dist[i] + 1; queue[tail++] = n }
            }
        }
        return IntArray(w * h) { i ->
            val white = (region[i] && dist[i] <= ring) || (!region[i] && !outside[i])
            if (white) -1 else 0  // -1 = opaque white
        }
    }

    private fun crop(out: IntArray, w: Int, h: Int): ImageBitmap? {
        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (i in out.indices) {
            if ((out[i] ushr 24) <= 24) continue
            val x = i % w
            val y = i / w
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        if (maxX < 0) return null
        val full = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
        val pad = 2
        val x0 = (minX - pad).coerceAtLeast(0)
        val y0 = (minY - pad).coerceAtLeast(0)
        val x1 = (maxX + pad).coerceAtMost(w - 1)
        val y1 = (maxY + pad).coerceAtMost(h - 1)
        return Bitmap.createBitmap(full, x0, y0, x1 - x0 + 1, y1 - y0 + 1).asImageBitmap()
    }
}
