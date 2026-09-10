package cu.stockcuba.app.domain.repository

import cu.stockcuba.app.domain.model.Gasto
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface GastoRepository {
    fun getAll(): Flow<List<Gasto>>
    fun getGastosPorRango(desde: Long, hasta: Long): Flow<List<Gasto>>
    suspend fun registrarGasto(gasto: Gasto): Result<Unit>
    suspend fun eliminarGasto(gasto: Gasto): Result<Unit>
    suspend fun getTotalGastosPorRango(desde: Long, hasta: Long): Result<Double>
}
