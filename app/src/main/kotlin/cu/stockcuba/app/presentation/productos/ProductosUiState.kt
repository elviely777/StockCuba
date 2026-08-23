package cu.stockcuba.app.presentation.productos

import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.UnidadMedida

/**
 * Estado de UI para la lista de productos.
 */
sealed interface ListaProductosUiState {
    data class Success(
        val productos: List<Producto>,
        val categorias: List<Categoria>,
        val query: String = "",
        val categoriaSeleccionada: String? = null,
        val isLoading: Boolean = false
    ) : ListaProductosUiState

    data object Loading : ListaProductosUiState

    data class Error(val message: String) : ListaProductosUiState

    companion object {
        val empty = Success(productos = emptyList(), categorias = emptyList())
    }
}

/**
 * Estado para formulario de producto (crear/editar).
 */
sealed interface FormularioProductoUiState {
    data class Editing(
        val nombre: String = "",
        val descripcion: String = "",
        val precioVenta: String = "",
        val costoUnitario: String = "",
        val stockInicial: String = "",
        val stockMinimo: String = "",
        val unidadMedida: UnidadMedida = UnidadMedida.UNIDAD,
        val categoriaId: String? = null,
        val categorias: List<Categoria> = emptyList(),
        val isLoading: Boolean = false,
        val errors: Map<String, String> = emptyMap(),
        val isEditing: Boolean = false,
        val productoId: String? = null
    ) : FormularioProductoUiState

    data object Saving : FormularioProductoUiState

    data object Saved : FormularioProductoUiState

    data class Error(val message: String) : FormularioProductoUiState
}

/**
 * Filtros para la lista de productos.
 */
data class ProductosFiltros(
    val query: String = "",
    val categoriaId: String? = null
)

/**
 * Stock status para UI.
 */
enum class StockStatus {
    OK,        // Verde - stock > stockMinimo
    BAJO,      // Coral - stock <= stockMinimo y > 0
    SIN_STOCK  // Coral fuerte - stock <= 0
}

fun Producto.stockStatus(): StockStatus {
    return when {
        stockActual <= 0 -> StockStatus.SIN_STOCK
        stockActual <= stockMinimo -> StockStatus.BAJO
        else -> StockStatus.OK
    }
}

fun Producto.stockStatusColor(): androidx.compose.ui.graphics.Color {
    return when (stockStatus()) {
        StockStatus.OK -> cu.stockcuba.app.presentation.theme.StockCubaColors.VerdeExito
        StockStatus.BAJO -> cu.stockcuba.app.presentation.theme.StockCubaColors.CoralAlerta
        StockStatus.SIN_STOCK -> cu.stockcuba.app.presentation.theme.StockCubaColors.CoralAlerta.copy(alpha = 0.9f)
    }
}

fun Producto.stockStatusBackground(): androidx.compose.ui.graphics.Color {
    return when (stockStatus()) {
        StockStatus.OK -> cu.stockcuba.app.presentation.theme.StockCubaColors.ChipStockAltoFondo
        StockStatus.BAJO -> cu.stockcuba.app.presentation.theme.StockCubaColors.ChipStockBajoFondo
        StockStatus.SIN_STOCK -> cu.stockcuba.app.presentation.theme.StockCubaColors.ChipStockBajoFondo
    }
}

fun Producto.stockStatusLabel(): String {
    return when (stockStatus()) {
        StockStatus.OK -> "OK"
        StockStatus.BAJO -> "BAJO"
        StockStatus.SIN_STOCK -> "SIN STOCK"
    }
}