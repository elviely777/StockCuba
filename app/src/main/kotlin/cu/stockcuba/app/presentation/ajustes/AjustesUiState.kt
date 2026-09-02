package cu.stockcuba.app.presentation.ajustes

/**
 * Monedas soportadas.
 */
enum class Moneda {
    CUP, USD, MLC, EUR
}

/**
 * Estado de UI para la pantalla de Ajustes.
 */
sealed interface AjustesUiState {
    data class Success(
        val nombreNegocio: String = "Mi Negocio",
        val direccion: String = "",
        val telefono: String = "",
        val moneda: Moneda = Moneda.CUP,
        val impuesto: Double = 0.0,
        val tema: String = "SYSTEM",
        val seguridadBiometrica: Boolean = false,
        val tienePin: Boolean = false,
        val appVersion: String = "1.0.0",
        val isVinculado: Boolean = false,
        val businessId: String = "",
        val isLoading: Boolean = false,
        val validationErrors: Map<String, String> = emptyMap()
    ) : AjustesUiState {
        /** Error de validación para el campo nombre, o null si es válido */
        val nombreError: String? get() = validationErrors["nombre"]

        /** Error de validación para el campo teléfono, o null si es válido */
        val telefonoError: String? get() = validationErrors["telefono"]

        /** Error de validación para el campo impuesto, o null si es válido */
        val impuestoError: String? get() = validationErrors["impuesto"]

        /** Indica si hay algún error de validación */
        val hasValidationErrors: Boolean get() = validationErrors.isNotEmpty()
    }

    data object Loading : AjustesUiState

    data class Error(val message: String) : AjustesUiState

    data object Saved : AjustesUiState

    companion object {
        val empty = Success()
    }
}
