package cu.stockcuba.app.domain.repository

import cu.stockcuba.app.domain.model.Cliente
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface ClienteRepository {

    fun getAll(): Flow<List<Cliente>>

    fun getActivos(): Flow<List<Cliente>>

    fun getById(id: String): Flow<Cliente?>

    suspend fun getByIdSync(id: String): Result<Cliente>

    suspend fun insert(cliente: Cliente): Result<Unit>

    suspend fun update(cliente: Cliente): Result<Unit>

    suspend fun deleteById(id: String): Result<Unit>

    fun searchByName(query: String): Flow<List<Cliente>>
}