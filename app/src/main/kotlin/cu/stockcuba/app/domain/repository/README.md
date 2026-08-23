# domain/repository/ — Contratos de Repositorio (Interfaces)

Interfaces que definen **qué** se puede hacer con los datos, sin decir **cómo**.

## Principios
- Definidas en `domain` → implementadas en `data.repository`
- Inyectadas en `domain.usecase` y `presentation.<feature>.ViewModel`
- Métodos `suspend` para one-shots; `Flow<T>` para streams reactivos
- Nombres: `NombreRepository` (p.ej. `ProductRepository`, `AuthRepository`)

## Ejemplo
```kotlin
interface ProductRepository {
    fun getProduct(id: String): Flow<Product?>
    fun getProducts(category: ProductCategory?): Flow<List<Product>>
    suspend fun getProductById(id: String): Result<Product>
    suspend fun syncProducts(): Result<Unit>
    suspend fun reserveStock(productId: String, quantity: Int): Result<Unit>
}
```

## Qué NO va aquí
- Detalles de implementación (Room, Retrofit, cache keys, etc.)
- Lógica de negocio compleja → eso va en `usecase`