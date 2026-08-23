package cu.stockcuba.app.data.repository

import cu.stockcuba.app.data.local.dao.CategoriaDao
import cu.stockcuba.app.data.mapper.*
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.CategoriaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoriaRepositoryImpl @Inject constructor(
    private val categoriaDao: CategoriaDao
) : CategoriaRepository {

    override fun getAll(): Flow<List<Categoria>> = categoriaDao.getAll().map { it.map { it.toDomain() } }

    override fun getActivas(): Flow<List<Categoria>> = categoriaDao.getAllActive().map { it.map { it.toDomain() } }

    override fun getById(id: String): Flow<Categoria?> = categoriaDao.getById(id).map { it?.toDomain() }

    override suspend fun getByIdSync(id: String): Result<Categoria> {
        return try {
            val entity = categoriaDao.getByIdSync(id)
            entity?.let { Result.Success(it.toDomain()) } ?: Result.Failure(DomainError.NotFound("Categoria", id))
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun insert(categoria: Categoria): Result<Unit> {
        return try {
            categoriaDao.insert(categoria.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun update(categoria: Categoria): Result<Unit> {
        return try {
            categoriaDao.update(categoria.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun deleteById(id: String): Result<Unit> {
        return try {
            categoriaDao.deleteById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override fun countProductosInCategoria(categoriaId: String): Flow<Int> =
        categoriaDao.countProductosInCategoria(categoriaId)
}