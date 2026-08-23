package cu.stockcuba.app.data.repository

import cu.stockcuba.app.data.local.dao.ClienteDao
import cu.stockcuba.app.data.mapper.*
import cu.stockcuba.app.domain.model.Cliente
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.ClienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClienteRepositoryImpl @Inject constructor(
    private val clienteDao: ClienteDao
) : ClienteRepository {

    override fun getAll(): Flow<List<Cliente>> = clienteDao.getAllActive().map { it.map { it.toDomain() } }

    override fun getActivos(): Flow<List<Cliente>> = clienteDao.getAllActive().map { it.map { it.toDomain() } }

    override fun getById(id: String): Flow<Cliente?> = clienteDao.getById(id).map { it?.toDomain() }

    override suspend fun getByIdSync(id: String): Result<Cliente> {
        return try {
            val entity = clienteDao.getByIdSync(id)
            entity?.let { Result.Success(it.toDomain()) } ?: Result.Failure(DomainError.NotFound("Cliente", id))
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun insert(cliente: Cliente): Result<Unit> {
        return try {
            clienteDao.insert(cliente.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun update(cliente: Cliente): Result<Unit> {
        return try {
            clienteDao.update(cliente.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun deleteById(id: String): Result<Unit> {
        return try {
            clienteDao.deleteById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override fun searchByName(query: String): Flow<List<Cliente>> =
        clienteDao.searchByName(query).map { it.map { it.toDomain() } }
}