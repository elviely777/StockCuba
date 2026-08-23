package cu.stockcuba.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import cu.stockcuba.app.data.local.dao.VentaDao
import cu.stockcuba.app.data.local.entity.VentaEntity
import cu.stockcuba.app.data.mapper.toDomain
import cu.stockcuba.app.data.remote.api.StockCubaApi
import cu.stockcuba.app.data.remote.dto.SyncVentasRequest
import cu.stockcuba.app.data.remote.dto.VentaSyncDto
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio de sincronización offline-first.
 * Sube ventas pendientes cuando hay conectividad.
 * No bloquea la app si no hay backend configurado.
 */
@Singleton
class SyncRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val ventaDao: VentaDao,
    private val api: StockCubaApi
) : LifecycleObserver {

    private val syncJob = SupervisorJob()
    private val syncScope = CoroutineScope(Dispatchers.IO + syncJob)
    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null
    private var isSyncing = false
    private val SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5 minutos

    /**
     * Inicia el observador de conectividad.
     * Debe llamarse desde onStart del Application/Activity principal.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun startSyncObserver() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: android.net.Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    // Hay internet validado, intentar sincronizar
                    triggerSync()
                }
            }

            override fun onLost(network: android.net.Network) {
                super.onLost(network)
                // Se perdió conectividad, cancelar sync en curso
                cancelCurrentSync()
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, connectivityCallback!!)

        // También programar sync periódica
        schedulePeriodicSync()
    }

    /**
     * Detiene el observador de conectividad.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stopSyncObserver() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        connectivityCallback = null
        syncJob.cancel()
    }

    /**
     * Dispara sincronización inmediata de forma no bloqueante.
     */
    fun triggerSync() {
        if (isSyncing) return

        syncScope.launch {
            isSyncing = true
            try {
                sincronizarVentasPendientes()
            } finally {
                isSyncing = false
            }
        }
    }

    /**
     * Sincroniza ventas marcadas como no sincronizadas.
     * Usa la transacción atómica de Room vía DAO.
     */
    private suspend fun sincronizarVentasPendientes(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Obtener ventas con sync_status = 'PENDING'
            val ventasPendientes = ventaDao.getVentasNoSincronizadas()

            if (ventasPendientes.isEmpty()) {
                return@withContext Result.Success(Unit)
            }

            // Convertir a DTOs
            val ventasDto = ventasPendientes.map { ventaEntity ->
                val items = ventaDao.getItemsByVentaIdSync(ventaEntity.id)
                    .map { it.toDomain() }
                ventaEntity.toDomain().copy(items = items)
            }.map { venta ->
                VentaSyncDto(
                    id = venta.id,
                    fecha = venta.fecha.toEpochMilli(),
                    total = venta.total,
                    metodoPago = venta.metodoPago.name,
                    clienteId = venta.clienteId,
                    items = venta.items.map { item ->
                        cu.stockcuba.app.data.remote.dto.VentaItemSyncDto(
                            productoId = item.productoId,
                            nombreProducto = item.nombreProducto,
                            cantidad = item.cantidad,
                            precioUnitario = item.precioUnitario,
                            subtotal = item.subtotal
                        )
                    }
                )
            }

            val request = SyncVentasRequest(
                ventas = ventasDto,
                deviceId = obtenerDeviceId(),
                timestamp = System.currentTimeMillis()
            )

            // Llamar API
            val response = api.syncVentas(request)

            if (response.isSuccessful) {
                response.body()?.let { syncResponse ->
                    // Marcar como sincronizadas las que el backend confirmó
                    val idsSincronizados = syncResponse.sincronizadas
                    if (idsSincronizados.isNotEmpty()) {
                        ventaDao.marcarComoSincronizadas(idsSincronizados)
                    }

                    // Manejar errores parciales
                    syncResponse.errores?.forEach { error ->
                        // Loggear error específico, no bloquear las demás
                        android.util.Log.w("SyncRepository", "Error sincronizando venta ${error.ventaId}: ${error.codigo} - ${error.mensaje}")
                    }
                }
                Result.Success(Unit)
            } else {
                // Error HTTP - reintentar después
                Result.Failure(DomainError.NetworkError(Exception("HTTP ${response.code()}: ${response.message()}")))
            }
        } catch (e: Exception) {
            // Error de red, timeout, etc. - no crashea la app
            Result.Failure(DomainError.NetworkError(e))
        }
    }

    /**
     * Programa sincronización periódica cada 5 minutos.
     */
    private fun schedulePeriodicSync() {
        syncScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(SYNC_INTERVAL_MS)
                if (!isSyncing) {
                    triggerSync()
                }
            }
        }
    }

    /**
     * Cancela sincronización en curso.
     */
    private fun cancelCurrentSync() {
        if (isSyncing) {
            syncJob.cancel()
            isSyncing = false
        }
    }

    /**
     * Obtiene un ID único del dispositivo para identificación en el backend.
     */
    private fun obtenerDeviceId(): String {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        return prefs.getString("device_id", "") ?: run {
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }

    /**
     * Fuerza sincronización manual (ej. botón "Sincronizar ahora" en UI).
     */
    suspend fun forzarSincronizacion(): Result<Unit> = withContext(Dispatchers.IO) {
        sincronizarVentasPendientes()
    }
}