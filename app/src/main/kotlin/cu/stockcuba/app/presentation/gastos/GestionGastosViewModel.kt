package cu.stockcuba.app.presentation.gastos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.Gasto
import cu.stockcuba.app.domain.model.Moneda
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.TipoGasto
import cu.stockcuba.app.domain.repository.GastoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GestionGastosViewModel @Inject constructor(
    private val gastoRepository: GastoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GestionGastosUiState>(GestionGastosUiState.Loading)
    val uiState: StateFlow<GestionGastosUiState> = _uiState.asStateFlow()

    init {
        cargarGastos()
    }

    private fun cargarGastos() {
        gastoRepository.getAll().onEach { gastos ->
            _uiState.update { 
                GestionGastosUiState.Success(
                    gastos = gastos,
                    totalHoy = gastos.filter { isHoy(it.fecha) }.sumOf { it.monto } // Simplificado, no maneja tasas aquí
                )
            }
        }.launchIn(viewModelScope)
    }

    fun registrarGasto(concepto: String, monto: Double, tipo: TipoGasto, moneda: Moneda) {
        viewModelScope.launch {
            val gasto = Gasto(
                id = UUID.randomUUID().toString(),
                concepto = concepto,
                monto = monto,
                tipo = tipo,
                moneda = moneda,
                fecha = Instant.now()
            )
            gastoRepository.registrarGasto(gasto)
        }
    }

    fun eliminarGasto(gasto: Gasto) {
        viewModelScope.launch {
            gastoRepository.eliminarGasto(gasto)
        }
    }

    private fun isHoy(fecha: Instant): Boolean {
        val hoy = java.time.LocalDate.now()
        val fechaGasto = fecha.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return hoy == fechaGasto
    }
}

sealed interface GestionGastosUiState {
    data object Loading : GestionGastosUiState
    data class Success(
        val gastos: List<Gasto>,
        val totalHoy: Double
    ) : GestionGastosUiState
    data class Error(val message: String) : GestionGastosUiState
}
