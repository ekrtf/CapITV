package com.ekrtf.capitv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val Background = Color(0xFF0B0C10)
private val TileIdle = Color(0xFF16181F)
private val TileFocused = Color(0xFF1F2230)
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
                .background(Background)
                .padding(horizontal = 64.dp, vertical = 40.dp),
        ) {
            Clock(modifier = Modifier.align(Alignment.TopEnd))
            Text(
                text = stringResource(R.string.app_name),
                color = TextDim,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.TopStart),
            )

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
                        modifier = if (index == 0) Modifier.focusRequester(firstTile) else Modifier,
                        onClick = { onLaunch(tile) },
                    )
                }
            }
            LaunchedEffect(Unit) { runCatching { firstTile.requestFocus() } }

            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsChip(onClick = onSettings)
                Spacer(Modifier.width(24.dp))
                Text(
                    text = stringResource(R.string.shortcut_hint, config.tiles.size),
                    color = TextDim,
                    fontSize = 14.sp,
                )
            }

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

@Composable
private fun AppTile(
    tile: Tile,
    number: Int,
    installed: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = parseAccent(tile.accent)
    val shape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.size(width = 248.dp, height = 156.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TileIdle,
            focusedContainerColor = TileFocused,
            pressedContainerColor = TileFocused,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(border = BorderStroke(3.dp, accent), shape = shape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(accent),
            )
            Text(
                text = "$number",
                color = TextDim,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 18.dp),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
            ) {
                Text(
                    text = tile.label,
                    color = if (installed) Color.White else TextDim,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!installed) {
                    Text(
                        text = stringResource(R.string.tile_not_installed),
                        color = TextDim,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsChip(onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
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
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            // Wake up just after the next minute boundary.
            delay((60 - now.second) * 1_000L - now.nano / 1_000_000 + 50)
        }
    }
    val time = remember(now, locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(now)
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
