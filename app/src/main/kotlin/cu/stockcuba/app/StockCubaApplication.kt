package cu.stockcuba.app

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import cu.stockcuba.app.data.supabase.SupabaseSyncRepository
import javax.inject.Inject

@HiltAndroidApp
class StockCubaApplication : Application(), LifecycleObserver {

    @Inject
    lateinit var supabaseSyncRepository: SupabaseSyncRepository

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppStart() {
        supabaseSyncRepository.startSync()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppStop() {
        supabaseSyncRepository.stopSync()
    }
}