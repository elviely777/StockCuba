package cu.stockcuba.app.data.mapper

import cu.stockcuba.app.data.local.entity.AbonoEntity
import cu.stockcuba.app.domain.model.Abono
import cu.stockcuba.app.domain.model.MetodoPago
import java.time.Instant

fun AbonoEntity.toDomain(): Abono = Abono(
    id = id,
    clienteId = clienteId,
    monto = monto,
    fecha = Instant.ofEpochMilli(fecha),
    metodoPago = MetodoPago.valueOf(metodoPago),
    notas = notas
)

fun Abono.toEntity(): AbonoEntity = AbonoEntity(
    id = id,
    clienteId = clienteId,
    monto = monto,
    fecha = fecha.toEpochMilli(),
    metodoPago = metodoPago.name,
    notas = notas
)
