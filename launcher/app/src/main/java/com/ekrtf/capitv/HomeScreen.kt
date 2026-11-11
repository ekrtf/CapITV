package com.ekrtf.capitv

import android.graphics.BitmapFactory
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sqrt

private val BackgroundTop = Color(0xFF111319)
private val BackgroundBottom = Color(0xFF060709)
private val TileIdle = Color(0xFF16181F)
private val TileFocused = Color(0xFF1E212B)
private val TextDim = Color(0xFF8A8F9C)

@Composable
fun HomeScreen(
    config: AppConfig,
    resolver: LaunchResolver,
    resumeTick: Int,
    messages: MessageBus,
    onLaunch: (Tile) -> Unit,
    onSettings: () -> Unit,
) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BackgroundTop, BackgroundBottom)))
                .padding(horizontal = 64.dp, vertical = 40.dp),
        ) {
            Clock(modifier = Modifier.align(Alignment.TopEnd))

            val firstTile = remember { FocusRequester() }
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            ) {
                config.tiles.forEachIndexed { index, tile ->
                    // Re-evaluated on every resume (resumeTick) so newly installed apps light up.
                    val installed = remember(resumeTick, tile.id) { resolver.resolve(tile) != null }
                    AppTile(
                        tile = tile,
                        number = index + 1,
                        installed = installed,
                        focusRequester = if (index == 0) firstTile else null,
                        // Tiles share the row: a TV screen is only 960dp wide, so four full-size
                        // tiles plus gaps (1076dp) would push the last one off screen.
                        modifier = Modifier.weight(1f, fill = false),
                        onClick = { onLaunch(tile) },
                    )
                }
            }
            LaunchedEffect(Unit) { runCatching { firstTile.requestFocus() } }

            SettingsChip(onClick = onSettings, modifier = Modifier.align(Alignment.BottomStart))

            messages.current?.let { msg ->
                LaunchedEffect(msg.id) {
                    delay(4_000)
                    messages.dismiss(msg.id)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                        .background(Color(0xE6202330), RoundedCornerShape(12.dp))
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                ) {
                    Text(text = msg.text, color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}

/** A logo card with its remote key number underneath, lit when the card has focus. */
@Composable
private fun AppTile(
    tile: Tile,
    number: Int,
    installed: Boolean,
    focusRequester: FocusRequester?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = parseAccent(tile.accent)
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        modifier = modifier.widthIn(max = 248.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            shape = ClickableSurfaceDefaults.shape(shape = shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = TileIdle,
                focusedContainerColor = TileFocused,
                pressedContainerColor = TileFocused,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(border = BorderStroke(2.dp, accent), shape = shape),
            ),
            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = Glow(elevationColor = accent.copy(alpha = 0.5f), elevation = 18.dp),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
            interactionSource = interaction,
        ) {
            TileFace(tile = tile, dimmed = !installed)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (installed) "$number" else "$number · ${stringResource(R.string.tile_not_installed)}",
            color = if (focused) Color.White else TextDim,
            fontSize = 15.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun TileFace(tile: Tile, dimmed: Boolean) {
    val logo = rememberLogo(tile.logo)
    val alpha = if (dimmed) 0.3f else 1f
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (logo != null) {
            // Every logo gets the same area, so a square mark and a long wordmark look equally big.
            val aspect = logo.width.toFloat() / logo.height
            val width = minOf(maxHeight * 0.56f * sqrt(aspect), maxWidth * 0.74f)
            Image(
                bitmap = logo,
                contentDescription = tile.label,
                alpha = alpha,
                modifier = Modifier
                    .width(width)
                    .aspectRatio(aspect),
            )
        } else {
            Text(
                text = tile.label,
                color = Color.White.copy(alpha = alpha),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/** Decodes a bundled logo once; null (so the label is shown) if it is missing or unreadable. */
@Composable
private fun rememberLogo(path: String?): ImageBitmap? {
    val assets = LocalContext.current.assets
    return remember(path) {
        path?.let { runCatching { assets.open(it).use(BitmapFactory::decodeStream)?.asImageBitmap() }.getOrNull() }
    }
}

@Composable
private fun SettingsChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = TileFocused,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.dp, Color(0xFF2A2D38)), shape = shape),
            focusedBorder = Border(border = BorderStroke(2.dp, Color.White), shape = shape),
        ),
    ) {
        Text(
            text = stringResource(R.string.settings),
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    // The box's own 12/24-hour setting, not just the locale's default.
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            // Wake up just after the next minute boundary.
            delay((60 - now.second) * 1_000L - now.nano / 1_000_000 + 50)
        }
    }
    val time = remember(now, locale, is24Hour) {
        val skeleton = if (is24Hour) "Hm" else "hm"
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(now)
    }
    val date = remember(now.toLocalDate(), locale) {
        DateTimeFormatter.ofPattern("EEEE d MMMM", locale).format(now)
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(text = time, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light)
        Text(text = date, color = TextDim, fontSize = 16.sp)
    }
}

private fun parseAccent(hex: String): Color =
    Color(0xFF000000 or hex.removePrefix("#").toLong(16))
