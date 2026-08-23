package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.VentaRepository
import javax.inject.Inject

class ObtenerResumenDelDiaUseCase @Inject constructor(
    private val ventaRepository: VentaRepository
) {

    suspend operator fun invoke(fecha: Long = System.currentTimeMillis()): Result<VentaRepository.ResumenDia> {
        return ventaRepository.getResumenDelDia(fecha)
    }
}