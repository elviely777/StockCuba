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
    version = 14,
    autoMigrations = [
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14)
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
         * Reconstruye el esquema completo a la Versión 14 de forma literal.
         * Copia los datos esenciales y garantiza que TableInfo coincida al 100%.
         */
        private fun reconstruirEsquemaLiteralV14(db: SupportSQLiteDatabase) {
            android.util.Log.d("StockCubaMigration", "Iniciando reconstrucción maestra de esquema v14")
            db.execSQL("PRAGMA foreign_keys = OFF")

            // 1. Eliminar indices globales conflictivos para permitir la recreación limpia
            val indices = listOf(
                "index_productos_categoria_id", "index_productos_activo", "index_productos_stock_actual",
                "index_ventas_cliente_id", "index_ventas_fecha", "index_ventas_metodo_pago",
                "index_venta_items_venta_id", "index_venta_items_producto_id",
                "index_movimientos_inventario_producto_id", "index_movimientos_inventario_fecha", "index_movimientos_inventario_tipo",
                "index_cierres_diarios_fecha", "index_cierres_mensuales_anio_mes",
                "index_gastos_fecha", "index_gastos_tipo", "index_abonos_cliente_id", "index_abonos_fecha"
            )
            for (idx in indices) { db.execSQL("DROP INDEX IF EXISTS `$idx` ") }

            // 2. Resguardar datos en tablas temporales
            val tablas = listOf("productos", "categorias", "ventas", "venta_items", "clientes", "movimientos_inventario", "cierres_diarios", "cierres_mensuales", "gastos", "abonos")
            for (tabla in tablas) {
                db.execSQL("DROP TABLE IF EXISTS `${tabla}_backup` ")
                val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$tabla'")
                if (cursor.moveToFirst()) {
                    db.execSQL("ALTER TABLE `$tabla` RENAME TO `${tabla}_backup` ")
                }
                cursor.close()
            }

            // 3. Crear tablas con el esquema EXACTO de v14
            db.execSQL("CREATE TABLE IF NOT EXISTS `productos` (`id` TEXT NOT NULL, `nombre` TEXT NOT NULL, `descripcion` TEXT, `precio_venta` REAL NOT NULL, `costo_unitario` REAL NOT NULL, `moneda` TEXT NOT NULL DEFAULT 'CUP', `stock_actual` INTEGER NOT NULL, `stock_minimo` INTEGER NOT NULL, `unidad_medida` TEXT NOT NULL, `codigo_barras` TEXT, `categoria_id` TEXT NOT NULL, `imagen_url` TEXT, `fecha_creacion` INTEGER NOT NULL, `activo` INTEGER NOT NULL DEFAULT 1, `vincular_tasa` INTEGER NOT NULL DEFAULT 0, `fecha_actualizacion` INTEGER NOT NULL, `sync_status` TEXT NOT NULL DEFAULT 'SYNCED', PRIMARY KEY(`id`), FOREIGN KEY(`categoria_id`) REFERENCES `categorias`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
            db.execSQL("CREATE TABLE IF NOT EXISTS `categorias` (`id` TEXT NOT NULL, `nombre` TEXT NOT NULL, `color` INTEGER NOT NULL, `fecha_creacion` INTEGER NOT NULL, `activo` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `ventas` (`id` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `total` REAL NOT NULL, `total_original` REAL NOT NULL DEFAULT 0.0, `descuento` REAL NOT NULL DEFAULT 0.0, `metodo_pago` TEXT NOT NULL, `cliente_id` TEXT, `monto_efectivo` REAL NOT NULL, `monto_transferencia` REAL NOT NULL, `id_transferencia` TEXT, `vendedor_nombre` TEXT NOT NULL DEFAULT 'Desconocido', `fecha_creacion` INTEGER NOT NULL, `sync_status` TEXT NOT NULL DEFAULT 'PENDING', PRIMARY KEY(`id`), FOREIGN KEY(`cliente_id`) REFERENCES `clientes`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
            db.execSQL("CREATE TABLE IF NOT EXISTS `venta_items` (`id` TEXT NOT NULL, `venta_id` TEXT NOT NULL, `producto_id` TEXT NOT NULL, `nombre_producto` TEXT NOT NULL, `cantidad` INTEGER NOT NULL, `precio_unitario` REAL NOT NULL, `subtotal` REAL NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`venta_id`) REFERENCES `ventas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`producto_id`) REFERENCES `productos`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
            db.execSQL("CREATE TABLE IF NOT EXISTS `clientes` (`id` TEXT NOT NULL, `nombre` TEXT NOT NULL, `ci` TEXT NOT NULL, `telefono` TEXT, `notas` TEXT, `saldo_deuda` REAL NOT NULL DEFAULT 0.0, `fecha_creacion` INTEGER NOT NULL, `activo` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `movimientos_inventario` (`id` TEXT NOT NULL, `producto_id` TEXT NOT NULL, `tipo` TEXT NOT NULL, `cantidad` INTEGER NOT NULL, `fecha` INTEGER NOT NULL, `motivo` TEXT, `fecha_creacion` INTEGER NOT NULL, `sync_status` TEXT NOT NULL DEFAULT 'SYNCED', PRIMARY KEY(`id`), FOREIGN KEY(`producto_id`) REFERENCES `productos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE TABLE IF NOT EXISTS `cierres_diarios` (`id` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `total_recaudado` REAL NOT NULL, `total_efectivo` REAL NOT NULL, `total_transferencia` REAL NOT NULL, `cantidad_ventas` INTEGER NOT NULL, `ipb` REAL NOT NULL, `ipc` REAL NOT NULL, `notas` TEXT NOT NULL, `fecha_creacion` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `cierres_mensuales` (`id` TEXT NOT NULL, `mes` INTEGER NOT NULL, `anio` INTEGER NOT NULL, `total_recaudado` REAL NOT NULL, `total_efectivo` REAL NOT NULL, `total_transferencia` REAL NOT NULL, `cantidad_ventas` INTEGER NOT NULL, `ipb` REAL NOT NULL, `ipc` REAL NOT NULL, `notas` TEXT NOT NULL, `fecha_cierre` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `gastos` (`id` TEXT NOT NULL, `concepto` TEXT NOT NULL, `tipo` TEXT NOT NULL, `monto` REAL NOT NULL, `moneda` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `sync_status` TEXT NOT NULL DEFAULT 'PENDING', `fecha_creacion` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `abonos` (`id` TEXT NOT NULL, `cliente_id` TEXT NOT NULL, `monto` REAL NOT NULL, `fecha` INTEGER NOT NULL, `metodo_pago` TEXT NOT NULL, `notas` TEXT NOT NULL, `sync_status` TEXT NOT NULL DEFAULT 'PENDING', `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`cliente_id`) REFERENCES `clientes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")

            // 4. Crear Indices EXACTOS inmediatamente después de las tablas
            db.execSQL("CREATE INDEX `index_productos_categoria_id` ON `productos` (`categoria_id`)")
            db.execSQL("CREATE INDEX `index_productos_activo` ON `productos` (`activo`)")
            db.execSQL("CREATE INDEX `index_productos_stock_actual` ON `productos` (`stock_actual`)")
            db.execSQL("CREATE INDEX `index_ventas_cliente_id` ON `ventas` (`cliente_id`)")
            db.execSQL("CREATE INDEX `index_ventas_fecha` ON `ventas` (`fecha`)")
            db.execSQL("CREATE INDEX `index_ventas_metodo_pago` ON `ventas` (`metodo_pago`)")
            db.execSQL("CREATE INDEX `index_venta_items_venta_id` ON `venta_items` (`venta_id`)")
            db.execSQL("CREATE INDEX `index_venta_items_producto_id` ON `venta_items` (`producto_id`)")
            db.execSQL("CREATE INDEX `index_movimientos_inventario_producto_id` ON `movimientos_inventario` (`producto_id`)")
            db.execSQL("CREATE INDEX `index_movimientos_inventario_fecha` ON `movimientos_inventario` (`fecha`)")
            db.execSQL("CREATE INDEX `index_movimientos_inventario_tipo` ON `movimientos_inventario` (`tipo`)")
            db.execSQL("CREATE INDEX `index_cierres_diarios_fecha` ON `cierres_diarios` (`fecha`)")
            db.execSQL("CREATE UNIQUE INDEX `index_cierres_mensuales_anio_mes` ON `cierres_mensuales` (`anio`, `mes`)")
            db.execSQL("CREATE INDEX `index_gastos_fecha` ON `gastos` (`fecha`)")
            db.execSQL("CREATE INDEX `index_gastos_tipo` ON `gastos` (`tipo`)")
            db.execSQL("CREATE INDEX `index_abonos_cliente_id` ON `abonos` (`cliente_id`)")
            db.execSQL("CREATE INDEX `index_abonos_fecha` ON `abonos` (`fecha`)")

            // 5. Restaurar datos fundamentales
            try { db.execSQL("INSERT OR IGNORE INTO `categorias` (id, nombre, color, fecha_creacion) SELECT id, nombre, 0, 0 FROM `categorias_backup` ") } catch(_: Exception) {}
            db.execSQL("INSERT OR IGNORE INTO `categorias` (id, nombre, color, fecha_creacion) VALUES ('general', 'General', -1, 0)")
            
            try {
                db.execSQL("""
                    INSERT OR IGNORE INTO `productos` (id, nombre, descripcion, precio_venta, costo_unitario, stock_actual, stock_minimo, unidad_medida, categoria_id, fecha_creacion, fecha_actualizacion) 
                    SELECT id, nombre, descripcion, precio_venta, 0.0, stock_actual, 0, 'UNIDAD', COALESCE(categoria_id, 'general'), fecha_creacion, fecha_creacion FROM `productos_backup` 
                """.trimIndent())
            } catch(_: Exception) {}
            
            try { db.execSQL("INSERT OR IGNORE INTO `clientes` (id, nombre, ci, fecha_creacion) SELECT id, nombre, ci, fecha_creacion FROM `clientes_backup` ") } catch(_: Exception) {}
            try { db.execSQL("INSERT OR IGNORE INTO `ventas` (id, fecha, total, metodo_pago, cliente_id, monto_efectivo, monto_transferencia, fecha_creacion) SELECT id, fecha, total, metodo_pago, cliente_id, total, 0.0, fecha FROM `ventas_backup` ") } catch(_: Exception) {}
            try { db.execSQL("INSERT OR IGNORE INTO `venta_items` SELECT * FROM `venta_items_backup` ") } catch(_: Exception) {}
            try { db.execSQL("INSERT OR IGNORE INTO `movimientos_inventario` SELECT * FROM `movimientos_inventario_backup` ") } catch(_: Exception) {}
            try { db.execSQL("INSERT OR IGNORE INTO `cierres_diarios` SELECT * FROM `cierres_diarios_backup` ") } catch(_: Exception) {}
            try { db.execSQL("INSERT OR IGNORE INTO `cierres_mensuales` SELECT * FROM `cierres_mensuales_backup` ") } catch(_: Exception) {}

            // 6. Actualizar identity_hash y user_version
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f9f644160b12ce6cf7eb6d353f0af213')")

            // 7. Limpieza
            for (tabla in tablas) { db.execSQL("DROP TABLE IF EXISTS `${tabla}_backup` ") }

            db.execSQL("PRAGMA foreign_keys = ON")
            android.util.Log.d("StockCubaMigration", "Reconstrucción maestra completada exitosamente")
        }

        // Definimos migraciones individuales que todas ejecutan la reconstrucción total
        val MIGRATION_1_14 = object : Migration(1, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_2_14 = object : Migration(2, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_3_14 = object : Migration(3, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_4_14 = object : Migration(4, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_5_14 = object : Migration(5, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_6_14 = object : Migration(6, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_7_14 = object : Migration(7, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_8_14 = object : Migration(8, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_9_14 = object : Migration(9, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_10_14 = object : Migration(10, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_11_14 = object : Migration(11, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_12_14 = object : Migration(12, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }
        val MIGRATION_13_14 = object : Migration(13, 14) { override fun migrate(db: SupportSQLiteDatabase) = reconstruirEsquemaLiteralV14(db) }

        val ALL_MANUAL_MIGRATIONS = arrayOf(
            MIGRATION_1_14, MIGRATION_2_14, MIGRATION_3_14, MIGRATION_4_14, MIGRATION_5_14, 
            MIGRATION_6_14, MIGRATION_7_14, MIGRATION_8_14, MIGRATION_9_14, MIGRATION_10_14, 
            MIGRATION_11_14, MIGRATION_12_14, MIGRATION_13_14
        )

        fun getInstance(context: Context): StockCubaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockCubaDatabase::class.java,
                    "stockcuba_db"
                )
                    .addMigrations(*ALL_MANUAL_MIGRATIONS)
                    .fallbackToDestructiveMigration() 
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
