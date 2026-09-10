package cu.stockcuba.app.data.repository

import cu.stockcuba.app.data.local.dao.AbonoDao
import cu.stockcuba.app.data.local.database.StockCubaDatabase
import cu.stockcuba.app.data.mapper.toDomain
import cu.stockcuba.app.data.mapper.toEntity
import cu.stockcuba.app.domain.model.Abono
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.AbonoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AbonoRepositoryImpl @Inject constructor(
    private val abonoDao: AbonoDao,
    private val database: StockCubaDatabase
) : AbonoRepository {

    override fun getByCliente(clienteId: String): Flow<List<Abono>> =
        abonoDao.getByCliente(clienteId).map { it.map { it.toDomain() } }

    override fun getAll(): Flow<List<Abono>> =
        abonoDao.getAll().map { it.map { it.toDomain() } }

    override suspend fun registrarAbono(abono: Abono): Result<Unit> {
        return try {
            database.withTransaction {
                // 1. Insertar el abono
                abonoDao.insert(abono.toEntity())
                
                // 2. Restar el monto de la deuda del cliente (monto negativo para restar)
                database.clienteDao().updateDeuda(abono.clienteId, -abono.monto)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun getTotalAbonosPorRango(desde: Long, hasta: Long): Result<Double> {
        return try {
            val total = abonoDao.getTotalByDateRange(desde, hasta).first() ?: 0.0
            Result.Success(total)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }
}
