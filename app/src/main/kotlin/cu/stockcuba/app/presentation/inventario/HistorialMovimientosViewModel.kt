package cu.stockcuba.app.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.MovimientoInventario
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.repository.InventarioRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistorialMovimientosViewModel @Inject constructor(
    private val inventarioRepository: InventarioRepository,
    private val productoRepository: ProductoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistorialMovimientosUiState>(HistorialMovimientosUiState.Loading)
    val uiState = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistorialMovimientosUiState.Loading)

    /**
     * Carga el historial de movimientos de un producto.
     */
    fun cargarHistorial(productoId: String) {
        viewModelScope.launch {
            _uiState.value = HistorialMovimientosUiState.Loading

            // Cargar producto
            val productoResult = productoRepository.getByIdSync(productoId)
            val producto = when (productoResult) {
                is cu.stockcuba.app.domain.model.Result.Success -> productoResult.value
                else -> null
            }

            // Cargar movimientos
            val movimientosResult = inventarioRepository.getHistorialPorProducto(productoId).firstOrNull() ?: emptyList()

            _uiState.value = HistorialMovimientosUiState.Success(
                movimientos = movimientosResult,
                producto = producto
            )
        }
    }

    fun refrescar(productoId: String) {
        cargarHistorial(productoId)
    }
}