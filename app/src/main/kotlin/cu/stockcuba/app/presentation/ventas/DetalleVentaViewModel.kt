package cu.stockcuba.app.presentation.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.ClienteRepository
import cu.stockcuba.app.domain.repository.VentaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetalleVentaViewModel @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val clienteRepository: ClienteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetalleVentaUiState>(DetalleVentaUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun cargarVenta(ventaId: String) {
        viewModelScope.launch {
            _uiState.update { DetalleVentaUiState.Loading }
            
            val ventaResult = ventaRepository.getByIdSync(ventaId)
            
            if (ventaResult is Result.Success) {
                val venta = ventaResult.value
                val items = ventaRepository.getItemsByVentaId(ventaId).valueOrNull ?: emptyList()
                
                val fullVenta = venta.copy(items = items)
                
                var cliente: cu.stockcuba.app.domain.model.Cliente? = null
                if (venta.clienteId != null) {
                    cliente = clienteRepository.getByIdSync(venta.clienteId).valueOrNull
                }
                
                _uiState.update { DetalleVentaUiState.Success(fullVenta, cliente) }
            } else if (ventaResult is Result.Failure) {
                _uiState.update { DetalleVentaUiState.Error(ventaResult.error.toString()) }
            }
        }
    }
}
