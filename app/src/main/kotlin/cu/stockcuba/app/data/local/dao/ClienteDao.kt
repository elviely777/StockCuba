package cu.stockcuba.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cu.stockcuba.app.data.local.entity.ClienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cliente: ClienteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clientes: List<ClienteEntity>)

    @Update
    suspend fun update(cliente: ClienteEntity)

    @Query("DELETE FROM clientes WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM clientes")
    suspend fun deleteAll()

    @Query("SELECT * FROM clientes WHERE id = :id")
    fun getById(id: String): Flow<ClienteEntity?>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun getByIdSync(id: String): ClienteEntity?

    @Query("SELECT * FROM clientes WHERE activo = 1 ORDER BY nombre ASC")
    fun getAllActive(): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes WHERE nombre LIKE '%' || :query || '%' AND activo = 1 ORDER BY nombre ASC LIMIT 20")
    fun searchByName(query: String): Flow<List<ClienteEntity>>

    @Query("SELECT COUNT(*) FROM clientes WHERE activo = 1")
    fun countActive(): Flow<Int>
}