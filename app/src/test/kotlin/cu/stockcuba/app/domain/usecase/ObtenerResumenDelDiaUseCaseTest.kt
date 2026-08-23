package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.VentaRepository
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ObtenerResumenDelDiaUseCaseTest {

    private lateinit var ventaRepository: VentaRepository
    private lateinit var useCase: ObtenerResumenDelDiaUseCase

    @Before
    fun setup() {
        ventaRepository = mock()
        useCase = ObtenerResumenDelDiaUseCase(ventaRepository)
    }

    @Test
    fun `obtiene resumen del dia con total, cantidad y producto mas vendido`() = runBlockingTest {
        // Given
        val resumen = VentaRepository.ResumenDia(
            fecha = java.time.Instant.now().toEpochMilli(),
            totalVendido = 500.0,
            cantidadVentas = 10,
            productoMasVendido = VentaRepository.ProductoMasVendido(
                productoId = "prod-1",
                nombreProducto = "Producto Top",
                cantidadTotal = 25,
                totalVendido = 250.0
            )
        )

        whenever(ventaRepository.getResumenDelDia(any()))
            .thenReturn(Result.Success(resumen))

        // When
        val result = useCase(System.currentTimeMillis())

        // Then
        assertTrue(result is Result.Success)
        val resumenResult = (result as Result.Success).value
        assertEquals(500.0, resumenResult.totalVendido, 0.001)
        assertEquals(10, resumenResult.cantidadVentas)
        assertNotNull(resumenResult.productoMasVendido)
        assertEquals("Producto Top", resumenResult.productoMasVendido!!.nombreProducto)
        assertEquals(25, resumenResult.productoMasVendido!!.cantidadTotal)
    }

    @Test
    fun `retorna error si repositorio falla`() = runBlockingTest {
        // Given
        val error = Result.DomainError.DatabaseError(Exception("DB error"))
        whenever(ventaRepository.getResumenDelDia(any()))
            .thenReturn(Result.Failure(error))

        // When
        val result = useCase(System.currentTimeMillis())

        // Then
        assertTrue(result is Result.Failure)
        assertEquals(error, (result as Result.Failure).error)
    }

    @Test
    fun `retorna resumen sin producto mas vendido si no hay ventas`() = runBlockingTest {
        // Given
        val resumen = VentaRepository.ResumenDia(
            fecha = java.time.Instant.now().toEpochMilli(),
            totalVendido = 0.0,
            cantidadVentas = 0,
            productoMasVendido = null
        )

        whenever(ventaRepository.getResumenDelDia(any()))
            .thenReturn(Result.Success(resumen))

        // When
        val result = useCase(System.currentTimeMillis())

        // Then
        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).value.productoMasVendido)
    }
}