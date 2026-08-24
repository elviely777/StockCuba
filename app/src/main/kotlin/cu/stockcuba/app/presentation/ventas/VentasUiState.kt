package cu.stockcuba.app.presentation.ventas

import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.VentaItem
import cu.stockcuba.app.domain.repository.VentaRepository

/**
 * Estado de UI para Nueva Venta (POS).
 */
sealed interface NuevaVentaUiState {
    data class Editing(
        val productosDisponibles: List<Producto> = emptyList(),
        val carrito: List<CarritoItem> = emptyList(),
        val query: String = "",
        val metodoPago: MetodoPago = MetodoPago.EFECTIVO,
        val efectivoRecibido: String = "",
        val transferenciaMonto: String = "",
        val clienteId: String? = null,
        val clientes: List<ClienteSimple> = emptyList(),
        val isLoading: Boolean = false,
        val errors: Map<String, String> = emptyMap(),
        val showSuccess: Boolean = false,
        val showNuevoClienteDialog: Boolean = false,
        val editingClienteId: String? = null, // null = creando, not null = editando
        val nuevoClienteNombre: String = "",
        val nuevoClienteCI: String = "",
        val nuevoClienteTelefono: String = ""
    ) : NuevaVentaUiState

    data object Saving : NuevaVentaUiState

    data class Saved(val ventaId: String) : NuevaVentaUiState

    data class Error(val message: String) : NuevaVentaUiState

    companion object {
        val empty = Editing()
    }
}

/**
 * Item en el carrito de venta.
 */
data class CarritoItem(
    val producto: Producto,
    var cantidad: Int = 1
) {
    val subtotal: Double
        get() = precioUnitario * cantidad

    val precioUnitario: Double
        get() = producto.precioVenta

    val stockDisponible: Int
        get() = producto.stockActual

    val puedeAumentar: Boolean
        get() = cantidad < stockDisponible

    val stockStatusColor: androidx.compose.ui.graphics.Color
        get() = when {
            stockDisponible <= 0 -> cu.stockcuba.app.presentation.theme.StockCubaColors.CoralAlerta
            stockDisponible <= producto.stockMinimo -> cu.stockcuba.app.presentation.theme.StockCubaColors.CoralAlerta
            else -> cu.stockcuba.app.presentation.theme.StockCubaColors.VerdeExito
        }
}

/**
 * Cliente simplificado para selector.
 */
data class ClienteSimple(
    val id: String,
    val nombre: String,
    val telefono: String?
)

/**
 * Totales calculados del carrito.
 */
data class CarritoTotales(
    val subtotal: Double,
    val total: Double
) {
    companion object {
        fun calcular(carrito: List<CarritoItem>): CarritoTotales {
            val subtotal = carrito.sumOf { it.subtotal }
            return CarritoTotales(subtotal = subtotal, total = subtotal)
        }
    }
}

/**
 * Estado para Historial de Ventas.
 */
sealed interface HistorialVentasUiState {
    data class Success(
        val ventasPorDia: List<VentasPorDia>,
        val isLoading: Boolean = false
    ) : HistorialVentasUiState

    data object Loading : HistorialVentasUiState

    data class Error(val message: String) : HistorialVentasUiState

    companion object {
        val empty = Success(ventasPorDia = emptyList())
    }
}

/**
 * Ventas agrupadas por día con información de clientes mapeada.
 */
data class VentasPorDia(
    val fecha: Long,
    val ventas: List<VentaUi>,
    val totalDia: Double
) {
    val cantidadVentas: Int
        get() = ventas.size

    val fechaFormateada: String
        get() = java.time.Instant.ofEpochMilli(fecha)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", java.util.Locale.getDefault()))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
}

/**
 * Representación de venta para la UI con nombre de cliente.
 */
data class VentaUi(
    val venta: Venta,
    val clienteNombre: String? = null
)

/**
 * Estado para Detalle de Venta.
 */
sealed interface DetalleVentaUiState {
    data object Loading : DetalleVentaUiState
    data class Success(
        val venta: Venta,
        val cliente: cu.stockcuba.app.domain.model.Cliente? = null
    ) : DetalleVentaUiState
    data class Error(val message: String) : DetalleVentaUiState
}

/**
 * Detalle de venta expandido.
 */
data class VentaDetalleExpandido(
    val venta: Venta,
    val items: List<VentaItem>,
    var isExpanded: Boolean = false
)