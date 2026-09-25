package cu.stockcuba.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import cu.stockcuba.app.data.local.dao.VentaDao
import cu.stockcuba.app.data.mapper.toDomain
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val ventaDao: VentaDao,
    private val ajustesDataStore: AjustesDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startSync() {
        scope.launch {
            // Solo sincronizar si está vinculado a un negocio
            combine(
                ajustesDataStore.businessId,
                ajustesDataStore.posId
            ) { bId, pId ->
                if (bId != null && pId != null) bId to pId else null
            }.collect { vinculacion ->
                if (vinculacion != null) {
                    val (businessId, posId) = vinculacion
                    observarVentasNoSincronizadas(businessId, posId)
                }
            }
        }
    }

    private suspend fun observarVentasNoSincronizadas(businessId: String, posId: String) {
        ventaDao.getVentasNoSincronizadasFlow().collect { ventasPendientes ->
            ventasPendientes.forEach { ventaEntity ->
                subirVentaAFirestore(businessId, posId, ventaEntity)
            }
        }
    }

    private suspend fun subirVentaAFirestore(businessId: String, posId: String, ventaEntity: cu.stockcuba.app.data.local.entity.VentaEntity) {
        try {
            val items = ventaDao.getItemsByVentaIdSync(ventaEntity.id).map { it.toDomain() }
            val venta = ventaEntity.toDomain().copy(items = items)

            val ventaData = mapOf(
                "id" to venta.id,
                "posId" to posId,
                "fecha" to venta.fecha.toEpochMilli(),
                "total" to venta.total,
                "metodoPago" to venta.metodoPago.name,
                "montoEfectivo" to venta.montoEfectivo,
                "montoTransferencia" to venta.montoTransferencia,
                "items" to venta.items.map { item ->
                    mapOf(
                        "productoId" to item.productoId,
                        "nombre" to item.nombreProducto,
                        "cantidad" to item.cantidad,
                        "precio" to item.precioUnitario,
                        "subtotal" to item.subtotal
                    )
                }
            )

            firestore.collection("businesses")
                .document(businessId)
                .collection("ventas")
                .document(venta.id)
                .set(ventaData)
                .await()

            // Marcar como sincronizada localmente
            ventaDao.marcarComoSincronizada(venta.id)
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSync", "Error subiendo venta ${ventaEntity.id}", e)
        }
    }
}
