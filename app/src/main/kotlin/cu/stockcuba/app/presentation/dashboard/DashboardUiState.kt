package cu.stockcuba.app.presentation.dashboard

import cu.stockcuba.app.domain.model.CierreDiario
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.Moneda
import cu.stockcuba.app.domain.repository.VentaRepository

/**
 * Rangos de tiempo para el Dashboard.
 */
enum class DashboardTimeRange {
    HOY, SEMANA, MES
}

/**
 * Estado de UI para el Dashboard.
 * Inmutable, refleja exactamente lo que necesita la pantalla.
 */
sealed interface DashboardUiState {
    data class Success(
        val timeRange: DashboardTimeRange = DashboardTimeRange.HOY,
        val totalVendido: Double,
        val cantidadVentas: Int,
        val ticketPromedio: Double,
        val productoMasVendido: VentaRepository.ProductoMasVendido?,
        
        // Desglose de dinero
        val montoEfectivo: Double = 0.0,
        val montoTransferencia: Double = 0.0,
        
        // Metas (respecto a ayer o periodo anterior)
        val metaVenta: Double = 0.0, // El valor de referencia (ej. ayer)
        val progresoMeta: Float = 0f, // 0.0 a 1.0 (o más)

        // IPB e IPC (T66)
        val valorInventarioVenta: Double = 0.0, // IPB
        val valorInventarioCosto: Double = 0.0, // IPC
        val gananciaProyectada: Double = 0.0,
        
        // Listas
        val listaProductosBajoStock: List<Producto>,
        val ventasRecientes: List<Venta> = emptyList(),
        
        // Tendencias (Strings formateados)
        val tendenciaTotal: String = "—",
        val tendenciaVentas: String = "—",
        
        val ultimoCierre: CierreDiario? = null,
        val facturacionEstimada: Double = 0.0,
        val isLoading: Boolean = false
    ) : DashboardUiState

    data object Loading : DashboardUiState

    data class Error(val message: String) : DashboardUiState

    companion object {
        val empty = Success(
            totalVendido = 0.0,
            cantidadVentas = 0,
            ticketPromedio = 0.0,
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
    return "%,.2f CUP".format(java.util.Locale.US, this)
}

fun Double.formatoCUPEntero(): String {
    return "%,.0f CUP".format(java.util.Locale.US, this)
}

fun Int.formatoCantidad(): String {
    return "%,d".format(java.util.Locale.US, this)
}
