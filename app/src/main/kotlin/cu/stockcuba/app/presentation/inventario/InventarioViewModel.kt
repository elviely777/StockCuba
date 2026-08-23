package cu.stockcuba.app.presentation.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.MovimientoInventario
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.TipoMovimientoInventario
import cu.stockcuba.app.domain.repository.InventarioRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InventarioViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val inventarioRepository: InventarioRepository
) : ViewModel() {

    private val _filtroQuery = MutableStateFlow("")
    private val _filtroStock = MutableStateFlow(FiltroStock.TODOS)

    val filtroQuery = _filtroQuery
    val filtroStock = _filtroStock

    // StateFlow combinado: productos filtrados
    val uiState = combine(
        productoRepository.getAll(),
        _filtroQuery,
        _filtroStock
    ) { allProductos, query, filtro ->
        val filtered = allProductos
            .filter { it.activo }
            .map { it.toProductoConStock() }
            .filter { productoConStock ->
                val matchesQuery = query.isBlank() ||
                    productoConStock.producto.nombre.lowercase(java.util.Locale.getDefault()).contains(query.lowercase(java.util.Locale.getDefault())) ||
                    productoConStock.producto.descripcion?.lowercase(java.util.Locale.getDefault())?.contains(query.lowercase(java.util.Locale.getDefault())) == true

                val matchesFiltro = when (filtro) {
                    FiltroStock.TODOS -> true
                    FiltroStock.OK -> productoConStock.stockStatus == StockStatus.OK
                    FiltroStock.BAJO -> productoConStock.stockStatus == StockStatus.BAJO
                    FiltroStock.SIN_STOCK -> productoConStock.stockStatus == StockStatus.SIN_STOCK
                }

                matchesQuery && matchesFiltro
            }
            .sortedBy { it.producto.nombre }

        InventarioUiState.Success(
            productos = filtered,
            query = query,
            filtroStock = filtro,
            isLoading = false
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InventarioUiState.empty
        )

    fun setQuery(query: String) {
        _filtroQuery.value = query
    }

    fun setFiltroStock(filtro: FiltroStock) {
        _filtroStock.value = filtro
    }

    fun limpiarFiltros() {
        _filtroQuery.value = ""
        _filtroStock.value = FiltroStock.TODOS
    }

    /**
     * Registra un movimiento de inventario (entrada/ajuste).
     */
    suspend fun registrarMovimiento(
        producto: Producto,
        tipo: TipoMovimientoInventario,
        cantidad: Int,
        motivo: String?
    ): Result<Unit> {
        val movimiento = MovimientoInventario(
            id = UUID.randomUUID().toString(),
            productoId = producto.id,
            tipo = tipo,
            cantidad = cantidad,
            fecha = java.time.Instant.now(),
            motivo = motivo?.takeIf { it.isNotBlank() }
        )
        return inventarioRepository.registrarMovimiento(movimiento)
    }
}