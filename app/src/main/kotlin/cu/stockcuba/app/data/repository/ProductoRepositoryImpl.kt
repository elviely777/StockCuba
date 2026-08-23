package cu.stockcuba.app.data.repository

import cu.stockcuba.app.data.local.dao.ProductoDao
import cu.stockcuba.app.data.mapper.*
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductoRepositoryImpl @Inject constructor(
    private val productoDao: ProductoDao
) : ProductoRepository {

    override fun getAll(): Flow<List<Producto>> = productoDao.getAllActive().map { it.map { it.toDomain() } }

    override fun getById(id: String): Flow<Producto?> = productoDao.getById(id).map { it?.toDomain() }

    override fun getByCategoria(categoriaId: String): Flow<List<Producto>> =
        productoDao.getByCategoria(categoriaId).map { it.map { it.toDomain() } }

    override fun getProductosBajoStock(): Flow<List<Producto>> =
        productoDao.getLowStock().map { it.map { it.toDomain() } }

    override fun getSinStock(): Flow<List<Producto>> =
        productoDao.getOutOfStock().map { it.map { it.toDomain() } }

    override fun searchByName(query: String): Flow<List<Producto>> =
        productoDao.searchByName(query).map { it.map { it.toDomain() } }

    override suspend fun getByIdSync(id: String): Result<Producto> {
        return try {
            val entity = productoDao.getByIdSync(id)
            entity?.let { Result.Success(it.toDomain()) } ?: Result.Failure(DomainError.NotFound("Producto", id))
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun insert(producto: Producto): Result<Unit> {
        return try {
            productoDao.insert(producto.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun update(producto: Producto): Result<Unit> {
        return try {
            productoDao.update(producto.toEntityForUpdate())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun deleteById(id: String): Result<Unit> {
        return try {
            productoDao.deleteById(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun updateStock(productoId: String, cantidad: Int): Result<Int> {
        return try {
            val updated = productoDao.updateStock(productoId, cantidad)
            Result.Success(updated)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun reserveStock(productoId: String, cantidad: Int): Result<Boolean> {
        return try {
            val success = productoDao.reserveStock(productoId, cantidad)
            Result.Success(success)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun insertAll(productos: List<Producto>): Result<Unit> {
        return try {
            productoDao.insertAll(productos.map { it.toEntity() })
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }
}