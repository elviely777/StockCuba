package cu.stockcuba.app.domain.model

import java.time.Instant

data class VentaItem(
    val id: String,
    val ventaId: String,
    val productoId: String,
    val nombreProducto: String, // snapshot del nombre al momento de la venta
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
) {
    init {
        require(cantidad > 0) { "La cantidad debe ser mayor a 0" }
        require(precioUnitario >= 0) { "El precio unitario no puede ser negativo" }
        require(subtotal >= 0) { "El subtotal no puede ser negativo" }
    }
}