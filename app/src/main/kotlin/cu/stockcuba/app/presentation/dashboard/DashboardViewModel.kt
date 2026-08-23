package cu.stockcuba.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.usecase.ObtenerProductosBajoStockUseCase
import cu.stockcuba.app.domain.usecase.ObtenerResumenDelDiaUseCase
import cu.stockcuba.app.domain.usecase.ObtenerVentasDeHoyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val obtenerVentasDeHoyUseCase: ObtenerVentasDeHoyUseCase,
    private val obtenerResumenDelDiaUseCase: ObtenerResumenDelDiaUseCase,
    private val obtenerProductosBajoStockUseCase: ObtenerProductosBajoStockUseCase
) : ViewModel() {

    // StateFlow que combina todos los datos del dashboard
    val uiState = combine(
        obtenerVentasDeHoyUseCase(),
        obtenerProductosBajoStockUseCase()
    ) { ventasHoy, productosBajoStock ->
        Pair(ventasHoy, productosBajoStock)
    }.flatMapLatest { (ventasHoy, productosBajoStock) ->
        flow {
            val resumenResult = obtenerResumenDelDiaUseCase()
            when (resumenResult) {
                is cu.stockcuba.app.domain.model.Result.Success -> {
                    val resumen = resumenResult.value
                    emit(DashboardUiState.Success(
                        totalVendidoHoy = resumen.totalVendido,
                        cantidadVentasHoy = ventasHoy.size,
                        productoMasVendido = resumen.productoMasVendido,
                        listaProductosBajoStock = productosBajoStock,
                        isLoading = false
                    ))
                }
                is cu.stockcuba.app.domain.model.Result.Failure -> {
                    emit(DashboardUiState.Error(resumenResult.error.toString()))
                }
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )

    fun refresh() {
        // Los flows se actualizan automáticamente al cambiar los datos en Room
    }
}