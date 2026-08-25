package cu.stockcuba.app.di

import android.content.Context
import androidx.room.Room
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import cu.stockcuba.app.data.local.dao.CategoriaDao
import cu.stockcuba.app.data.local.dao.CierreDao
import cu.stockcuba.app.data.local.dao.ClienteDao
import cu.stockcuba.app.data.local.dao.MovimientoInventarioDao
import cu.stockcuba.app.data.local.dao.ProductoDao
import cu.stockcuba.app.data.local.dao.VentaDao
import cu.stockcuba.app.data.local.database.StockCubaDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): StockCubaDatabase {
        return Room.databaseBuilder(
            context,
            StockCubaDatabase::class.java,
            "stockcuba_db"
        )
            .fallbackToDestructiveMigration() // v1 only, replace with migrations later
            .build()
    }

    @Provides
    @Singleton
    fun provideProductoDao(database: StockCubaDatabase): ProductoDao = database.productoDao()

    @Provides
    @Singleton
    fun provideCategoriaDao(database: StockCubaDatabase): CategoriaDao = database.categoriaDao()

    @Provides
    @Singleton
    fun provideVentaDao(database: StockCubaDatabase): VentaDao = database.ventaDao()

    @Provides
    @Singleton
    fun provideClienteDao(database: StockCubaDatabase): ClienteDao = database.clienteDao()

    @Provides
    @Singleton
    fun provideMovimientoInventarioDao(database: StockCubaDatabase): MovimientoInventarioDao =
        database.movimientoInventarioDao()

    @Provides
    @Singleton
    fun provideCierreDao(database: StockCubaDatabase): CierreDao = database.cierreDao()
}