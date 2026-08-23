package cu.stockcuba.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cu.stockcuba.app.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(producto: ProductoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(productos: List<ProductoEntity>)

    @Update
    suspend fun update(producto: ProductoEntity)

    @Update
    suspend fun updateAll(productos: List<ProductoEntity>)

    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM productos")
    suspend fun deleteAll()

    @Query("SELECT * FROM productos WHERE id = :id")
    fun getById(id: String): Flow<ProductoEntity?>

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun getByIdSync(id: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE activo = 1 ORDER BY nombre ASC")
    fun getAllActive(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE activo = 1 AND categoria_id = :categoriaId ORDER BY nombre ASC")
    fun getByCategoria(categoriaId: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE activo = 1 AND stock_actual <= stock_minimo ORDER BY stock_actual ASC")
    fun getLowStock(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE activo = 1 AND stock_actual <= 0")
    fun getOutOfStock(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE nombre LIKE '%' || :query || '%' AND activo = 1 ORDER BY nombre ASC LIMIT 20")
    fun searchByName(query: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE activo = 1 ORDER BY fecha_creacion DESC LIMIT :limit OFFSET :offset")
    fun getPaginated(limit: Int, offset: Int): Flow<List<ProductoEntity>>

    @Query("SELECT COUNT(*) FROM productos WHERE activo = 1")
    fun countActive(): Flow<Int>

    @Query("SELECT COUNT(*) FROM productos WHERE activo = 1 AND stock_actual <= stock_minimo")
    fun countLowStock(): Flow<Int>

    @Query("UPDATE productos SET stock_actual = stock_actual + :cantidad, fecha_actualizacion = :now WHERE id = :productoId")
    suspend fun updateStockInternal(productoId: String, cantidad: Int, now: Long): Int

    @Transaction
    suspend fun updateStock(productoId: String, cantidad: Int): Int {
        return updateStockInternal(productoId, cantidad, System.currentTimeMillis())
    }

    @Query("UPDATE productos SET stock_actual = stock_actual - :cantidad, fecha_actualizacion = :now WHERE id = :productoId AND stock_actual >= :cantidad")
    suspend fun reserveStockInternal(productoId: String, cantidad: Int, now: Long): Int

    @Transaction
    suspend fun reserveStock(productoId: String, cantidad: Int): Boolean {
        return reserveStockInternal(productoId, cantidad, System.currentTimeMillis()) > 0
    }
}