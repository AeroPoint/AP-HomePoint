package nyc.arash.homescreen.ui

import android.graphics.drawable.Drawable
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.tv.material3.Text
import nyc.arash.homescreen.apps.TvApp

/** App artwork as a bitmap, drawn locally (no image-loading library). */
private fun Drawable.tileBitmap(defaultWidth: Int, defaultHeight: Int): ImageBitmap? = runCatching {
    if (intrinsicWidth <= 0 || intrinsicHeight <= 0) return@runCatching toBitmap(defaultWidth, defaultHeight).asImageBitmap()
    // One scale factor for both sides, so oversized art shrinks without being squashed.
    val scale = minOf(1f, 640f / intrinsicWidth, 360f / intrinsicHeight)
    toBitmap((intrinsicWidth * scale).toInt().coerceAtLeast(1), (intrinsicHeight * scale).toInt().coerceAtLeast(1)).asImageBitmap()
}.getOrNull()

/**
 * A frosted-glass tile. With a white logo (cut from the app's own art by MinimalArt) it shows that, centered; otherwise
 * the app's original artwork, inset with rounded corners. Focus brightens the glass and draws a white border; it never
 * changes the tile's size or position. `big` is the Home screen's 3-across size.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: TvApp,
    logo: ImageBitmap?,
    onClick: () -> Unit,
    onArrange: () -> Unit = {},
    modifier: Modifier = Modifier,
    big: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val radius = if (big) 26.dp else 16.dp
    val shape = RoundedCornerShape(radius)
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .navigationSound()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent {
                if (it.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_MENU) {
                    if (it.type == KeyEventType.KeyDown && it.nativeKeyEvent.repeatCount == 0) onArrange()
                    true
                } else false
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onArrange,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shadow(if (focused) 18.dp else 0.dp, shape, ambientColor = Color(0x664E86D8), spotColor = Color(0x664E86D8))
                .clip(shape)
                .background(if (focused) Color(0x52FFFFFF) else Color(0x24FFFFFF))
                .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0x38FFFFFF), shape),
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) {
                Image(
                    bitmap = logo,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(if (big) 0.62f else 0.66f).fillMaxHeight(if (big) 0.42f else 0.46f),
                )
            } else {
                val artwork = remember(app.id) {
                    app.banner?.tileBitmap(320, 180) ?: app.icon.tileBitmap(192, 192)
                }
                if (artwork != null) {
                    Image(
                        bitmap = artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(if (big) 22.dp else 10.dp).clip(RoundedCornerShape(radius * 0.6f)),
                    )
                }
            }
        }
        Spacer(Modifier.height(if (big) 12.dp else 8.dp))
        Text(
            app.name,
            color = if (focused) Color.White else Color(0xFFC9D2E3),
            fontSize = if (big) 15.sp else 11.sp,
            fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
