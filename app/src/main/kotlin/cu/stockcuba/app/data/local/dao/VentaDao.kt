package cu.stockcuba.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import cu.stockcuba.app.data.local.entity.VentaEntity
import cu.stockcuba.app.data.local.entity.VentaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venta: VentaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ventas: List<VentaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<VentaItemEntity>)

    @Query("SELECT * FROM ventas WHERE id = :id")
    fun getById(id: String): Flow<VentaEntity?>

    @Query("SELECT * FROM ventas WHERE id = :id")
    suspend fun getByIdSync(id: String): VentaEntity?

    @Query("SELECT * FROM ventas ORDER BY fecha DESC")
    fun getAll(): Flow<List<VentaEntity>>

    @Query("SELECT * FROM ventas WHERE fecha BETWEEN :startDate AND :endDate ORDER BY fecha DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<VentaEntity>>

    @Query("SELECT * FROM ventas WHERE cliente_id = :clienteId ORDER BY fecha DESC")
    fun getByCliente(clienteId: String): Flow<List<VentaEntity>>

    @Query("SELECT * FROM ventas WHERE metodo_pago = :metodoPago ORDER BY fecha DESC")
    fun getByMetodoPago(metodoPago: String): Flow<List<VentaEntity>>

    @Query("SELECT * FROM ventas ORDER BY fecha DESC LIMIT :limit OFFSET :offset")
    fun getPaginated(limit: Int, offset: Int): Flow<List<VentaEntity>>

    @Query("SELECT COUNT(*) FROM ventas")
    fun countAll(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ventas WHERE fecha BETWEEN :startDate AND :endDate")
    fun countByDateRange(startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT SUM(total) FROM ventas WHERE fecha BETWEEN :startDate AND :endDate")
    fun getTotalByDateRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT fecha, SUM(total) as total FROM ventas GROUP BY date(fecha/1000, 'unixepoch') ORDER BY fecha DESC")
    fun getDailyTotals(): Flow<List<DailyTotal>>

    @Query("SELECT fecha, SUM(total) as total FROM ventas WHERE fecha BETWEEN :startDate AND :endDate GROUP BY date(fecha/1000, 'unixepoch') ORDER BY fecha DESC")
    fun getDailyTotalsByRange(startDate: Long, endDate: Long): Flow<List<DailyTotal>>

    @Query("SELECT * FROM venta_items WHERE venta_id = :ventaId")
    fun getItemsByVentaId(ventaId: String): Flow<List<VentaItemEntity>>

    @Query("SELECT * FROM venta_items WHERE venta_id = :ventaId")
    suspend fun getItemsByVentaIdSync(ventaId: String): List<VentaItemEntity>

    @Query("SELECT vi.*, p.nombre as productoNombre FROM venta_items vi JOIN productos p ON vi.producto_id = p.id WHERE vi.venta_id = :ventaId")
    suspend fun getItemsWithProductName(ventaId: String): List<VentaItemWithProduct>

    @Transaction
    suspend fun insertVentaWithItems(venta: VentaEntity, items: List<VentaItemEntity>) {
        insert(venta)
        insertItems(items)
    }

    // Clear all methods for reset (T27)
    @Query("DELETE FROM venta_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM ventas")
    suspend fun deleteAll()

    // ===== SYNC METHODS =====

    @Query("SELECT * FROM ventas WHERE sync_status = 'PENDING' ORDER BY fecha ASC")
    suspend fun getVentasNoSincronizadas(): List<VentaEntity>

    @Query("UPDATE ventas SET sync_status = 'SYNCED' WHERE id IN (:ids)")
    suspend fun marcarComoSincronizadas(ids: List<String>): Int

    data class DailyTotal(
        val fecha: Long,
        val total: Double
    )

    data class VentaItemWithProduct(
        val id: String,
        @ColumnInfo(name = "venta_id")
        val ventaId: String,
        @ColumnInfo(name = "producto_id")
        val productoId: String,
        @ColumnInfo(name = "nombre_producto")
        val nombreProducto: String,
        val cantidad: Int,
        @ColumnInfo(name = "precio_unitario")
        val precioUnitario: Double,
        val subtotal: Double,
        val productoNombre: String
    )
}