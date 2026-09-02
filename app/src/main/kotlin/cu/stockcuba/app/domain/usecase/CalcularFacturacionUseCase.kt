package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.repository.BusinessRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Calcula la facturación mensual estimada del negocio.
 * Lógica: 500 CUP (fijo) + 3% de las ventas (variable, tope 20,000 CUP).
 */
class CalcularFacturacionUseCase @Inject constructor(
    private val businessRepository: BusinessRepository
) {
    operator fun invoke(mes: Int, anio: Int): Flow<Double> {
        return businessRepository.getFacturacionEstimada(mes, anio)
    }
}
