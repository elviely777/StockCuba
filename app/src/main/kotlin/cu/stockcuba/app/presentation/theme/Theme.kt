package cu.stockcuba.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import cu.stockcuba.app.domain.model.ThemeMode

/**
 * Tema StockCuba · "Mostrador".
 *
 * El modo oscuro es el principal: fondo de tinta, paneles con borde de 1px y
 * acentos con significado. El modo claro es una variante de papel frío, no una
 * inversión automática: comparte la misma jerarquía y los mismos acentos
 * oscurecidos para mantener el contraste AA.
 *
 * Todo se construye desde [StockCubaSemanticColors], así que cambiar un token
 * cambia el sistema completo (y las pantallas antiguas se adaptan solas).
 */

private fun esquemaOscuro() = darkColorScheme(
    primary = ColoresOscuros.acento,
    onPrimary = ColoresOscuros.sobreAcento,
    primaryContainer = ColoresOscuros.acentoContenedor,
    onPrimaryContainer = ColoresOscuros.sobreAcentoContenedor,
    inversePrimary = ColoresOscuros.acento,

    secondary = ColoresOscuros.dinero,
    onSecondary = ColoresOscuros.sobreDinero,
    secondaryContainer = ColoresOscuros.dineroContenedor,
    onSecondaryContainer = ColoresOscuros.sobreDineroContenedor,

    tertiary = ColoresOscuros.info,
    onTertiary = ColoresOscuros.sobreInfo,
    tertiaryContainer = ColoresOscuros.infoContenedor,
    onTertiaryContainer = ColoresOscuros.sobreInfoContenedor,

    error = ColoresOscuros.alerta,
    onError = ColoresOscuros.sobreAlerta,
    errorContainer = ColoresOscuros.alertaContenedor,
    onErrorContainer = ColoresOscuros.sobreAlertaContenedor,

    background = ColoresOscuros.canvas,
    onBackground = ColoresOscuros.texto,

    surface = ColoresOscuros.canvas,
    onSurface = ColoresOscuros.texto,
    surfaceDim = ColoresOscuros.canvas,
    surfaceBright = ColoresOscuros.panelAlto,
    surfaceContainerLowest = ColoresOscuros.canvas,
    surfaceContainerLow = ColoresOscuros.panelHundido,
    surfaceContainer = ColoresOscuros.panel,
    surfaceContainerHigh = ColoresOscuros.panelAlto,
    surfaceContainerHighest = ColoresOscuros.panelAlto,
    surfaceVariant = ColoresOscuros.panelAlto,
    onSurfaceVariant = ColoresOscuros.textoSecundario,
    surfaceTint = ColoresOscuros.acento,

    outline = ColoresOscuros.borde,
    outlineVariant = ColoresOscuros.bordeFuerte,

    inverseSurface = ColoresOscuros.texto,
    inverseOnSurface = ColoresOscuros.canvas,

    scrim = ColoresOscuros.scrim
)

private fun esquemaClaro() = lightColorScheme(
    primary = ColoresClaros.acento,
    onPrimary = ColoresClaros.sobreAcento,
    primaryContainer = ColoresClaros.acentoContenedor,
    onPrimaryContainer = ColoresClaros.sobreAcentoContenedor,
    inversePrimary = ColoresClaros.acento,

    secondary = ColoresClaros.dinero,
    onSecondary = ColoresClaros.sobreDinero,
    secondaryContainer = ColoresClaros.dineroContenedor,
    onSecondaryContainer = ColoresClaros.sobreDineroContenedor,

    tertiary = ColoresClaros.info,
    onTertiary = ColoresClaros.sobreInfo,
    tertiaryContainer = ColoresClaros.infoContenedor,
    onTertiaryContainer = ColoresClaros.sobreInfoContenedor,

    error = ColoresClaros.alerta,
    onError = ColoresClaros.sobreAlerta,
    errorContainer = ColoresClaros.alertaContenedor,
    onErrorContainer = ColoresClaros.sobreAlertaContenedor,

    background = ColoresClaros.canvas,
    onBackground = ColoresClaros.texto,

    surface = ColoresClaros.canvas,
    onSurface = ColoresClaros.texto,
    surfaceDim = ColoresClaros.canvas,
    surfaceBright = ColoresClaros.panel,
    surfaceContainerLowest = ColoresClaros.panel,
    surfaceContainerLow = ColoresClaros.panelHundido,
    surfaceContainer = ColoresClaros.panel,
    surfaceContainerHigh = ColoresClaros.panelAlto,
    surfaceContainerHighest = ColoresClaros.panelHundido,
    surfaceVariant = ColoresClaros.panelAlto,
    onSurfaceVariant = ColoresClaros.textoSecundario,
    surfaceTint = ColoresClaros.acento,

    outline = ColoresClaros.borde,
    outlineVariant = ColoresClaros.bordeFuerte,

    inverseSurface = ColoresClaros.texto,
    inverseOnSurface = ColoresClaros.canvas,

    scrim = ColoresClaros.scrim
)

@Composable
fun StockCubaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Modo elegido por el usuario (OSCURO por defecto desde v2.0).
    val temaMode = LocalTemaPreference.current
    val effectiveDarkTheme = temaMode.toDarkThemeBoolean(isSystemInDarkTheme())
    val semantic = if (effectiveDarkTheme) ColoresOscuros else ColoresClaros
    val colorScheme = if (effectiveDarkTheme) esquemaOscuro() else esquemaClaro()
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

                // Barras transparentes: el contenido del sistema flota sobre el
                // lienzo del app en lugar de dibujar una franja de otro color.
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
            }
        }
    }

    CompositionLocalProvider(LocalStockCubaColors provides semantic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography.StockCubaTypography,
            shapes = Shape.StockCubaShapes,
            content = content
        )
    }
}

@Composable
fun StockCubaThemePreview(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val semantic = if (darkTheme) ColoresOscuros else ColoresClaros
    val colorScheme = if (darkTheme) esquemaOscuro() else esquemaClaro()
    CompositionLocalProvider(LocalStockCubaColors provides semantic) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography.StockCubaTypography,
            shapes = Shape.StockCubaShapes,
            content = content
        )
    }
}

/**
 * Elevación: el sistema NO usa sombras difusas. La profundidad se comunica con
 * capas tonales (canvas → panel → panel alto) y un borde de 1px. Esta clase
 * queda por compatibilidad con pantallas antiguas.
 */
object StockCubaElevation {
    val CardBorder = ColoresOscuros.borde
    val CardBorderWidth = 1.dp

    val ModalBackgroundOpacity = 0.86f
    val ModalBlurRadius = 20.dp
    val ModalGlowShadow = ColoresOscuros.acento.copy(alpha = 0.10f)
    val ModalGlowShadowCoral = ColoresOscuros.alerta.copy(alpha = 0.10f)

    val ShadowColor = ColoresOscuros.canvas
    val ShadowElevation1dp = 0.dp
    val ShadowElevation4dp = 0.dp
    val ShadowElevation8dp = 0.dp
    val ShadowElevation16dp = 0.dp
    val ShadowElevation24dp = 0.dp
}
