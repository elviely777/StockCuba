package cu.stockcuba.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cu.stockcuba.app.data.local.dao.CategoriaDao
import cu.stockcuba.app.data.local.dao.ClienteDao
import cu.stockcuba.app.data.local.dao.MovimientoInventarioDao
import cu.stockcuba.app.data.local.dao.ProductoDao
import cu.stockcuba.app.data.local.dao.VentaDao
import cu.stockcuba.app.data.local.entity.CategoriaEntity
import cu.stockcuba.app.data.local.entity.ClienteEntity
import cu.stockcuba.app.data.local.entity.Converters
import cu.stockcuba.app.data.local.entity.MovimientoInventarioEntity
import cu.stockcuba.app.data.local.entity.ProductoEntity
import cu.stockcuba.app.data.local.entity.VentaEntity
import cu.stockcuba.app.data.local.entity.VentaItemEntity
import java.io.File

@Database(
    entities = [
        ProductoEntity::class,
        CategoriaEntity::class,
        VentaEntity::class,
        VentaItemEntity::class,
        ClienteEntity::class,
        MovimientoInventarioEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class StockCubaDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun ventaDao(): VentaDao
    abstract fun clienteDao(): ClienteDao
    abstract fun movimientoInventarioDao(): MovimientoInventarioDao

    /**
     * Clears all tables in the database, respecting foreign key order.
     * Also deletes WAL and SHM files if they exist (T27).
     */
    override fun clearAllTables() {
        // Disable foreign key checks temporarily to allow truncation in any order
        // Room's clearAllTables() handles FK order automatically, but we ensure it
        runInTransaction {
            // Delete in reverse dependency order to avoid FK violations
            // MovimientoInventario -> VentaItem -> Venta -> Producto -> Cliente -> Categoria
            // Use blocking calls for DAOs that are suspend
            // Note: This is a simplified approach - in production, consider using a coroutine
            try {
                kotlinx.coroutines.runBlocking {
                    movimientoInventarioDao().deleteAll()
                    ventaDao().deleteAllItems()
                    ventaDao().deleteAll()
                    productoDao().deleteAll()
                    clienteDao().deleteAll()
                    categoriaDao().deleteAll()
                }
            } catch (e: Exception) {
                // Log error but continue
                android.util.Log.e("StockCubaDatabase", "Error clearing tables", e)
            }
        }
        
        // Delete WAL and SHM files
        val dbFile = File(openHelper.writableDatabase.path)
        val walFile = File("${dbFile.absolutePath}-wal")
        val shmFile = File("${dbFile.absolutePath}-shm")
        walFile.delete()
        shmFile.delete()
    }

    companion object {
        @Volatile
        private var INSTANCE: StockCubaDatabase? = null

        fun getInstance(context: Context): StockCubaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockCubaDatabase::class.java,
                    "stockcuba_db"
                )
                    .fallbackToDestructiveMigration() // v1 only, replace with migrations later
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}