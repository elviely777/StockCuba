package cu.stockcuba.app.domain.repository

import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface ProductoRepository {

    fun getAll(): Flow<List<Producto>>

    fun getById(id: String): Flow<Producto?>

    fun getByCategoria(categoriaId: String): Flow<List<Producto>>

    fun getProductosBajoStock(): Flow<List<Producto>>

    fun getSinStock(): Flow<List<Producto>>

    fun searchByName(query: String): Flow<List<Producto>>

    suspend fun getByIdSync(id: String): Result<Producto>

    suspend fun insert(producto: Producto): Result<Unit>

    suspend fun update(producto: Producto): Result<Unit>

    suspend fun deleteById(id: String): Result<Unit>

    suspend fun updateStock(productoId: String, cantidad: Int): Result<Int>

    suspend fun reserveStock(productoId: String, cantidad: Int): Result<Boolean>

    suspend fun insertAll(productos: List<Producto>): Result<Unit>
}