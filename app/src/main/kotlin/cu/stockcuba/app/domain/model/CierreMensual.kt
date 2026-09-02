package cu.stockcuba.app.domain.model

import java.time.Instant

data class CierreMensual(
    val id: String,
    val mes: Int,
    val anio: Int,
    val totalRecaudado: Double,
    val totalEfectivo: Double,
    val totalTransferencia: Double,
    val cantidadVentas: Int,
    val ipb: Double, // Inventario a precio de venta al cierre
    val ipc: Double, // Inventario a precio de costo al cierre
    val fechaCierre: Instant,
    val notas: String = ""
)
