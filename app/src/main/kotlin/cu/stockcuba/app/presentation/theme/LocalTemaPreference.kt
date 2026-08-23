package cu.stockcuba.app.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import cu.stockcuba.app.domain.model.ThemeMode

/**
 * CompositionLocal que expone el modo de tema actual (SYSTEM/LIGHT/DARK)
 * leído desde DataStore. Se actualiza en vivo cuando cambia la preferencia.
 *
 * Default: SYSTEM (sigue la configuración del sistema operativo)
 */
val LocalTemaPreference = staticCompositionLocalOf { ThemeMode.SYSTEM }