package cu.stockcuba.app.data.mapper

import cu.stockcuba.app.data.local.entity.ClienteEntity
import cu.stockcuba.app.domain.model.Cliente
import java.time.Instant

fun ClienteEntity.toDomain(): Cliente = Cliente(
    id = id,
    nombre = nombre,
    telefono = telefono,
    notas = notas
)

fun Cliente.toEntity(): ClienteEntity = ClienteEntity(
    id = id,
    nombre = nombre,
    telefono = telefono,
    notas = notas,
    fechaCreacion = Instant.now().toEpochMilli(),
    activo = true
)
