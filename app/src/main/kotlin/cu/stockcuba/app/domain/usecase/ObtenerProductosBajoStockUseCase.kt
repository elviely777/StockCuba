package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObtenerProductosBajoStockUseCase @Inject constructor(
    private val productoRepository: ProductoRepository
) {

    operator fun invoke(): Flow<List<Producto>> = productoRepository.getProductosBajoStock()
}