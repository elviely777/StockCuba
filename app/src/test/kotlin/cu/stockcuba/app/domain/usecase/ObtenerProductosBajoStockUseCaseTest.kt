package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ObtenerProductosBajoStockUseCaseTest {

    private lateinit var productoRepository: ProductoRepository
    private lateinit var useCase: ObtenerProductosBajoStockUseCase

    @Before
    fun setup() {
        productoRepository = mock()
        useCase = ObtenerProductosBajoStockUseCase(productoRepository)
    }

    @Test
    fun `obtiene productos con stock <= stockMinimo`() = runBlockingTest {
        // Given
        val productoBajoStock = Producto(
            id = "prod-1",
            nombre = "Producto Bajo Stock",
            descripcion = null,
            precioVenta = 10.0,
            costoUnitario = 5.0,
            stockActual = 3,
            stockMinimo = 10,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )
        val productoOk = Producto(
            id = "prod-2",
            nombre = "Producto OK",
            descripcion = null,
            precioVenta = 10.0,
            costoUnitario = 5.0,
            stockActual = 20,
            stockMinimo = 10,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )

        whenever(productoRepository.getProductosBajoStock())
            .thenReturn(flowOf(listOf(productoBajoStock, productoOk)))

        // When
        val result = useCase().first()

        // Then
        assertEquals(1, result.size)
        assertEquals("prod-1", result.first().id)
    }

    @Test
    fun `retorna lista vacia si no hay productos con stock bajo`() = runBlockingTest {
        // Given
        val productoOk = Producto(
            id = "prod-1",
            nombre = "Producto OK",
            descripcion = null,
            precioVenta = 10.0,
            costoUnitario = 5.0,
            stockActual = 20,
            stockMinimo = 10,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )

        whenever(productoRepository.getProductosBajoStock())
            .thenReturn(flowOf(listOf(productoOk)))

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.isEmpty())
    }
}