package cu.stockcuba.app.data.mapper

import cu.stockcuba.app.data.local.entity.CategoriaEntity
import cu.stockcuba.app.domain.model.Categoria
import java.time.Instant

fun CategoriaEntity.toDomain(): Categoria = Categoria(
    id = id,
    nombre = nombre,
    color = color
)

fun Categoria.toEntity(): CategoriaEntity = CategoriaEntity(
    id = id,
    nombre = nombre,
    color = color,
    fechaCreacion = Instant.now().toEpochMilli(),
    activo = true
)
