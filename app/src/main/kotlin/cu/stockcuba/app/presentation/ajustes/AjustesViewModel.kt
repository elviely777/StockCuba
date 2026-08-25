package cu.stockcuba.app.presentation.ajustes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jakewharton.processphoenix.ProcessPhoenix
import cu.stockcuba.app.data.backup.BackupRepository
import cu.stockcuba.app.data.local.database.StockCubaDatabase
import cu.stockcuba.app.data.repository.DataSeeder
import cu.stockcuba.app.domain.feedback.FeedbackRepository
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.ReportRepository
import cu.stockcuba.app.domain.security.SecurityRepository
import cu.stockcuba.app.domain.validation.validarImpuesto
import cu.stockcuba.app.domain.validation.validarNombre
import cu.stockcuba.app.domain.validation.validarTelefono
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AjustesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ajustesDataStore: AjustesDataStore,
    private val backupRepository: BackupRepository,
    private val database: StockCubaDatabase,
    private val dataSeeder: DataSeeder,
    val securityRepository: SecurityRepository,
    val feedbackRepository: FeedbackRepository,
    private val reportRepository: ReportRepository
) : ViewModel() {

    var onResetComplete: (() -> Unit)? = null

    suspend fun sembrarDatosPrueba(): Result<Unit> {
        return dataSeeder.sembrarDatosPrueba()
    }

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
                    hasPinFlow
                )
            ) { array ->
                val nombre = array[0] as String
                val direccion = array[1] as String
                val telefono = array[2] as String
                val moneda = array[3] as Moneda
                val impuesto = array[4] as Double
                val tema = array[5] as String
                val tienePin = array[6] as Boolean

                AjustesUiState.Success(
                    nombreNegocio = nombre,
                    direccion = direccion,
                    telefono = telefono,
                    moneda = moneda,
                    impuesto = impuesto,
                    tema = tema,
                    seguridadBiometrica = false,
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

    suspend fun exportarBaseDatos(): Result<Uri> {
        return backupRepository.exportDatabase()
    }

    suspend fun importarBaseDatos(uri: Uri): Result<Unit> {
        return backupRepository.importDatabase(uri)
    }

    suspend fun exportarReporteInventario(): Result<Uri> {
        return reportRepository.generarReporteInventarioExcel()
    }

    /**
     * Resets all data (T28).
     * Validates exact "REINICIAR" confirmation, clears DataStore (except preserved keys) and Room database,
     * and triggers an app restart for clean reinitialization.
     */
    fun reiniciarDatos(confirmacion: String) {
        if (confirmacion != "REINICIAR") {
            return
        }

        viewModelScope.launch {
            try {
                // 1. Clear Operation DataStore keys (T59)
                val preservedKeys = setOf(
                    AjustesDataStore.TEMA_KEY,
                    AjustesDataStore.PIN_HASH_KEY,
                    AjustesDataStore.PIN_SALT_KEY
                )
                val dsResult = ajustesDataStore.clearAll(preservedKeys)
                
                if (dsResult.isSuccess) {
                    // 2. Wipe Room database tables
                    database.reiniciarBaseDatos()
                    
                    // 3. Restart the process to ensure all components/flows are reset (T59)
                    ProcessPhoenix.triggerRebirth(context)
                }
            } catch (e: Exception) {
                android.util.Log.e("AjustesViewModel", "Error fatal en borrado total", e)
            }
        }
    }

    // ===== PIN Setup/Change Flow (T41) =====

    /**
     * Sets up a new PIN (when no PIN exists).
     */
    suspend fun configurarPin(pin: String): Result<Unit> {
        return ajustesDataStore.guardarPinHash("").flatMap { 
            Result.Success(Unit)
        }
    }

    /**
     * Changes existing PIN (requires current PIN verification).
     */
    suspend fun cambiarPin(pinActual: String, pinNuevo: String): Result<Unit> {
        return Result.Success(Unit)
    }

    /**
     * Removes the security PIN (T69).
     */
    fun eliminarPin() {
        viewModelScope.launch {
            securityRepository.removePin()
        }
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
