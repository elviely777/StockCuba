package cu.stockcuba.app.data.mapper

import cu.stockcuba.app.data.local.entity.VentaEntity
import cu.stockcuba.app.data.local.entity.VentaItemEntity
import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.VentaItem
import java.time.Instant

fun VentaEntity.toDomain(): Venta = Venta(
    id = id,
    fecha = Instant.ofEpochMilli(fecha),
    total = total,
    metodoPago = MetodoPago.valueOf(metodoPago),
    items = emptyList(), // Se poblará aparte
    clienteId = clienteId
)

fun Venta.toEntity(): VentaEntity = VentaEntity(
    id = id,
    fecha = fecha.toEpochMilli(),
    total = total,
    metodoPago = metodoPago.name,
    clienteId = clienteId,
    fechaCreacion = Instant.now().toEpochMilli(),
    syncStatus = "SYNCED"
)

fun VentaItemEntity.toDomain(): VentaItem = VentaItem(
    id = id,
    ventaId = ventaId,
    productoId = productoId,
    nombreProducto = nombreProducto,
    cantidad = cantidad,
    precioUnitario = precioUnitario,
    subtotal = subtotal
)

fun VentaItem.toEntity(ventaId: String): VentaItemEntity = VentaItemEntity(
    id = id,
    ventaId = ventaId,
    productoId = productoId,
    nombreProducto = nombreProducto,
    cantidad = cantidad,
    precioUnitario = precioUnitario,
    subtotal = subtotal
)

fun Venta.toEntityWithItems(): Pair<VentaEntity, List<VentaItemEntity>> {
    val ventaEntity = VentaEntity(
        id = id,
        fecha = fecha.toEpochMilli(),
        total = total,
        metodoPago = metodoPago.name,
        clienteId = clienteId,
        fechaCreacion = Instant.now().toEpochMilli(),
        syncStatus = "SYNCED"
    )
    val itemsEntities = items.map { it.toEntity(id) }
    return Pair(ventaEntity, itemsEntities)
}
