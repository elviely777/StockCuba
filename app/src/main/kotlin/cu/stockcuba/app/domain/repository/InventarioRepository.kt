package cu.stockcuba.app.domain.repository

import cu.stockcuba.app.domain.model.MovimientoInventario
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface InventarioRepository {

    fun getHistorialPorProducto(productoId: String): Flow<List<MovimientoInventario>>

    fun getHistorialPorProductoYRango(productoId: String, desde: Long, hasta: Long): Flow<List<MovimientoInventario>>

    fun getHistorialPorRango(desde: Long, hasta: Long): Flow<List<MovimientoInventario>>

    suspend fun registrarMovimiento(movimiento: MovimientoInventario): Result<Unit>

    suspend fun registrarMovimientos(movimientos: List<MovimientoInventario>): Result<Unit>

    suspend fun getTotalEntradas(productoId: String): Result<Int>

    suspend fun getTotalSalidas(productoId: String): Result<Int>

    suspend fun getTotalAjustes(productoId: String): Result<Int>
}