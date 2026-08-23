package cu.stockcuba.app.di

import android.content.Context
import cu.stockcuba.app.data.feedback.FeedbackRepositoryImpl
import cu.stockcuba.app.domain.feedback.FeedbackRepository
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeedbackModule {

    @Provides
    @Singleton
    fun provideFeedbackRepository(
        @ApplicationContext context: Context,
        ajustesDataStore: AjustesDataStore
    ): FeedbackRepository = FeedbackRepositoryImpl(context, ajustesDataStore)
}
