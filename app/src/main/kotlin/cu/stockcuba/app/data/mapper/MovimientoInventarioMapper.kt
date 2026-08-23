package cu.stockcuba.app.data.mapper

import cu.stockcuba.app.data.local.entity.MovimientoInventarioEntity
import cu.stockcuba.app.domain.model.MovimientoInventario
import cu.stockcuba.app.domain.model.TipoMovimientoInventario
import java.time.Instant

fun MovimientoInventarioEntity.toDomain(): MovimientoInventario = MovimientoInventario(
    id = id,
    productoId = productoId,
    tipo = TipoMovimientoInventario.valueOf(tipo),
    cantidad = cantidad,
    fecha = Instant.ofEpochMilli(fecha),
    motivo = motivo
)

fun MovimientoInventario.toEntity(): MovimientoInventarioEntity = MovimientoInventarioEntity(
    id = id,
    productoId = productoId,
    tipo = tipo.name,
    cantidad = cantidad,
    fecha = fecha.toEpochMilli(),
    motivo = motivo,
    fechaCreacion = Instant.now().toEpochMilli(),
    syncStatus = "SYNCED"
)
