package nyc.arash.homescreen.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * The home screen's photo background, from the shared folder /sdcard/Pictures/HomePoint/ (put pictures there with
 * `adb push`; reading it needs the photos permission, granted by scripts/setup.sh) or from the app's own folder
 * files/wallpapers/ where a device lets adb write there (Fire OS 8 doesn't). The first picture by file name is used,
 * so rename a file (for example to 0-...) to choose it.
 * Pictures are scaled to fit a 1080p screen, kept in RGB_565, turned upright from their EXIF orientation, and a picture
 * that can't be read is skipped. With no usable picture the home screen keeps its built-in gradient.
 */
internal object Wallpaper {
    private val EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    private const val TARGET_W = 1920
    private const val TARGET_H = 1080
    private const val MAX_PIXELS = TARGET_W * TARGET_H * 2

    /** The pictures in name order (shared folder and the app's own folder together), or empty. */
    fun candidates(context: Context): List<File> {
        @Suppress("DEPRECATION") // the plain path is what adb push writes to; MediaStore isn't needed for one folder
        val shared = runCatching { File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "HomePoint") }.getOrNull()
        val own = runCatching { context.getExternalFilesDir("wallpapers") }.getOrNull()
        return listOfNotNull(shared, own).flatMap { dir ->
            runCatching { dir.listFiles()?.toList().orEmpty() }.getOrDefault(emptyList())
        }.filter { it.isFile && !it.name.startsWith(".") && it.extension.lowercase() in EXTENSIONS && it.length() > 0 }
            .sortedBy { it.name.lowercase() }
    }

    /** Identifies the picture that would be shown, so callers can skip decoding when nothing changed. */
    fun cacheKey(files: List<File>): String = files.joinToString("|") { "${it.path}:${it.lastModified()}:${it.length()}" }

    fun load(files: List<File>): ImageBitmap? {
        for (file in files) decode(file)?.let { return it.asImageBitmap() }
        return null
    }

    private fun decode(file: File): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null
        // Largest power-of-two step that still covers the screen in both directions (the picture is cropped to fill).
        var sample = 1
        while (w / (sample * 2) >= TARGET_W && h / (sample * 2) >= TARGET_H) sample *= 2
        // Very tall or wide pictures: keep stepping down until the pixel count is reasonable.
        while ((w / sample).toLong() * (h / sample) > MAX_PIXELS) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return null
        upright(bitmap, file)
    }.getOrNull()

    private fun upright(bitmap: Bitmap, file: File): Bitmap {
        val degrees = runCatching {
            when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (degrees == 0f) return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }
}
