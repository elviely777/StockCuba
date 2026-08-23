package cu.stockcuba.app.domain.model

import java.time.Instant

data class Producto(
    val id: String,
    val nombre: String,
    val descripcion: String?,
    val precioVenta: Double,
    val costoUnitario: Double,
    val stockActual: Int,
    val stockMinimo: Int,
    val unidadMedida: UnidadMedida,
    val categoriaId: String,
    val fechaCreacion: Instant,
    val activo: Boolean = true
) {
    val margenGanancia: Double
        get() = if (costoUnitario > 0) ((precioVenta - costoUnitario) / costoUnitario) * 100 else 0.0

    val stockBajo: Boolean
        get() = stockActual <= stockMinimo

    val sinStock: Boolean
        get() = stockActual <= 0
}