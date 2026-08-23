package cu.stockcuba.app.data.remote.api

import cu.stockcuba.app.data.remote.dto.SyncVentasRequest
import cu.stockcuba.app.data.remote.dto.SyncVentasResponse
import cu.stockcuba.app.data.remote.dto.SyncProductosResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Interfaz Retrofit para la API de StockCuba.
 * Endpoints de ejemplo para futura sincronización con backend.
 * Actualmente offline-first: Room es la fuente de verdad.
 */
interface StockCubaApi {

    /**
     * Sube ventas pendientes de sincronización al backend.
     * El backend debe ser idempotente (usar venta.id como clave única).
     */
    @POST("sync/ventas")
    suspend fun syncVentas(@Body request: SyncVentasRequest): Response<SyncVentasResponse>

    /**
     * Obtiene productos actualizados desde el backend.
     * Útil para sincronización inicial o catálogo compartido.
     */
    @GET("sync/productos")
    suspend fun syncProductos(): Response<SyncProductosResponse>

    /**
     * Health check del backend.
     */
    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>

    /**
     * Respuesta de health check.
     */
    data class HealthResponse(
        val status: String,
        val timestamp: Long,
        val version: String?
    )
}