# data/mapper/ — Funciones de Extensión Entity ↔ Domain ↔ DTO

Mapeo puro entre capas. **Sin lógica de negocio**, solo transformación de datos.

## Archivos esperados
- `EntityMapper.kt` — `Entity.toDomain()`, `DomainModel.toEntity()`
- `DtoMapper.kt` — `Dto.toDomain()`, `DomainModel.toDto()`
- `Mapper.kt` (barrel) — re-exporta todos

## Convenciones
- Funciones de extensión: `fun ProductEntity.toDomain(): Product`
- En `DomainModel.toEntity()`: generar IDs si faltan, poner timestamps
- En `Dto.toDomain()`: parsear fechas, normalizar enums, validar requeridos
- Testing: unit tests exhaustivos aquí (fácil, sin mocks)

## Ejemplo
```kotlin
// data/mapper/EntityMapper.kt
fun ProductEntity.toDomain(): Product = Product(
    id = id,
    name = name,
    price = price,
    stock = stock,
    createdAt = createdAt.toInstant()
)

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    name = name,
    price = price,
    stock = stock,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = System.currentTimeMillis(),
    syncStatus = SyncStatus.SYNCED
)
```