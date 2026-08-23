package cu.stockcuba.app.data.remote.dto

import com.squareup.moshi.Json

/**
 * DTO para request de sincronización de ventas.
 * Se envía un lote de ventas no sincronizadas.
 */
data class SyncVentasRequest(
    @Json(name = "ventas")
    val ventas: List<VentaSyncDto>,

    @Json(name = "device_id")
    val deviceId: String,

    @Json(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * DTO de venta para sincronización.
 * Subconjunto de campos necesarios para el backend.
 */
data class VentaSyncDto(
    @Json(name = "id")
    val id: String,

    @Json(name = "fecha")
    val fecha: Long,

    @Json(name = "total")
    val total: Double,

    @Json(name = "metodo_pago")
    val metodoPago: String,

    @Json(name = "cliente_id")
    val clienteId: String?,

    @Json(name = "items")
    val items: List<VentaItemSyncDto>
)

/**
 * DTO de item de venta para sincronización.
 */
data class VentaItemSyncDto(
    @Json(name = "producto_id")
    val productoId: String,

    @Json(name = "nombre_producto")
    val nombreProducto: String,

    @Json(name = "cantidad")
    val cantidad: Int,

    @Json(name = "precio_unitario")
    val precioUnitario: Double,

    @Json(name = "subtotal")
    val subtotal: Double
)

/**
 * DTO de respuesta de sincronización de ventas.
 */
data class SyncVentasResponse(
    @Json(name = "sincronizadas")
    val sincronizadas: List<String>, // IDs de ventas sincronizadas exitosamente

    @Json(name = "errores")
    val errores: List<SyncErrorDto>?,

    @Json(name = "server_timestamp")
    val serverTimestamp: Long
)

/**
 * Error de sincronización individual.
 */
data class SyncErrorDto(
    @Json(name = "venta_id")
    val ventaId: String,

    @Json(name = "codigo")
    val codigo: String,

    @Json(name = "mensaje")
    val mensaje: String
)

/**
 * DTO de respuesta de sincronización de productos.
 */
data class SyncProductosResponse(
    @Json(name = "productos")
    val productos: List<ProductoSyncDto>,

    @Json(name = "server_timestamp")
    val serverTimestamp: Long,

    @Json(name = "version_catalogo")
    val versionCatalogo: Int
)

/**
 * DTO de producto para sincronización.
 */
data class ProductoSyncDto(
    @Json(name = "id")
    val id: String,

    @Json(name = "nombre")
    val nombre: String,

    @Json(name = "descripcion")
    val descripcion: String?,

    @Json(name = "precio_venta")
    val precioVenta: Double,

    @Json(name = "costo_unitario")
    val costoUnitario: Double,

    @Json(name = "stock_actual")
    val stockActual: Int,

    @Json(name = "stock_minimo")
    val stockMinimo: Int,

    @Json(name = "unidad_medida")
    val unidadMedida: String,

    @Json(name = "categoria_id")
    val categoriaId: String,

    @Json(name = "activo")
    val activo: Boolean
)