package cu.stockcuba.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import cu.stockcuba.app.data.supabase.SupabaseSyncRepository
import javax.inject.Inject

@HiltAndroidApp
class StockCubaApplication : Application() {

    @Inject
    lateinit var supabaseSyncRepository: SupabaseSyncRepository

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("StockCubaApp", "Aplicación creada")
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                android.util.Log.d("StockCubaApp", "Iniciando sincronización en segundo plano")
                supabaseSyncRepository.startSync()
            }

            override fun onStop(owner: LifecycleOwner) {
                android.util.Log.d("StockCubaApp", "Deteniendo sincronización")
                supabaseSyncRepository.stopSync()
            }
        })
    }
}
