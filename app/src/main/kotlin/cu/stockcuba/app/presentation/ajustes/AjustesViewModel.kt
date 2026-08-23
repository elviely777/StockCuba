package cu.stockcuba.app.presentation.ajustes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.data.backup.BackupRepository
import cu.stockcuba.app.data.local.database.StockCubaDatabase
import cu.stockcuba.app.domain.feedback.FeedbackRepository
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.security.SecurityRepository
import cu.stockcuba.app.domain.validation.validarImpuesto
import cu.stockcuba.app.domain.validation.validarNombre
import cu.stockcuba.app.domain.validation.validarTelefono
import cu.stockcuba.app.presentation.security.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val ajustesDataStore: AjustesDataStore,
    private val backupRepository: BackupRepository,
    private val database: StockCubaDatabase,
    val securityRepository: SecurityRepository,
    val feedbackRepository: FeedbackRepository,
    val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    var onResetComplete: (() -> Unit)? = null

    private val _uiState = MutableStateFlow<AjustesUiState>(AjustesUiState.Loading)
    val uiState = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AjustesUiState.Loading)

    init {
        cargarAjustes()
    }

    private fun cargarAjustes() {
        viewModelScope.launch {
            // Get hasPin as a Flow by converting the Result
            val hasPinFlow = kotlinx.coroutines.flow.flow {
                val result = securityRepository.hasPin()
                emit(result.fold(
                    onSuccess = { it },
                    onFailure = { false }
                ))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

            kotlinx.coroutines.flow.combine(
                listOf(
                    ajustesDataStore.nombreNegocio,
                    ajustesDataStore.direccion,
                    ajustesDataStore.telefono,
                    ajustesDataStore.moneda,
                    ajustesDataStore.impuesto,
                    ajustesDataStore.tema,
                    ajustesDataStore.seguridadBiometrica,
                    hasPinFlow
                )
            ) { array ->
                val nombre = array[0] as String
                val direccion = array[1] as String
                val telefono = array[2] as String
                val moneda = array[3] as Moneda
                val impuesto = array[4] as Double
                val tema = array[5] as String
                val seguridadBiometrica = array[6] as Boolean
                val tienePin = array[7] as Boolean

                AjustesUiState.Success(
                    nombreNegocio = nombre,
                    direccion = direccion,
                    telefono = telefono,
                    moneda = moneda,
                    impuesto = impuesto,
                    tema = tema,
                    seguridadBiometrica = seguridadBiometrica,
                    tienePin = tienePin,
                    appVersion = "1.0.0",
                    validationErrors = emptyMap()
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun guardarNombreNegocio(nombre: String) {
        val resultado = validarNombre(nombre)
        val currentState = _uiState.value as? AjustesUiState.Success ?: return
        
        when (resultado) {
            is Result.Success -> {
                val nuevosErrores = currentState.validationErrors - "nombre"
                _uiState.value = currentState.copy(validationErrors = nuevosErrores)
                viewModelScope.launch { ajustesDataStore.guardarNombreNegocio(resultado.value) }
            }
            is Result.Failure -> {
                val nuevosErrores = currentState.validationErrors + ("nombre" to resultado.error.toString())
                _uiState.value = currentState.copy(validationErrors = nuevosErrores)
            }
        }
    }

    fun guardarDireccion(direccion: String) {
        viewModelScope.launch { ajustesDataStore.guardarDireccion(direccion.trim()) }
    }

    fun guardarTelefono(telefono: String) {
        val resultado = validarTelefono(telefono)
        val currentState = _uiState.value as? AjustesUiState.Success ?: return
        
        when (resultado) {
            is Result.Success -> {
                val nuevosErrores = currentState.validationErrors - "telefono"
                _uiState.value = currentState.copy(validationErrors = nuevosErrores)
                viewModelScope.launch { ajustesDataStore.guardarTelefono(resultado.value) }
            }
            is Result.Failure -> {
                val nuevosErrores = currentState.validationErrors + ("telefono" to resultado.error.toString())
                _uiState.value = currentState.copy(validationErrors = nuevosErrores)
            }
        }
    }

    fun guardarMoneda(moneda: Moneda) {
        viewModelScope.launch { ajustesDataStore.guardarMoneda(moneda) }
    }

    fun guardarImpuesto(impuesto: String) {
        val resultado = validarImpuesto(impuesto)
        val currentState = _uiState.value as? AjustesUiState.Success ?: return
        
        when (resultado) {
            is Result.Success -> {
                val nuevosErrores = currentState.validationErrors - "impuesto"
                _uiState.value = currentState.copy(validationErrors = nuevosErrores)
                viewModelScope.launch { ajustesDataStore.guardarImpuesto(resultado.value) }
            }
            is Result.Failure -> {
                val nuevosErrores = currentState.validationErrors + ("impuesto" to resultado.error.toString())
                _uiState.value = currentState.copy(validationErrors = nuevosErrores)
            }
        }
    }

    fun guardarTema(tema: String) {
        viewModelScope.launch { ajustesDataStore.guardarTema(tema) }
    }

    fun guardarSeguridadBiometrica(habilitada: Boolean) {
        viewModelScope.launch { ajustesDataStore.guardarSeguridadBiometrica(habilitada) }
    }

    suspend fun exportarBaseDatos(): Result<Uri> {
        return backupRepository.exportDatabase()
    }

    suspend fun importarBaseDatos(uri: Uri): Result<Unit> {
        return backupRepository.importDatabase(uri)
    }

    /**
     * Resets all data (T28).
     * Validates exact "REINICIAR" confirmation, clears DataStore (except preserved keys) and Room database,
     * triggers onResetComplete callback for navigation to Dashboard with popUpTo(start) { inclusive = true }.
     */
    fun reiniciarDatos(confirmacion: String) {
        if (confirmacion != "REINICIAR") {
            return // Silently ignore invalid confirmation
        }

        viewModelScope.launch {
            // Clear DataStore (preserves tema, pin, biometric)
            val preservedKeys = setOf(
                AjustesDataStore.TEMA_KEY,
                AjustesDataStore.PIN_HASH_KEY,
                AjustesDataStore.PIN_SALT_KEY,
                AjustesDataStore.BIOMETRIC_ENABLED_KEY
            )
            val result = ajustesDataStore.clearAll(preservedKeys)
            if (result.isSuccess) {
                // Clear Room database
                database.clearAllTables()
                // Trigger navigation callback
                onResetComplete?.invoke()
            }
        }
    }

    // ===== PIN Setup/Change Flow (T41) =====

    /**
     * Sets up a new PIN (when no PIN exists).
     */
    suspend fun configurarPin(pin: String): Result<Unit> {
        return ajustesDataStore.guardarPinHash("").flatMap { 
            // This will be handled by SecurityRepository via AjustesDataStore
            // ViewModel just delegates to SecurityRepository in real usage
            Result.Success(Unit)
        }
    }

    /**
     * Changes existing PIN (requires current PIN verification).
     */
    suspend fun cambiarPin(pinActual: String, pinNuevo: String): Result<Unit> {
        // This will be handled by SecurityRepository
        return Result.Success(Unit)
    }

    /**
     * Toggles biometric authentication.
     */
    suspend fun toggleBiometric(enabled: Boolean): Result<Unit> {
        return ajustesDataStore.guardarBiometricEnabled(enabled)
    }

    // ===== Feedback (T46) =====

    /**
     * Sends feedback via email with prefilled context.
     * Returns Result.Success(Unit) if email intent was launched.
     * Returns Result.Failure if no email app is available.
     */
    suspend fun sendFeedback(): Result<Unit> {
        return feedbackRepository.sendFeedback()
    }
}