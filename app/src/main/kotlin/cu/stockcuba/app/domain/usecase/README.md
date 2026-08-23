# domain/usecase/ — Casos de Uso (Business Logic)

Una clase por **acción de negocio** concreta. Orquestan repositorios, aplican reglas, devuelven resultado.

## Principios
- **Single Responsibility**: un use case = una acción del usuario/negocio
- Nombres: verbo + sustantivo → `GetProductsUseCase`, `ReserveStockUseCase`, `LoginUseCase`
- `operator fun invoke(...): Result<T>` o `suspend fun execute(...): Result<T>`
- Inyectan `Repository` interfaces (no impls)
- **No** conocen Android, ViewModel, Compose, ni LiveData/Flow de UI
- Retornan `Result<T>` (sealed) o lanzan `DomainError` tipado

## Ejemplo
```kotlin
class ReserveStockUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(productId: String, quantity: Int): Result<Unit> {
        if (quantity <= 0) return Result.Failure(DomainError.InvalidQuantity)
        return productRepository.reserveStock(productId, quantity)
    }
}
```

## Testing
- Fácil: mock `Repository` interfaces, verifica llamadas y resultados
- Sin Robolectric, sin Android context