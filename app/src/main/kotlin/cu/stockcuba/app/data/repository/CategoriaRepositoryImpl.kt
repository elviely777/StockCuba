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

    override suspend fun prepopular(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val count = categoriaDao.getByIdSync("otros")
            if (count == null) {
                val categoriasDefecto = listOf(
                    Categoria("alimentos", "Alimentos", 0xFFFF9800.toInt()),
                    Categoria("bebidas", "Bebidas", 0xFF2196F3.toInt()),
                    Categoria("limpieza", "Limpieza e Higiene", 0xFFF44336.toInt()),
                    Categoria("hogar", "Hogar y Construcción", 0xFF795548.toInt()),
                    Categoria("electronica", "Electrónica", 0xFF9C27B0.toInt()),
                    Categoria("ferreteria", "Ferretería", 0xFF607D8B.toInt()),
                    Categoria("ropa", "Ropa y Calzado", 0xFFE91E63.toInt()),
                    Categoria("papeleria", "Papelería", 0xFF4CAF50.toInt()),
                    Categoria("otros", "Otros", 0xFF9E9E9E.toInt())
                )
                categoriaDao.insertAll(categoriasDefecto.map { it.toEntity() })
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }
}