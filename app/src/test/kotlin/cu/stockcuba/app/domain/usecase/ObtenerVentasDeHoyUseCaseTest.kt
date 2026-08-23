package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.repository.VentaRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ObtenerVentasDeHoyUseCaseTest {

    private lateinit var ventaRepository: VentaRepository
    private lateinit var useCase: ObtenerVentasDeHoyUseCase

    @Before
    fun setup() {
        ventaRepository = mock()
        useCase = ObtenerVentasDeHoyUseCase(ventaRepository)
    }

    @Test
    fun `obtiene ventas de hoy desde el repositorio`() = runBlockingTest {
        // Given
        val venta1 = Venta(
            id = "venta-1",
            fecha = java.time.Instant.now(),
            total = 50.0,
            metodoPago = cu.stockcuba.app.domain.model.MetodoPago.EFECTIVO,
            items = emptyList(),
            clienteId = null
        )
        val venta2 = Venta(
            id = "venta-2",
            fecha = java.time.Instant.now(),
            total = 75.0,
            metodoPago = cu.stockcuba.app.domain.model.MetodoPago.TRANSFERENCIA,
            items = emptyList(),
            clienteId = "cliente-1"
        )

        whenever(ventaRepository.getVentasDeHoy())
            .thenReturn(flowOf(listOf(venta1, venta2)))

        // When
        val result = useCase().first()

        // Then
        assertEquals(2, result.size)
        assertEquals("venta-1", result[0].id)
        assertEquals("venta-2", result[1].id)
    }

    @Test
    fun `retorna lista vacia si no hay ventas hoy`() = runBlockingTest {
        // Given
        whenever(ventaRepository.getVentasDeHoy())
            .thenReturn(flowOf(emptyList()))

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.isEmpty())
    }
}