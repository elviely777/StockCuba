package cu.stockcuba.app.data.repository

import cu.stockcuba.app.data.local.dao.GastoDao
import cu.stockcuba.app.data.mapper.toDomain
import cu.stockcuba.app.data.mapper.toEntity
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Gasto
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.GastoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GastoRepositoryImpl @Inject constructor(
    private val gastoDao: GastoDao
) : GastoRepository {

    override fun getAll(): Flow<List<Gasto>> = 
        gastoDao.getAll().map { it.map { it.toDomain() } }

    override fun getGastosPorRango(desde: Long, hasta: Long): Flow<List<Gasto>> =
        gastoDao.getByDateRange(desde, hasta).map { it.map { it.toDomain() } }

    override suspend fun registrarGasto(gasto: Gasto): Result<Unit> {
        return try {
            gastoDao.insert(gasto.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun eliminarGasto(gasto: Gasto): Result<Unit> {
        return try {
            gastoDao.delete(gasto.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun getTotalGastosPorRango(desde: Long, hasta: Long): Result<Double> {
        return try {
            val total = gastoDao.getTotalByDateRange(desde, hasta).first() ?: 0.0
            Result.Success(total)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }
}
