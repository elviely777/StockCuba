package cu.stockcuba.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cu.stockcuba.app.data.local.entity.GastoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gasto: GastoEntity)

    @Delete
    suspend fun delete(gasto: GastoEntity)

    @Query("SELECT * FROM gastos ORDER BY fecha DESC")
    fun getAll(): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gastos WHERE fecha BETWEEN :startDate AND :endDate ORDER BY fecha DESC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<GastoEntity>>

    @Query("SELECT SUM(monto) FROM gastos WHERE fecha BETWEEN :startDate AND :endDate")
    fun getTotalByDateRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT * FROM gastos WHERE sync_status = 'PENDING' ORDER BY fecha ASC")
    fun getGastosNoSincronizadosFlow(): Flow<List<GastoEntity>>

    @Query("UPDATE gastos SET sync_status = 'SYNCED' WHERE id = :id")
    suspend fun marcarComoSincronizado(id: String): Int

    @Query("DELETE FROM gastos")
    suspend fun deleteAll()
}
