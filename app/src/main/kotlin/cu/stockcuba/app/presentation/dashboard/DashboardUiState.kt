package cu.stockcuba.app.presentation.dashboard

import cu.stockcuba.app.domain.model.CierreDiario
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.Moneda
import cu.stockcuba.app.domain.model.RolUsuario
import cu.stockcuba.app.domain.model.ProductInsight
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
        val rolActual: RolUsuario = RolUsuario.DUENO,
        val nombreVendedor: String = "",
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
        
        // Rentabilidad Real del periodo
        val totalGastos: Double = 0.0, // Costo de productos
        val totalGastosOperativos: Double = 0.0, // Luz, salarios, etc
        val totalPorCobrar: Double = 0.0,
        val gananciaReal: Double = 0.0,
        
        // Listas
        val listaProductosBajoStock: List<Producto>,
        val ventasRecientes: List<Venta> = emptyList(),
        val ventasSemanales: List<Pair<String, Double>> = emptyList(), // Para el gráfico [Dia -> Total]
        val listaInsights: List<ProductInsight> = emptyList(),
        val eficienciaVendedores: List<VentaRepository.EficienciaVendedor> = emptyList(),
        
        // Tendencias (Strings formateados)
        val tendenciaTotal: String = "—",
        val tendenciaVentas: String = "—",
        
        val ultimoCierre: CierreDiario? = null,
        val ultimoCierreMensual: cu.stockcuba.app.domain.model.CierreMensual? = null,
        val monedaBase: Moneda = Moneda.CUP,
        val isLoading: Boolean = false
    ) : DashboardUiState

    data object Loading : DashboardUiState

    data class Error(val message: String) : DashboardUiState

    companion object {
        val empty = Success(
            rolActual = RolUsuario.DUENO,
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
 * Extensiones para formateo de moneda dinámico
 */
fun Double.formatoMoneda(moneda: Moneda): String {
    return "%,.2f ${moneda.name}".format(java.util.Locale.US, this)
}

fun Double.formatoAuto(
    moneda: Moneda, 
    vinculado: Boolean, 
    tasa: Double = 1.0, 
    monedaBase: Moneda = Moneda.CUP
): String {
    return if (vinculado) {
        (this * tasa).formatoMoneda(monedaBase)
    } else {
        this.formatoMoneda(moneda)
    }
}

fun Int.formatoCantidad(): String {
    return "%,d".format(java.util.Locale.US, this)
}

// Retrocompatibilidad
fun Double.formatoCUP(): String = this.formatoMoneda(Moneda.CUP)
fun Double.formatoCUPEntero(): String = "%,.0f CUP".format(java.util.Locale.US, this)
