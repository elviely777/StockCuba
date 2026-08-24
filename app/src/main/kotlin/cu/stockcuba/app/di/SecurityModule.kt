package cu.stockcuba.app.di

import cu.stockcuba.app.data.security.SecurityRepositoryImpl
import cu.stockcuba.app.domain.security.SecurityRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository = impl
}
