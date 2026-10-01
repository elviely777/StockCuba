package cu.stockcuba.app.domain.model

data class VentaDiariaItem(
    val id: String,
    val fecha: Long,
    val productoId: String,
    val nombreProducto: String,
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
