package cu.stockcuba.app.presentation.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.repository.VentaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistorialVentasViewModel @Inject constructor(
    private val ventaRepository: VentaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialVentasUiState.empty)
    val uiState = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistorialVentasUiState.empty)

    init {
        cargarHistorial()
    }

    private fun cargarHistorial() {
        viewModelScope.launch {
            ventaRepository.getAll().firstOrNull()?.let { allVentas ->
                val ventasPorDia = agruparPorDia(allVentas)
                _uiState.update {
                    HistorialVentasUiState.Success(ventasPorDia = ventasPorDia)
                }
            }
        }
    }

    /**
     * Agrupa las ventas por día.
     */
    private fun agruparPorDia(ventas: List<Venta>): List<VentasPorDia> {
        return ventas
            .groupBy { venta ->
                venta.fecha
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }
            .map { (fechaInicio, ventasDelDia) ->
                VentasPorDia(
                    fecha = fechaInicio,
                    ventas = ventasDelDia.sortedByDescending { it.fecha },
                    totalDia = ventasDelDia.sumOf { it.total }
                )
            }
            .sortedByDescending { it.fecha }
    }

    fun refrescar() {
        cargarHistorial()
    }
}