package cu.stockcuba.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cu.stockcuba.app.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoria: CategoriaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categorias: List<CategoriaEntity>)

    @Update
    suspend fun update(categoria: CategoriaEntity)

    @Query("DELETE FROM categorias WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM categorias")
    suspend fun deleteAll()

    @Query("SELECT * FROM categorias WHERE id = :id")
    fun getById(id: String): Flow<CategoriaEntity?>

    @Query("SELECT * FROM categorias WHERE id = :id")
    suspend fun getByIdSync(id: String): CategoriaEntity?

    @Query("SELECT * FROM categorias WHERE activo = 1 ORDER BY nombre ASC")
    fun getAllActive(): Flow<List<CategoriaEntity>>

    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun getAll(): Flow<List<CategoriaEntity>>

    @Query("SELECT COUNT(*) FROM categorias WHERE activo = 1")
    fun countActive(): Flow<Int>

    @Query("SELECT COUNT(*) FROM productos WHERE categoria_id = :categoriaId AND activo = 1")
    fun countProductosInCategoria(categoriaId: String): Flow<Int>
}