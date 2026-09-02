package cu.stockcuba.app.domain.repository

import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface BusinessRepository {
    val isVinculado: Flow<Boolean>
    val businessId: Flow<String?>
    val posId: Flow<String?>
    val estadoNegocio: Flow<String>
    
    suspend fun vincular(businessId: String, posNombre: String): Result<Unit>
    suspend fun desvincular(): Result<Unit>
    suspend fun verificarEstadoRemoto(): Result<String>
    fun getFacturacionEstimada(mes: Int, anio: Int): Flow<Double>
}
