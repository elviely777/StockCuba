package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.repository.VentaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerVentasDeHoyUseCase @Inject constructor(
    private val ventaRepository: VentaRepository
) {

    operator fun invoke(): Flow<List<Venta>> = ventaRepository.getVentasDeHoy()
}