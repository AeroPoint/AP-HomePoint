package nyc.arash.homescreen.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import nyc.arash.homescreen.apps.AppCategory
import nyc.arash.homescreen.apps.LauncherLayout
import nyc.arash.homescreen.apps.TvApp
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first

private const val HOME_COLUMNS = 3
private const val ALL_COLUMNS = 6

/**
 * HomePoint. Home: the favorites as big frosted-glass tiles, three across. All Apps: every app in groups (Recently
 * used first, then Streaming, Live TV, Kids, ...), each under a heading with a thin white line.
 * Episode-level "Continue watching" stays on Amazon's home (hold Home): Fire OS lets only system apps read it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    apps: List<TvApp>,
    favoriteIds: List<String>,
    recentIds: List<String>,
    wallpaper: ImageBitmap?,
    logos: Map<String, ImageBitmap?>,
    resumeGeneration: Int = 0,
    onLaunchApp: (TvApp) -> Unit,
    onSaveFavorites: (List<String>) -> Unit,
) {
    val inputMode = LocalInputModeManager.current
    var allApps by remember(resumeGeneration) { mutableStateOf(false) }
    var arranging by remember(resumeGeneration) { mutableStateOf<TvApp?>(null) }
    val firstTile = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    var focusRequest by remember { mutableIntStateOf(0) }
    val byId = apps.associateBy { it.id }
    val favorites = favoriteIds.mapNotNull { byId[it] }
    val recent = recentIds.filter { it !in favoriteIds }.mapNotNull { byId[it] }.take(ALL_COLUMNS)
    val groups = AppCategory.entries.map { cat -> cat to apps.filter { it.category == cat }.sortedBy { it.name.lowercase(Locale.ROOT) } }
        .filter { it.second.isNotEmpty() }
    val now = currentTime()
    val (time, period) = formatHM(now, hour12 = true)

    LaunchedEffect(apps.isNotEmpty(), allApps, focusRequest, resumeGeneration) {
        if (apps.isNotEmpty()) {
            gridState.scrollToItem(0)
            // Lazy grids dispose off-screen tiles. Wait for the first tile to be placed.
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.any { it.index == 1 } }.first { it }
            withFrameNanos { }
            // Fire OS may retain touch mode after settings/helper setup. TV tiles need keyboard focus.
            inputMode.requestInputMode(InputMode.Keyboard)
            firstTile.requestFocus()
        }
    }
    BackHandler {
        if (arranging != null) arranging = null
        else if (allApps) allApps = false
        else if (apps.isNotEmpty()) focusRequest++
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF070D19))) {
        if (wallpaper != null) {
            Image(wallpaper, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            // Dim the photo so the glass tiles and text stay readable: lighter at the top, darker behind the tiles.
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x4D040912), Color(0x80040912), Color(0xB3040912)))))
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(listOf(Color(0xFF203D64), Color.Transparent), Offset(size.width * .25f, 0f), size.width * .82f))
                drawRect(Brush.radialGradient(listOf(Color(0xFF251D43), Color.Transparent), Offset(size.width * .94f, size.height * .85f), size.width * .8f))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99040912))))
            }
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 52.dp).padding(top = 25.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    NavChip("Home", selected = !allApps, onClick = { allApps = false })
                    NavChip("All Apps", selected = allApps, onClick = { allApps = true })
                }
                Text("$time $period", color = Color(0xFFE9EFFA), fontSize = 16.sp, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.height(if (allApps) 18.dp else 30.dp))
            Text(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())).uppercase(Locale.getDefault()), color = Color(0xFFB4C2DA), fontSize = 10.sp, letterSpacing = 1.7.sp)
            Spacer(Modifier.height(4.dp))
            Text(if (allApps) "All your apps." else greetingFor(now) + ".", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light, letterSpacing = (-.6).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(if (allApps) 10.dp else 30.dp))
            if (!allApps) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(HOME_COLUMNS),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    itemsIndexed(favorites, key = { _, app -> "favorite:${app.id}" }) { index, app ->
                        AppTile(app, logos[app.id], { onLaunchApp(app) }, { arranging = app },
                            if (index == 0) Modifier.focusRequester(firstTile) else Modifier, big = true)
                    }
                    if (apps.isEmpty()) {
                        items(HOME_COLUMNS * 2) {
                            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(26.dp)).background(Color(0x14FFFFFF)))
                        }
                    }
                    if (apps.isNotEmpty() && favorites.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            NavChip("Choose favorites in All Apps", onClick = { allApps = true }, modifier = Modifier.focusRequester(firstTile))
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(ALL_COLUMNS),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    fun section(title: String, list: List<TvApp>, keyPrefix: String, firstSection: Boolean) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "head:$keyPrefix") { GroupHeading(title, list.size) }
                        itemsIndexed(list, key = { _, app -> "$keyPrefix:${app.id}" }) { index, app ->
                            AppTile(app, logos[app.id], { onLaunchApp(app) }, { arranging = app },
                                if (firstSection && index == 0) Modifier.focusRequester(firstTile) else Modifier)
                        }
                    }
                    if (recent.isNotEmpty()) section("Recently used", recent, "recent", firstSection = true)
                    groups.forEachIndexed { i, (cat, list) -> section(cat.title, list, cat.name, firstSection = recent.isEmpty() && i == 0) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Hold Home for Amazon's home and Continue watching", color = Color(0xFF8C9DB8), fontSize = 10.sp)
                Text("← ↑ ↓ →  Navigate     OK  Open     ☰  Arrange", color = Color(0xFF8C9DB8), fontSize = 10.sp)
            }
        }
        arranging?.let { app ->
            ArrangeDialog(app, favoriteIds, shownIds = favorites.map { it.id }, onSave = onSaveFavorites, onDismiss = { arranging = null })
        }
    }
}

/** A group's title and count over a thin white line. */
@Composable
private fun GroupHeading(title: String, count: Int) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(title, color = Color(0xFFF2F5FB), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text("$count", color = Color(0xFF9AAAC4), fontSize = 11.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x66FFFFFF)))
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
internal fun NavChip(label: String, selected: Boolean = false, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, compact: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(if (compact) 10.dp else 14.dp)
    Box(
        modifier
            .navigationSound()
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(if (focused) Color(0xFFF0F7FC) else if (selected) Color(0x30FFFFFF) else if (compact) Color(0x10FFFFFF) else Color(0x12FFFFFF))
            .border(1.dp, if (focused) Color.White else Color.Transparent, shape)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = if (compact) 6.dp else 19.dp, vertical = if (compact) 6.dp else 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (!enabled) Color(0xFF617089) else if (focused) Color(0xFF111C2F) else Color(0xFFD7E0EF), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ArrangeDialog(app: TvApp, favorites: List<String>, shownIds: List<String>, onSave: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val firstAction = remember { FocusRequester() }
    // Count and move among the favorites on screen; favorites whose app isn't installed keep their places.
    val index = shownIds.indexOf(app.id)
    val shown = shownIds.toSet()
    LaunchedEffect(app.id) { firstAction.requestFocus() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.width(370.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF111D32))
                .border(.5.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(app.name, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Medium)
            Text(if (index >= 0) "Favorite ${index + 1} of ${shownIds.size}" else "Add this app to your Home screen.", color = Color(0xFFA6B6D0), fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            if (index >= 0) {
                NavChip("Move earlier", onClick = { onSave(LauncherLayout.moveAmongShown(favorites, shown, app.id, -1)) }, modifier = Modifier.fillMaxWidth().focusRequester(firstAction))
                NavChip("Move later", onClick = { onSave(LauncherLayout.moveAmongShown(favorites, shown, app.id, +1)) }, modifier = Modifier.fillMaxWidth())
                NavChip("Remove from favorites", onClick = { onSave(favorites.filterNot { it == app.id }); onDismiss() }, modifier = Modifier.fillMaxWidth())
            } else {
                NavChip("Add to favorites", onClick = { onSave(favorites + app.id); onDismiss() }, modifier = Modifier.fillMaxWidth().focusRequester(firstAction))
            }
            NavChip("Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
