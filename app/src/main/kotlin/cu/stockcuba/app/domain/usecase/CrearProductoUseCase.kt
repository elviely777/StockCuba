package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.ProductoRepository
import javax.inject.Inject

class CrearProductoUseCase @Inject constructor(
    private val productoRepository: ProductoRepository
) {

    suspend operator fun invoke(producto: Producto): Result<Unit> {
        return productoRepository.insert(producto)
    }
}