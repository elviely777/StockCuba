package cu.stockcuba.app.presentation.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.repository.ClienteRepository
import cu.stockcuba.app.domain.repository.VentaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistorialVentasViewModel @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val clienteRepository: ClienteRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)

    val uiState = combine(
        ventaRepository.getAll(),
        clienteRepository.getAll(),
        _isRefreshing
    ) { ventas, clientes, refreshing ->
        val clientesMap = clientes.associateBy { it.id }
        val ventasPorDia = agruparPorDia(ventas, clientesMap)
        
        HistorialVentasUiState.Success(
            ventasPorDia = ventasPorDia,
            isLoading = refreshing
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistorialVentasUiState.Loading
    )

    private fun agruparPorDia(ventas: List<Venta>, clientesMap: Map<String, cu.stockcuba.app.domain.model.Cliente>): List<VentasPorDia> {
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
                val ventasUi = ventasDelDia.map { venta ->
                    VentaUi(
                        venta = venta,
                        clienteNombre = venta.clienteId?.let { clientesMap[it]?.nombre }
                    )
                }.sortedByDescending { it.venta.fecha }

                VentasPorDia(
                    fecha = fechaInicio,
                    ventas = ventasUi,
                    totalDia = ventasDelDia.sumOf { it.total }
                )
            }
            .sortedByDescending { it.fecha }
    }

    fun refrescar() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // En un app real, aquí llamaríamos a un sync o forzaríamos recarga del Repo
            _isRefreshing.value = false
        }
    }
}
