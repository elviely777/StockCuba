package cu.stockcuba.app.data.local.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        CierreDiarioEntity::class,
        CierreMensualEntity::class,
        GastoEntity::class,
        AbonoEntity::class
    ],
    version = 12,
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12)
    ],
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
    abstract fun gastoDao(): GastoDao
    abstract fun abonoDao(): AbonoDao

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
            cierreDao().deleteAllMensuales()
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

        /**
         * Migración manual desde versiones muy antiguas (1-6) a la versión base documentada (7).
         * Esto evita que el fallback destructivo borre los datos al importar backups viejos.
         */
        val MIGRATION_1_7 = object : Migration(1, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Crear tablas que no existían en v1 si es necesario
                db.execSQL("CREATE TABLE IF NOT EXISTS `clientes` (`id` TEXT NOT NULL, `nombre` TEXT NOT NULL, `ci` TEXT NOT NULL, `telefono` TEXT, `notas` TEXT, `fecha_creacion` INTEGER NOT NULL, `activo` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `movimientos_inventario` (`id` TEXT NOT NULL, `producto_id` TEXT NOT NULL, `tipo` TEXT NOT NULL, `cantidad` INTEGER NOT NULL, `fecha` INTEGER NOT NULL, `motivo` TEXT, `fecha_creacion` INTEGER NOT NULL, `sync_status` TEXT NOT NULL DEFAULT 'SYNCED', PRIMARY KEY(`id`), FOREIGN KEY(`producto_id`) REFERENCES `productos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `cierres_diarios` (`id` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `total_recaudado` REAL NOT NULL, `total_efectivo` REAL NOT NULL, `total_transferencia` REAL NOT NULL, `cantidad_ventas` INTEGER NOT NULL, `ipb` REAL NOT NULL, `ipc` REAL NOT NULL, `notas` TEXT NOT NULL, `fecha_creacion` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `cierres_mensuales` (`id` TEXT NOT NULL, `mes` INTEGER NOT NULL, `anio` INTEGER NOT NULL, `total_recaudado` REAL NOT NULL, `total_efectivo` REAL NOT NULL, `total_transferencia` REAL NOT NULL, `cantidad_ventas` INTEGER NOT NULL, `ipb` REAL NOT NULL, `ipc` REAL NOT NULL, `notas` TEXT NOT NULL, `fecha_cierre` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                
                // Índices necesarios para v7
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_movimientos_inventario_producto_id` ON `movimientos_inventario` (`producto_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cierres_diarios_fecha` ON `cierres_diarios` (`fecha`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cierres_mensuales_anio_mes` ON `cierres_mensuales` (`anio`, `mes`)")
            }
        }

        fun getInstance(context: Context): StockCubaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockCubaDatabase::class.java,
                    "stockcuba_db"
                )
                    .addMigrations(MIGRATION_1_7)
                    .fallbackToDestructiveMigration() 
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}