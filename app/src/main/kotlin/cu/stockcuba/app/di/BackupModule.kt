package cu.stockcuba.app.di

import android.content.Context
import cu.stockcuba.app.data.backup.BackupRepository
import cu.stockcuba.app.data.backup.BackupRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    @Singleton
    fun provideBackupRepository(@ApplicationContext context: Context): BackupRepository = BackupRepositoryImpl(context)
}
