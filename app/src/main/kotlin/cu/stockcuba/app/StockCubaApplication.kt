package cu.stockcuba.app

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class StockCubaApplication : Application(), LifecycleObserver {

    @Inject
    lateinit var syncRepository: cu.stockcuba.app.data.repository.SyncRepository

    override fun onCreate() {
        super.onCreate()
        // Observar el ciclo de vida del proceso para iniciar/detener sync
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppStart() {
        syncRepository.startSyncObserver()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppStop() {
        syncRepository.stopSyncObserver()
    }
}