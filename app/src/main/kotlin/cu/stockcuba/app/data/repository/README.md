# data/repository/ — Implementaciones de Repositorio

Clases que implementan las interfaces de `domain.repository`.

## Responsabilidades
- Orquestar fuentes: `local` (Room), `remote` (Retrofit), `cache` (DataStore)
- Manejar estrategias: *network-first*, *cache-first*, *offline-first*, *sync*
- Mapear `Entity` ↔ `DomainModel` ↔ `Dto` usando `data.mapper`
- Exponer `Flow<>` o `suspend` según contrato del dominio

## Convenciones
- Nombres: `NombreRepositoryImpl` (p.ej. `ProductRepositoryImpl`)
- Inyectan DAOs, APIs, DataStore via constructor (Hilt)
- **No** contienen lógica de negocio compleja — delegar a `domain.usecase`
- Manejo de errores: `Result<T>`, `sealed class Resource<T>`, o excepciones de dominio

## Ejemplo
```kotlin
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao,
    private val productApi: ProductApi,
    private val mapper: ProductMapper
) : ProductRepository {
    override fun getProduct(id: String): Flow<Product> = productDao.getById(id)
        .map { mapper.toDomain(it) }

    override suspend fun syncProducts(): Result<Unit> = try {
        val dtos = productApi.getProducts(1, 50)
        productDao.insertAll(dtos.map { mapper.toEntity(it) })
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
```