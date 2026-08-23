# domain/model/ — Modelos de Dominio (Pure Data Classes)

**Núcleo de la app**. Clases de datos Kotlin puras, **sin anotaciones** de Room, Retrofit, Moshi, ni framework alguno.

## Reglas de oro
- `data class` inmutables (`val` only)
- Solo tipos del stdlib: `String`, `Int`, `Long`, `Double`, `Boolean`, `Instant`, `List<T>`, `Optional<T>`
- Sin `lateinit`, sin `null` salvo que el dominio lo permita realmente
- `equals()`/`hashCode()`/`toString()` automáticos por `data class`
- `sealed class` / `sealed interface` para estados, resultados, eventos

## Ejemplos
```kotlin
// domain/model/Product.kt
data class Product(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double,
    val stock: Int,
    val category: ProductCategory,
    val createdAt: Instant,
    val updatedAt: Instant
)

// domain/model/Result.kt
sealed interface Result<out T> {
    data class Success<out T>(val value: T) : Result<T>
    data class Failure(val error: DomainError) : Result<Nothing>
}
```

## Ubicación
- Un archivo por modelo principal
- `sealed` interfaces en su propio archivo (p.ej. `DomainError.kt`, `UiEvent.kt`)