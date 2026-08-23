package cu.stockcuba.app.domain.repository

import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.VentaItem
import kotlinx.coroutines.flow.Flow

interface VentaRepository {

    fun getAll(): Flow<List<Venta>>

    fun getById(id: String): Flow<Venta?>

    fun getVentasPorRango(desde: Long, hasta: Long): Flow<List<Venta>>

    fun getVentasDeHoy(): Flow<List<Venta>>

    fun getByCliente(clienteId: String): Flow<List<Venta>>

    suspend fun getByIdSync(id: String): Result<Venta>

    suspend fun getItemsByVentaId(ventaId: String): Result<List<VentaItem>>

    suspend fun registrarVenta(venta: Venta): Result<Unit>

    suspend fun getTotalVendidoPorRango(desde: Long, hasta: Long): Result<Double>

    suspend fun getResumenDelDia(fecha: Long): Result<ResumenDia>

    data class ResumenDia(
        val fecha: Long,
        val totalVendido: Double,
        val cantidadVentas: Int,
        val productoMasVendido: ProductoMasVendido?
    )

    data class ProductoMasVendido(
        val productoId: String,
        val nombreProducto: String,
        val cantidadTotal: Int,
        val totalVendido: Double
    )
}