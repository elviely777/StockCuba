package cu.stockcuba.app.di

import android.content.Context
import cu.stockcuba.app.data.security.SecurityRepositoryImpl
import cu.stockcuba.app.domain.security.SecurityRepository
import cu.stockcuba.app.presentation.security.BiometricAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository = impl

    @Provides
    @Singleton
    fun provideBiometricAuthenticator(@ApplicationContext context: Context): BiometricAuthenticator = 
        BiometricAuthenticator(context)
}
