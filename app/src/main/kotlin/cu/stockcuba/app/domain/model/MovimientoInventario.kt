package cu.stockcuba.app.domain.model

import java.time.Instant

data class MovimientoInventario(
    val id: String,
    val productoId: String,
    val tipo: TipoMovimientoInventario,
    val cantidad: Int,
    val fecha: Instant,
    val motivo: String?
) {
    init {
        require(cantidad != 0) { "La cantidad del movimiento no puede ser 0" }
    }

    val esEntrada: Boolean
        get() = tipo == TipoMovimientoInventario.ENTRADA

    val esSalida: Boolean
        get() = tipo == TipoMovimientoInventario.SALIDA || tipo == TipoMovimientoInventario.VENTA

    val cantidadConSigno: Int
        get() = when (tipo) {
            TipoMovimientoInventario.ENTRADA -> cantidad
            TipoMovimientoInventario.SALIDA, TipoMovimientoInventario.VENTA -> -cantidad
            TipoMovimientoInventario.AJUSTE -> cantidad // puede ser positivo o negativo según motivo
        }
}