package it.trotta.ticketonbus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brand = Color(0xFF0B4F8A)
private val Accent = Color(0xFFF2A33C)
private val Positive = Color(0xFF1B7F3B)
private val Negative = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4F7),
    onPrimaryContainer = Color(0xFF071E33),
    secondary = Accent,
    onSecondary = Color(0xFF3A2400),
    tertiary = Positive,
    onTertiary = Color.White,
    error = Negative,
    onError = Color.White,
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF11151A),
    surface = Color.White,
    onSurface = Color(0xFF11151A),
    surfaceVariant = Color(0xFFE6ECF3),
    onSurfaceVariant = Color(0xFF44515E),
    outline = Color(0xFF97A3B0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC9F5),
    onPrimary = Color(0xFF00325A),
    primaryContainer = Color(0xFF11497A),
    onPrimaryContainer = Color(0xFFD3E4F7),
    secondary = Accent,
    onSecondary = Color(0xFF3A2400),
    tertiary = Color(0xFF7FD79B),
    onTertiary = Color(0xFF00391A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF0E1218),
    onBackground = Color(0xFFE2E7EE),
    surface = Color(0xFF161C24),
    onSurface = Color(0xFFE2E7EE),
    surfaceVariant = Color(0xFF28313C),
    onSurfaceVariant = Color(0xFFBFCAD6),
    outline = Color(0xFF6B7783),
)

@Composable
fun TicketOnBusTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
