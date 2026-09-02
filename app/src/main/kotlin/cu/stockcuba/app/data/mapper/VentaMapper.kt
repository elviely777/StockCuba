package cu.stockcuba.app.data.mapper

import cu.stockcuba.app.data.local.entity.VentaEntity
import cu.stockcuba.app.data.local.entity.VentaItemEntity
import cu.stockcuba.app.data.local.entity.VentaWithItems
import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.VentaItem
import java.time.Instant

fun VentaEntity.toDomain(): Venta = Venta(
    id = id,
    fecha = Instant.ofEpochMilli(fecha),
    total = total,
    metodoPago = MetodoPago.valueOf(metodoPago),
    items = emptyList(), 
    clienteId = clienteId,
    montoEfectivo = montoEfectivo,
    montoTransferencia = montoTransferencia,
    idTransferencia = idTransferencia
)

fun VentaWithItems.toDomain(): Venta = Venta(
    id = venta.id,
    fecha = Instant.ofEpochMilli(venta.fecha),
    total = venta.total,
    metodoPago = MetodoPago.valueOf(venta.metodoPago),
    items = items.map { it.toDomain() },
    clienteId = venta.clienteId,
    montoEfectivo = venta.montoEfectivo,
    montoTransferencia = venta.montoTransferencia,
    idTransferencia = venta.idTransferencia
)

fun Venta.toEntity(): VentaEntity = VentaEntity(
    id = id,
    fecha = fecha.toEpochMilli(),
    total = total,
    metodoPago = metodoPago.name,
    clienteId = clienteId,
    montoEfectivo = montoEfectivo,
    montoTransferencia = montoTransferencia,
    idTransferencia = idTransferencia,
    fechaCreacion = Instant.now().toEpochMilli(),
    syncStatus = "PENDING"
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
        montoEfectivo = montoEfectivo,
        montoTransferencia = montoTransferencia,
        idTransferencia = idTransferencia,
        fechaCreacion = Instant.now().toEpochMilli(),
        syncStatus = "PENDING"
    )
    val itemsEntities = items.map { it.toEntity(id) }
    return Pair(ventaEntity, itemsEntities)
}
