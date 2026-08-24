package cu.stockcuba.app.domain.model

import java.time.Instant

data class Venta(
    val id: String,
    val fecha: Instant,
    val total: Double,
    val metodoPago: MetodoPago,
    val items: List<VentaItem>,
    val clienteId: String?,
    val montoEfectivo: Double = 0.0,
    val montoTransferencia: Double = 0.0
) {
    init {
        require(total >= 0) { "El total no puede ser negativo" }
    }

    val cantidadTotalItems: Int
        get() = items.sumOf { it.cantidad }

    val numeroItems: Int
        get() = items.size
}