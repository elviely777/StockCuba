package cu.stockcuba.app.di

import cu.stockcuba.app.data.repository.CategoriaRepositoryImpl
import cu.stockcuba.app.data.repository.CierreRepositoryImpl
import cu.stockcuba.app.data.repository.ClienteRepositoryImpl
import cu.stockcuba.app.data.repository.InventarioRepositoryImpl
import cu.stockcuba.app.data.repository.ProductoRepositoryImpl
import cu.stockcuba.app.data.repository.ReportRepositoryImpl
import cu.stockcuba.app.data.repository.VentaRepositoryImpl
import cu.stockcuba.app.domain.repository.BusinessRepository
import cu.stockcuba.app.domain.repository.CategoriaRepository
import cu.stockcuba.app.domain.repository.CierreRepository
import cu.stockcuba.app.domain.repository.ClienteRepository
import cu.stockcuba.app.domain.repository.InventarioRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import cu.stockcuba.app.domain.repository.ReportRepository
import cu.stockcuba.app.domain.repository.VentaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductoRepository(impl: ProductoRepositoryImpl): ProductoRepository

    @Binds
    @Singleton
    abstract fun bindCategoriaRepository(impl: CategoriaRepositoryImpl): CategoriaRepository

    @Binds
    @Singleton
    abstract fun bindVentaRepository(impl: VentaRepositoryImpl): VentaRepository

    @Binds
    @Singleton
    abstract fun bindClienteRepository(impl: ClienteRepositoryImpl): ClienteRepository

    @Binds
    @Singleton
    abstract fun bindInventarioRepository(impl: InventarioRepositoryImpl): InventarioRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    @Singleton
    abstract fun bindCierreRepository(impl: CierreRepositoryImpl): CierreRepository

    // BusinessRepository binding moved to SupabaseModule.kt (SupabaseBusinessRepository)
}