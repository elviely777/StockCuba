# data/remote/api/ — Interfaces Retrofit

Definiciones de endpoints HTTP. Anotadas con `@GET`, `@POST`, etc.

## Convenciones
- Una interfaz por dominio de API (p.ej. `ProductApi`, `AuthApi`, `SyncApi`)
- Métodos `suspend` + `Response<T>` o resultado directo según estrategia de error
- `@Headers` comunes en la interfaz; específicos por método si varían
- Paths relativos a `baseUrl` configurada en `NetworkModule`

## Ejemplo
```kotlin
interface ProductApi {
    @GET("products")
    suspend fun getProducts(@Query("page") page: Int, @Query("limit") limit: Int): List<ProductDto>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: String): ProductDto
}
```