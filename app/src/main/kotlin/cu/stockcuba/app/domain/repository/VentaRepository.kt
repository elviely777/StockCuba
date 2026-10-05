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

    suspend fun editarVenta(venta: Venta): Result<Unit>

    suspend fun getTotalVendidoPorRango(desde: Long, hasta: Long): Result<Double>

    suspend fun getResumenDelDia(fecha: Long): Result<ResumenDia>

    suspend fun getResumenAyer(): Result<ResumenDia>

    fun getEficienciaVendedores(desde: Long, hasta: Long): Flow<List<EficienciaVendedor>>

    /**
     * Obtiene totales diarios combinados (ventas reales + ventas históricas manuales)
     * para el rango de fechas especificado.
     * Retorna un Map<fecha, totalCombinado> donde totalCombinado = totalVentasReales + totalVentasManuales
     */
    fun getDailyTotalsBlended(desde: Long, hasta: Long): Flow<Map<Long, Double>>

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

    data class EficienciaVendedor(
        val nombre: String,
        val cantidadVentas: Int,
        val totalRecaudado: Double
    )
}