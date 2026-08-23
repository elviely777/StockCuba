package cu.stockcuba.app.data.repository

import cu.stockcuba.app.data.local.dao.MovimientoInventarioDao
import cu.stockcuba.app.data.mapper.*
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.MovimientoInventario
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.InventarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventarioRepositoryImpl @Inject constructor(
    private val movimientoDao: MovimientoInventarioDao
) : InventarioRepository {

    override fun getHistorialPorProducto(productoId: String): Flow<List<MovimientoInventario>> =
        movimientoDao.getByProducto(productoId).map { it.map { it.toDomain() } }

    override fun getHistorialPorProductoYRango(productoId: String, desde: Long, hasta: Long): Flow<List<MovimientoInventario>> =
        movimientoDao.getByProductoAndDateRange(productoId, desde, hasta).map { it.map { it.toDomain() } }

    override suspend fun registrarMovimiento(movimiento: MovimientoInventario): Result<Unit> {
        return try {
            movimientoDao.insert(movimiento.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun registrarMovimientos(movimientos: List<MovimientoInventario>): Result<Unit> {
        return try {
            movimientoDao.insertAll(movimientos.map { it.toEntity() })
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun getTotalEntradas(productoId: String): Result<Int> {
        return try {
            val total = movimientoDao.getTotalEntradas(productoId).first() ?: 0
            Result.Success(total)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun getTotalSalidas(productoId: String): Result<Int> {
        return try {
            val total = movimientoDao.getTotalSalidas(productoId).first() ?: 0
            Result.Success(total)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun getTotalAjustes(productoId: String): Result<Int> {
        return try {
            val total = movimientoDao.getTotalAjustes(productoId).first() ?: 0
            Result.Success(total)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }
}