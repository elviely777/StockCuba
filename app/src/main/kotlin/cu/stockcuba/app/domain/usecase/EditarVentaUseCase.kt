package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.repository.VentaRepository
import javax.inject.Inject

class EditarVentaUseCase @Inject constructor(
    private val ventaRepository: VentaRepository
) {
    suspend operator fun invoke(venta: Venta): Result<Unit> {
        return ventaRepository.editarVenta(venta)
    }
}
