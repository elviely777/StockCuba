package cu.stockcuba.app.domain.repository

import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface CategoriaRepository {

    fun getAll(): Flow<List<Categoria>>

    fun getActivas(): Flow<List<Categoria>>

    fun getById(id: String): Flow<Categoria?>

    suspend fun getByIdSync(id: String): Result<Categoria>

    suspend fun insert(categoria: Categoria): Result<Unit>

    suspend fun update(categoria: Categoria): Result<Unit>

    suspend fun deleteById(id: String): Result<Unit>

    fun countProductosInCategoria(categoriaId: String): Flow<Int>
}