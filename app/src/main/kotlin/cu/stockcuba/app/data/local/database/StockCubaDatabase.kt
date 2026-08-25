package cu.stockcuba.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import cu.stockcuba.app.data.local.dao.*
import cu.stockcuba.app.data.local.entity.*
import java.io.File

@Database(
    entities = [
        ProductoEntity::class,
        CategoriaEntity::class,
        VentaEntity::class,
        VentaItemEntity::class,
        ClienteEntity::class,
        MovimientoInventarioEntity::class,
        CierreDiarioEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class StockCubaDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun ventaDao(): VentaDao
    abstract fun clienteDao(): ClienteDao
    abstract fun movimientoInventarioDao(): MovimientoInventarioDao
    abstract fun cierreDao(): CierreDao

    /**
     * Clears all operation tables in the database, respecting foreign key order (T27).
     * This is a suspend function to be called from a coroutine.
     */
    suspend fun reiniciarBaseDatos() {
        val database = this
        database.withTransaction {
            // Delete in reverse dependency order to avoid FK violations
            // MovimientoInventario -> CierreDiario -> VentaItem -> Venta -> Producto -> Cliente -> Categoria
            movimientoInventarioDao().deleteAll()
            cierreDao().deleteAll()
            ventaDao().deleteAllItems()
            ventaDao().deleteAll()
            productoDao().deleteAll()
            clienteDao().deleteAll()
            categoriaDao().deleteAll()
        }
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