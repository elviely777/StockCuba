package cu.stockcuba.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cu.stockcuba.app.data.local.entity.AbonoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AbonoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(abono: AbonoEntity)

    @Query("SELECT * FROM abonos WHERE cliente_id = :clienteId ORDER BY fecha DESC")
    fun getByCliente(clienteId: String): Flow<List<AbonoEntity>>

    @Query("SELECT * FROM abonos ORDER BY fecha DESC")
    fun getAll(): Flow<List<AbonoEntity>>

    @Query("SELECT SUM(monto) FROM abonos WHERE fecha BETWEEN :startDate AND :endDate")
    fun getTotalByDateRange(startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT * FROM abonos WHERE sync_status = 'PENDING' ORDER BY fecha ASC")
    fun getAbonosNoSincronizadosFlow(): Flow<List<AbonoEntity>>

    @Query("UPDATE abonos SET sync_status = 'SYNCED' WHERE id = :id")
    suspend fun marcarComoSincronizado(id: String): Int
}
