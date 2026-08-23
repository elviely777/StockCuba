package cu.stockcuba.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cu.stockcuba.app.data.local.entity.MovimientoInventarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoInventarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movimiento: MovimientoInventarioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movimientos: List<MovimientoInventarioEntity>)

    @Query("SELECT * FROM movimientos_inventario WHERE id = :id")
    fun getById(id: String): Flow<MovimientoInventarioEntity?>

    @Query("SELECT * FROM movimientos_inventario WHERE producto_id = :productoId ORDER BY fecha DESC")
    fun getByProducto(productoId: String): Flow<List<MovimientoInventarioEntity>>

    @Query("SELECT * FROM movimientos_inventario WHERE producto_id = :productoId AND fecha BETWEEN :startDate AND :endDate ORDER BY fecha DESC")
    fun getByProductoAndDateRange(productoId: String, startDate: Long, endDate: Long): Flow<List<MovimientoInventarioEntity>>

    @Query("SELECT * FROM movimientos_inventario WHERE tipo = :tipo ORDER BY fecha DESC")
    fun getByTipo(tipo: String): Flow<List<MovimientoInventarioEntity>>

    @Query("SELECT * FROM movimientos_inventario ORDER BY fecha DESC LIMIT :limit OFFSET :offset")
    fun getPaginated(limit: Int, offset: Int): Flow<List<MovimientoInventarioEntity>>

    @Query("SELECT COUNT(*) FROM movimientos_inventario")
    fun countAll(): Flow<Int>

    @Query("SELECT SUM(cantidad) FROM movimientos_inventario WHERE producto_id = :productoId AND tipo = 'ENTRADA'")
    fun getTotalEntradas(productoId: String): Flow<Int?>

    @Query("SELECT SUM(cantidad) FROM movimientos_inventario WHERE producto_id = :productoId AND tipo IN ('SALIDA', 'VENTA')")
    fun getTotalSalidas(productoId: String): Flow<Int?>

    @Query("SELECT SUM(cantidad) FROM movimientos_inventario WHERE producto_id = :productoId AND tipo = 'AJUSTE'")
    fun getTotalAjustes(productoId: String): Flow<Int?>

    // Clear all method for reset (T27)
    @Query("DELETE FROM movimientos_inventario")
    suspend fun deleteAll()
}