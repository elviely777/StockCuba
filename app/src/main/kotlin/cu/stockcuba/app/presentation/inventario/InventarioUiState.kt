package cu.stockcuba.app.presentation.inventario

import androidx.compose.ui.graphics.Color
import cu.stockcuba.app.domain.model.MovimientoInventario
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.TipoMovimientoInventario

/**
 * Estado de UI para la pantalla de Inventario.
 */
sealed interface InventarioUiState {
    data class Success(
        val productos: List<ProductoConStock> = emptyList(),
        val query: String = "",
        val filtroStock: FiltroStock = FiltroStock.TODOS,
        val isLoading: Boolean = false,
        val totalOk: Int = 0,
        val totalBajo: Int = 0,
        val totalSinStock: Int = 0,
        val totalArticulos: Int = 0
    ) : InventarioUiState

    data object Loading : InventarioUiState

    data class Error(val message: String) : InventarioUiState

    companion object {
        val empty = Success()
    }
}

/**
 * Producto extendido con info de stock calculada.
 */
data class ProductoConStock(
    val producto: Producto,
    val stockStatus: StockStatus,
    val porcentajeStock: Float // 0.0 - 1.0+
) {
    val stockDisponible: Int
        get() = producto.stockActual

    val stockMinimo: Int
        get() = producto.stockMinimo
}

/**
 * Filtros de stock para la lista.
 */
enum class FiltroStock {
    TODOS,
    OK,        // Verde - stock > minimo
    BAJO,      // Amarillo - stock <= minimo y > 0
    SIN_STOCK  // Rojo - stock <= 0
}

/**
 * Estado de stock visual.
 */
enum class StockStatus {
    OK,        // Verde
    BAJO,      // Amarillo/Naranja
    SIN_STOCK  // Rojo
}

fun ProductoConStock.stockStatusColor(): androidx.compose.ui.graphics.Color {
    return when (stockStatus) {
        StockStatus.OK -> cu.stockcuba.app.presentation.theme.StockCubaColors.VerdeExito
        StockStatus.BAJO -> Color(0xFFF59E0B) // Amber/Warning
        StockStatus.SIN_STOCK -> cu.stockcuba.app.presentation.theme.StockCubaColors.CoralAlerta
    }
}

fun ProductoConStock.stockStatusBackground(): androidx.compose.ui.graphics.Color {
    return when (stockStatus) {
        StockStatus.OK -> cu.stockcuba.app.presentation.theme.StockCubaColors.ChipStockAltoFondo
        StockStatus.BAJO -> Color(0x1AF59E0B) // 10% Amber
        StockStatus.SIN_STOCK -> cu.stockcuba.app.presentation.theme.StockCubaColors.ChipStockBajoFondo
    }
}

fun ProductoConStock.stockStatusLabel(): String {
    return when (stockStatus) {
        StockStatus.OK -> "OK"
        StockStatus.BAJO -> "BAJO"
        StockStatus.SIN_STOCK -> "SIN STOCK"
    }
}

/**
 * Producto original + calcular status.
 */
fun Producto.toProductoConStock(): ProductoConStock {
    val status = when {
        stockActual <= 0 -> StockStatus.SIN_STOCK
        stockActual <= stockMinimo -> StockStatus.BAJO
        else -> StockStatus.OK
    }
    val porcentaje = if (stockMinimo > 0) {
        (stockActual.toFloat() / stockMinimo).coerceAtMost(2.0f)
    } else {
        2.0f
    }
    return ProductoConStock(this, status, porcentaje)
}

/**
 * Estado para diálogo de ajuste de inventario.
 */
sealed interface AjusteInventarioUiState {
    data class Editing(
        val producto: Producto,
        val tipo: TipoMovimientoInventario = TipoMovimientoInventario.ENTRADA,
        val cantidad: String = "",
        val motivo: String = "",
        val isLoading: Boolean = false,
        val errors: Map<String, String> = emptyMap()
    ) : AjusteInventarioUiState

    data object Saving : AjusteInventarioUiState

    data object Saved : AjusteInventarioUiState

    data class Error(val message: String) : AjusteInventarioUiState
}

/**
 * Estado para historial de movimientos.
 */
sealed interface HistorialMovimientosUiState {
    data class Success(
        val movimientos: List<MovimientoInventario> = emptyList(),
        val producto: Producto? = null,
        val isLoading: Boolean = false
    ) : HistorialMovimientosUiState

    data object Loading : HistorialMovimientosUiState

    data class Error(val message: String) : HistorialMovimientosUiState

    companion object {
        val empty = Success()
    }
}