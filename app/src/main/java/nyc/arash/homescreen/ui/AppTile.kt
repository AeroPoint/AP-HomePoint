package nyc.arash.homescreen.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.core.graphics.drawable.toBitmap
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.drawable.Drawable
import androidx.tv.material3.Text
import nyc.arash.homescreen.apps.TvApp

/** App artwork as a bitmap, drawn locally (no image-loading library). */
private fun Drawable.tileBitmap(defaultWidth: Int, defaultHeight: Int): ImageBitmap? = runCatching {
    if (intrinsicWidth <= 0 || intrinsicHeight <= 0) return@runCatching toBitmap(defaultWidth, defaultHeight).asImageBitmap()
    // One scale factor for both sides, so oversized art shrinks without being squashed.
    val scale = minOf(1f, 640f / intrinsicWidth, 360f / intrinsicHeight)
    toBitmap((intrinsicWidth * scale).toInt().coerceAtLeast(1), (intrinsicHeight * scale).toInt().coerceAtLeast(1)).asImageBitmap()
}.getOrNull()

/** A single focus target. Focus changes the border, never the tile's size or position. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: TvApp,
    onClick: () -> Unit,
    onArrange: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    val artworkSurface = remember(app.id) {
        runCatching {
            val pixel = app.banner?.toBitmap(8, 8)?.getPixel(0, 0)
            if (pixel != null && android.graphics.Color.alpha(pixel) > 220) Color(pixel) else Color(0xFF151D2B)
        }.getOrDefault(Color(0xFF151D2B))
    }
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
                .aspectRatio(1.5f)
                .shadow(if (focused) 14.dp else 4.dp, shape, ambientColor = Color(0x554E86D8), spotColor = Color(0x554E86D8))
                .clip(shape)
                .background(artworkSurface)
                .border(if (focused) 2.dp else 0.5.dp, if (focused) Color.White else Color(0x24FFFFFF), shape),
            contentAlignment = Alignment.Center,
        ) {
            val artwork = remember(app.id) {
                app.banner?.tileBitmap(320, 180) ?: app.icon.tileBitmap(192, 192)
            }
            if (artwork != null) {
                Image(
                    bitmap = artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = if (app.hasBanner) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(15.dp),
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x10FFFFFF), Color.Transparent, Color(0x19000000)))))
            if (focused) Box(Modifier.fillMaxSize().border(2.dp, Color.White, shape))
        }
        Spacer(Modifier.height(9.dp))
        Text(
            app.name,
            color = if (focused) Color.White else Color(0xFFBDC7D9),
            fontSize = 12.sp,
            fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
