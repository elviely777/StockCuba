package cu.stockcuba.app.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.ThemeMode
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel que lee el tema desde [AjustesDataStore] y expone el [ThemeMode]
 * para uso en [TemaProvider] y [StockCubaTheme].
 *
 * Expone el ThemeMode directamente (SYSTEM/LIGHT/DARK) para que el composable
 * decida cómo mapearlo a Boolean usando [ThemeMode.toDarkThemeBoolean()].
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val ajustesDataStore: AjustesDataStore
) : ViewModel() {

    /**
     * Flow que emite el ThemeMode actual (SYSTEM/LIGHT/DARK).
     * Se actualiza automáticamente cuando cambia la preferencia en DataStore.
     * Cacheado con stateIn para evitar múltiples suscripciones al DataStore.
     */
    val themeMode = ajustesDataStore.tema
        .map { ThemeMode.fromString(it) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM
        )
}