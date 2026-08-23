package cu.stockcuba.app.presentation.dashboard

import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.repository.VentaRepository

/**
 * Estado de UI para el Dashboard.
 * Inmutable, refleja exactamente lo que necesita la pantalla.
 */
sealed interface DashboardUiState {
    data class Success(
        val totalVendidoHoy: Double,
        val cantidadVentasHoy: Int,
        val productoMasVendido: VentaRepository.ProductoMasVendido?,
        val listaProductosBajoStock: List<Producto>,
        val tendenciaTotalVendido: String = "—",
        val tendenciaCantidadVentas: String = "—",
        val isLoading: Boolean = false
    ) : DashboardUiState

    data object Loading : DashboardUiState

    data class Error(val message: String) : DashboardUiState

    companion object {
        val empty = Success(
            totalVendidoHoy = 0.0,
            cantidadVentasHoy = 0,
            productoMasVendido = null,
            listaProductosBajoStock = emptyList(),
            isLoading = false
        )
    }
}

/**
 * Extensiones para formateo de moneda (CUP - Pesos Cubanos)
 */
fun Double.formatoCUP(): String {
    return "%,.0f CUP".format(java.util.Locale.US, this)
}

fun Int.formatoCantidad(): String {
    return "%,d".format(java.util.Locale.US, this)
}