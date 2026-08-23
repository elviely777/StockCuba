package cu.stockcuba.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AjustesDataStoreEntryPoint {
    fun ajustesDataStore(): AjustesDataStore
}