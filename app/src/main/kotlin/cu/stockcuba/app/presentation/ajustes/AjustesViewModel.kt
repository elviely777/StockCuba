package cu.stockcuba.app.presentation.ajustes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jakewharton.processphoenix.ProcessPhoenix
import cu.stockcuba.app.BuildConfig
import cu.stockcuba.app.data.backup.BackupRepository
import cu.stockcuba.app.data.local.database.StockCubaDatabase
import cu.stockcuba.app.domain.model.Moneda
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
import kotlinx.coroutines.flow.*
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
    private val reportRepository: ReportRepository,
    private val printerService: cu.stockcuba.app.data.service.BluetoothPrinterService
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
            val hasPinFlow = flow {
                val result = securityRepository.hasPin()
                emit(result.fold(onSuccess = { it }, onFailure = { false }))
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

            combine(
                ajustesDataStore.nombreNegocio,
                ajustesDataStore.direccion,
                ajustesDataStore.telefono,
                ajustesDataStore.moneda,
                ajustesDataStore.impuesto,
                ajustesDataStore.tema,
                ajustesDataStore.isVinculado,
                ajustesDataStore.businessId,
                hasPinFlow,
                ajustesDataStore.tasaUSD,
                ajustesDataStore.tasaMLC,
                ajustesDataStore.tasaEUR,
                ajustesDataStore.printerName,
                ajustesDataStore.printerMac
            ) { array ->
                AjustesUiState.Success(
                    nombreNegocio = array[0] as String,
                    direccion = array[1] as String,
                    telefono = array[2] as String,
                    moneda = array[3] as Moneda,
                    impuesto = array[4] as Double,
                    tema = array[5] as String,
                    isVinculado = array[6] as Boolean,
                    businessId = (array[7] as? String) ?: "",
                    tienePin = array[8] as Boolean,
                    tasaUSD = array[9] as Double,
                    tasaMLC = array[10] as Double,
                    tasaEUR = array[11] as Double,
                    printerName = (array[12] as? String) ?: "No vinculada",
                    printerMac = array[13] as? String,
                    appVersion = BuildConfig.VERSION_NAME,
                    validationErrors = emptyMap()
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun guardarNombreNegocio(nombre: String) {
        val currentState = _uiState.value as? AjustesUiState.Success ?: return
        val resultado = validarNombre(nombre)
        val nuevosErrores = when (resultado) {
            is Result.Success -> {
                viewModelScope.launch { ajustesDataStore.guardarNombreNegocio(resultado.value) }
                currentState.validationErrors - "nombre"
            }
            is Result.Failure -> currentState.validationErrors + ("nombre" to resultado.error.toString())
        }
        _uiState.value = currentState.copy(nombreNegocio = nombre, validationErrors = nuevosErrores)
    }

    fun guardarDireccion(direccion: String) {
        viewModelScope.launch { ajustesDataStore.guardarDireccion(direccion.trim()) }
    }

    fun guardarTelefono(telefono: String) {
        val currentState = _uiState.value as? AjustesUiState.Success ?: return
        val telefonoLimpio = telefono.filter { it.isDigit() || it == '+' }
        viewModelScope.launch { ajustesDataStore.guardarTelefono(telefonoLimpio) }
        val resultado = validarTelefono(telefonoLimpio)
        val nuevosErrores = when (resultado) {
            is Result.Success -> currentState.validationErrors - "telefono"
            is Result.Failure -> if (telefonoLimpio.length >= 8) currentState.validationErrors + ("telefono" to resultado.error.toString()) else currentState.validationErrors - "telefono"
        }
        _uiState.value = currentState.copy(telefono = telefonoLimpio, validationErrors = nuevosErrores)
    }

    fun guardarMoneda(moneda: Moneda) {
        viewModelScope.launch { ajustesDataStore.guardarMoneda(moneda) }
    }

    fun guardarImpuesto(impuesto: String) {
        val resultado = validarImpuesto(impuesto)
        val currentState = _uiState.value as? AjustesUiState.Success ?: return
        val nuevosErrores = when (resultado) {
            is Result.Success -> {
                viewModelScope.launch { ajustesDataStore.guardarImpuesto(resultado.value) }
                currentState.validationErrors - "impuesto"
            }
            is Result.Failure -> currentState.validationErrors + ("impuesto" to resultado.error.toString())
        }
        _uiState.value = currentState.copy(validationErrors = nuevosErrores)
    }

    fun guardarTema(tema: String) {
        viewModelScope.launch { ajustesDataStore.guardarTema(tema) }
    }

    fun guardarTasaUSD(tasa: String) {
        tasa.toDoubleOrNull()?.let { viewModelScope.launch { ajustesDataStore.guardarTasaUSD(it) } }
    }

    fun guardarTasaMLC(tasa: String) {
        tasa.toDoubleOrNull()?.let { viewModelScope.launch { ajustesDataStore.guardarTasaMLC(it) } }
    }

    fun guardarTasaEUR(tasa: String) {
        tasa.toDoubleOrNull()?.let { viewModelScope.launch { ajustesDataStore.guardarTasaEUR(it) } }
    }

    suspend fun exportarBaseDatos(): Result<Uri> = backupRepository.exportDatabase()
    suspend fun importarBaseDatos(uri: Uri): Result<Unit> = backupRepository.importDatabase(uri)
    suspend fun exportarReporteInventario(): Result<Uri> = reportRepository.generarReporteInventarioExcel()

    fun reiniciarDatos(confirmacion: String) {
        if (confirmacion != "REINICIAR") return
        viewModelScope.launch {
            try {
                val preservedKeys = setOf(AjustesDataStore.TEMA_KEY, AjustesDataStore.PIN_HASH_KEY, AjustesDataStore.PIN_SALT_KEY)
                if (ajustesDataStore.clearAll(preservedKeys).isSuccess) {
                    database.reiniciarBaseDatos()
                    ProcessPhoenix.triggerRebirth(context)
                }
            } catch (e: Exception) {
                android.util.Log.e("AjustesViewModel", "Error fatal en borrado total", e)
            }
        }
    }

    fun eliminarPin() {
        viewModelScope.launch { securityRepository.removePin() }
    }

    suspend fun sendFeedback(): Result<Unit> = feedbackRepository.sendFeedback()

    // ===== Bluetooth Printer (T76) =====
    fun getPairedPrinters(): List<android.bluetooth.BluetoothDevice> = printerService.getPairedDevices()

    fun vincularImpresora(device: android.bluetooth.BluetoothDevice) {
        viewModelScope.launch {
            @android.annotation.SuppressLint("MissingPermission")
            val name = device.name ?: "Impresora Térmica"
            ajustesDataStore.guardarImpresora(device.address, name)
        }
    }

    fun desvincularImpresora() {
        viewModelScope.launch { ajustesDataStore.desvincularImpresora() }
    }
}
