package cu.stockcuba.app.data.mapper

import cu.stockcuba.app.data.local.entity.ProductoEntity
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.UnidadMedida
import java.time.Instant

fun ProductoEntity.toDomain(): Producto = Producto(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    precioVenta = precioVenta,
    costoUnitario = costoUnitario,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    unidadMedida = UnidadMedida.valueOf(unidadMedida),
    categoriaId = categoriaId,
    fechaCreacion = Instant.ofEpochMilli(fechaCreacion),
    activo = activo
)

fun Producto.toEntity(): ProductoEntity = ProductoEntity(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    precioVenta = precioVenta,
    costoUnitario = costoUnitario,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    unidadMedida = unidadMedida.name,
    categoriaId = categoriaId,
    fechaCreacion = fechaCreacion.toEpochMilli(),
    activo = activo,
    fechaActualizacion = Instant.now().toEpochMilli(),
    syncStatus = "SYNCED"
)

fun Producto.toEntityForUpdate(): ProductoEntity = ProductoEntity(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    precioVenta = precioVenta,
    costoUnitario = costoUnitario,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    unidadMedida = unidadMedida.name,
    categoriaId = categoriaId,
    fechaCreacion = fechaCreacion.toEpochMilli(),
    activo = activo,
    fechaActualizacion = Instant.now().toEpochMilli(),
    syncStatus = "PENDING"
)
