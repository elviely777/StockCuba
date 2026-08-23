package cu.stockcuba.app.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import cu.stockcuba.app.domain.model.ThemeMode
import cu.stockcuba.app.presentation.theme.ThemeViewModel

/**
 * Composable que lee el tema desde [ThemeViewModel] y lo provee
 * via [LocalTemaPreference] para que [StockCubaTheme] lo consuma.
 *
 * Usa [collectAsStateWithLifecycle] para observar el StateFlow del ViewModel
 * y actualizar el CompositionLocal automáticamente cuando cambia la preferencia,
 * causando recomposición.
 */
@Composable
fun TemaProvider(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    // Observar el ThemeMode del ViewModel
    val temaMode by themeViewModel.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

    CompositionLocalProvider(LocalTemaPreference provides temaMode) {
        content()
    }
}
