package cu.stockcuba.app.presentation.theme

import android.app.Activity
import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import cu.stockcuba.app.domain.model.ThemeMode

/**
 * Tema Material 3 completo para StockCuba.
 *
 * DISEÑO: Modo oscuro principal (inmersivo, reduce fatiga visual en largas jornadas).
 * Colores, tipografía y formas derivados directamente de DESIGN.md (Stitch).
 */

// ===== COLOR SCHEMES MATERIAL 3 =====

private val DarkColorScheme = darkColorScheme(
    // Primary - Teal (Éxito, Crecimiento, Acciones principales)
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF001E1C),
    primaryContainer = Color(0xFF0F766E),
    onPrimaryContainer = Color(0xFFD1FAF5),

    // Secondary - Coral (Alerta, Advertencia, Acciones destructivas)
    secondary = Color(0xFFFB7185),
    onSecondary = Color(0xFF4D0019),
    secondaryContainer = Color(0xFF9F1239),
    onSecondaryContainer = Color(0xFFFCE7EC),

    // Tertiary - Indigo (Acentos de marca, navegación)
    tertiary = Color(0xFF6366F1),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF312E81),
    onTertiaryContainer = Color(0xFFE0E7FF),

    // Error semántico
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEF2F2),

    // Surface - Fondos oscuros inmersivos
    surface = Color(0xFF051424),        // Canvas principal
    onSurface = Color(0xFFF8FAFC),      // Texto primario near-white
    surfaceDim = Color(0xFF010F1F),
    surfaceBright = Color(0xFF1E293B),  // Superficie L1 (Cards)
    surfaceContainerLowest = Color(0xFF0F172A), // Fondo principal brand
    surfaceContainerLow = Color(0xFF0D1C2D),
    surfaceContainer = Color(0xFF1E293B),       // Superficie L1
    surfaceContainerHigh = Color(0xFF1C2B3C),
    surfaceContainerHighest = Color(0xFF334155), // Superficie L2 (Inputs, hover)

    // Outline
    outline = Color(0xFF334155),        // Borde sutil / divisores
    outlineVariant = Color(0xFF475569),

    // Inverse
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF051424),
    inversePrimary = Color(0xFF14B8A6),

    // Background (legacy, usar surface)
    background = Color(0xFF051424),
    onBackground = Color(0xFFF8FAFC),

    // Surface variant
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8), // Texto secundario

    // Scrim
    scrim = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF00201C),

    secondary = Color(0xFF9F1239),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFCE7EC),
    onSecondaryContainer = Color(0xFF4D0019),

    tertiary = Color(0xFF312E81),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF1E1B4B),

    error = Color(0xFFB91C1C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFF7F1D1D),

    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceDim = Color(0xFFF1F5F9),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5F9),
    surfaceContainer = Color(0xFFE2E8F0),
    surfaceContainerHigh = Color(0xFFCBD5E1),
    surfaceContainerHighest = Color(0xFF94A3B8),

    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),

    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFF2DD4BF),

    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),

    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),

    scrim = Color(0xFF000000)
)

// ===== TEMA PRINCIPAL =====

@Composable
fun StockCubaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Forzar dark=true para que coincida con el diseño (modo oscuro inmersivo)
    content: @Composable () -> Unit
) {
    // Consumir el modo de tema desde CompositionLocal
    val temaMode = LocalTemaPreference.current
    val effectiveDarkTheme = temaMode.toDarkThemeBoolean(isSystemInDarkTheme())
    val colorScheme = if (effectiveDarkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    val context = LocalContext.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = context as? Activity
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !effectiveDarkTheme
                insetsController.isAppearanceLightNavigationBars = !effectiveDarkTheme

                // Colores de barra de estado/navegación que coinciden con el fondo
                val statusBarColor = when {
                    effectiveDarkTheme -> colorScheme.background.toArgb()
                    else -> colorScheme.surface.toArgb()
                }
                val navBarColor = colorScheme.surfaceContainer.toArgb()

                window.statusBarColor = statusBarColor
                window.navigationBarColor = navBarColor
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography.StockCubaTypography,
        shapes = Shape.StockCubaShapes,
        content = content
    )
}

// ===== PREVIEW HELPER =====

@Composable
fun StockCubaThemePreview(
    darkTheme: Boolean = true, // Default a dark para previews
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography.StockCubaTypography,
        shapes = Shape.StockCubaShapes,
        content = content
    )
}

// ===== ELEVACIÓN / SOMBRAS =====
// Tonal Layering + Glassmorphism

object StockCubaElevation {
    // Level 0: Background #0F172A (sin sombra)
    // Level 1: Cards #1E293B + 1px border #334155
    val CardBorder = Color(0xFF334155)
    val CardBorderWidth = 1.dp

    // Level 2: Modals - 70% opacity + 20px blur + glow
    val ModalBackgroundOpacity = 0.7f
    val ModalBlurRadius = 20.dp
    val ModalGlowShadow = Color(0x1A2DD4BF) // Teal glow
    val ModalGlowShadowCoral = Color(0x1AFB7185) // Coral glow para alertas

    // Sombras difusas tinted navy
    val ShadowColor = Color(0xFF0F172A).copy(alpha = 0.15f)
    val ShadowElevation1dp = 1.dp
    val ShadowElevation4dp = 4.dp
    val ShadowElevation8dp = 8.dp
    val ShadowElevation16dp = 16.dp
    val ShadowElevation24dp = 24.dp
}