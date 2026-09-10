package cu.stockcuba.app.data.mapper

import cu.stockcuba.app.data.local.entity.ClienteEntity
import cu.stockcuba.app.domain.model.Cliente
import java.time.Instant

fun ClienteEntity.toDomain(): Cliente = Cliente(
    id = id,
    nombre = nombre,
    ci = ci,
    telefono = telefono,
    notas = notas,
    saldoDeuda = saldoDeuda
)

fun Cliente.toEntity(): ClienteEntity = ClienteEntity(
    id = id,
    nombre = nombre,
    ci = ci,
    telefono = telefono,
    notas = notas,
    saldoDeuda = saldoDeuda,
    fechaCreacion = Instant.now().toEpochMilli(),
    activo = true
)
