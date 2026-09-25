package cu.stockcuba.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.BusinessRepository
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseBusinessRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val ajustesDataStore: AjustesDataStore,
    private val ventaRepository: cu.stockcuba.app.domain.repository.VentaRepository
) : BusinessRepository {

    override val isVinculado: Flow<Boolean> = ajustesDataStore.isVinculado
    override val businessId: Flow<String?> = ajustesDataStore.businessId
    override val posId: Flow<String?> = ajustesDataStore.posId

    override suspend fun vincular(businessId: String, posNombre: String): Result<Unit> {
        return try {
            val newPosId = UUID.randomUUID().toString()
            
            // 1. Registrar el POS en Firestore para que el dueño sepa qué dispositivos tiene
            val posData = mapOf(
                "id" to newPosId,
                "nombre" to posNombre,
                "fechaVinculacion" to System.currentTimeMillis()
            )
            
            firestore.collection("businesses")
                .document(businessId)
                .collection("puntos_venta")
                .document(newPosId)
                .set(posData)
                .await()

            // 2. Guardar localmente
            ajustesDataStore.guardarVinculacion(businessId, newPosId, posNombre)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(cu.stockcuba.app.domain.model.DomainError.NetworkError(e))
        }
    }

    override suspend fun desvincular(): Result<Unit> {
        return ajustesDataStore.desvincular()
    }

    override suspend fun getFacturacionEstimada(mes: Int, anio: Int): Flow<Double> {
        // Implementación básica: cuota fija 500 + 3% de las ventas locales del mes (hasta 20k)
        // Esto se mostrará en el dashboard.
        
        // Obtenemos el rango del mes
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, anio)
            set(java.util.Calendar.MONTH, mes)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(java.util.Calendar.MONTH, 1)
        val end = calendar.timeInMillis - 1

        return kotlinx.coroutines.flow.map(ventaRepository.getVentasPorRango(start, end)) { ventas ->
            val totalMes = ventas.sumOf { it.total }
            val variable = minOf(totalMes * 0.03, 20000.0)
            500.0 + variable
        }
    }
}
