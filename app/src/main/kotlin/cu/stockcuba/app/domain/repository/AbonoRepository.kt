package cu.stockcuba.app.domain.repository

import cu.stockcuba.app.domain.model.Abono
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.flow.Flow

interface AbonoRepository {
    fun getByCliente(clienteId: String): Flow<List<Abono>>
    fun getAll(): Flow<List<Abono>>
    suspend fun registrarAbono(abono: Abono): Result<Unit>
    suspend fun getTotalAbonosPorRango(desde: Long, hasta: Long): Result<Double>
}
