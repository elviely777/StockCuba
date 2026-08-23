package cu.stockcuba.app.domain.usecase

import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.VentaItem
import cu.stockcuba.app.domain.repository.InventarioRepository
import cu.stockcuba.app.domain.repository.VentaRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class RegistrarVentaUseCaseTest {

    private lateinit var ventaRepository: VentaRepository
    private lateinit var inventarioRepository: InventarioRepository
    private lateinit var useCase: RegistrarVentaUseCase

    @Before
    fun setup() {
        ventaRepository = mock()
        inventarioRepository = mock()
        useCase = RegistrarVentaUseCase(ventaRepository, inventarioRepository)
    }

    @Test
    fun `registrar venta exitosa - guarda venta y registra movimientos de inventario`() = runBlockingTest {
        // Given
        val venta = crearVentaValida()
        whenever(ventaRepository.registrarVenta(venta))
            .thenReturn(Result.Success(Unit))
        whenever(inventarioRepository.registrarMovimientos(any()))
            .thenReturn(Result.Success(Unit))

        // When
        val result = useCase(venta)

        // Then
        assertTrue(result is Result.Success)
        verify(ventaRepository).registrarVenta(venta)
        verify(inventarioRepository).registrarMovimientos(argThat { movimientos ->
            movimientos.size == venta.items.size &&
            movimientos.all { it.tipo == cu.stockcuba.app.domain.model.TipoMovimientoInventario.VENTA }
        })
    }

    @Test
    fun `registrar venta falla si ventaRepository falla`() = runBlockingTest {
        // Given
        val venta = crearVentaValida()
        val error = Result.DomainError.DatabaseError(Exception("DB error"))
        whenever(ventaRepository.registrarVenta(venta))
            .thenReturn(Result.Failure(error))

        // When
        val result = useCase(venta)

        // Then
        assertTrue(result is Result.Failure)
        assertEquals(error, (result as Result.Failure).error)
        verify(inventarioRepository, never()).registrarMovimientos(any())
    }

    @Test
    fun `registrar venta falla si inventarioRepository falla`() = runBlockingTest {
        // Given
        val venta = crearVentaValida()
        val error = Result.DomainError.DatabaseError(Exception("Inventory error"))
        whenever(ventaRepository.registrarVenta(venta))
            .thenReturn(Result.Success(Unit))
        whenever(inventarioRepository.registrarMovimientos(any()))
            .thenReturn(Result.Failure(error))

        // When
        val result = useCase(venta)

        // Then
        assertTrue(result is Result.Failure)
        assertEquals(error, (result as Result.Failure).error)
    }

    @Test
    fun `registrar venta crea movimiento por cada item con cantidad correcta`() = runBlockingTest {
        // Given
        val item1 = VentaItem(
            id = "item-1",
            ventaId = "venta-1",
            productoId = "prod-1",
            nombreProducto = "Producto 1",
            cantidad = 3,
            precioUnitario = 10.0,
            subtotal = 30.0
        )
        val item2 = VentaItem(
            id = "item-2",
            ventaId = "venta-1",
            productoId = "prod-2",
            nombreProducto = "Producto 2",
            cantidad = 5,
            precioUnitario = 20.0,
            subtotal = 100.0
        )
        val venta = Venta(
            id = "venta-1",
            fecha = java.time.Instant.now(),
            total = 130.0,
            metodoPago = cu.stockcuba.app.domain.model.MetodoPago.EFECTIVO,
            items = listOf(item1, item2),
            clienteId = null
        )

        whenever(ventaRepository.registrarVenta(venta))
            .thenReturn(Result.Success(Unit))
        whenever(inventarioRepository.registrarMovimientos(any()))
            .thenReturn(Result.Success(Unit))

        // When
        val result = useCase(venta)

        // Then
        assertTrue(result is Result.Success)
        verify(inventarioRepository).registrarMovimientos(argThat { movimientos ->
            movimientos.size == 2 &&
            movimientos.any { it.productoId == "prod-1" && it.cantidad == 3 } &&
            movimientos.any { it.productoId == "prod-2" && it.cantidad == 5 }
        })
    }

    private fun crearVentaValida(): Venta {
        val item = VentaItem(
            id = "item-1",
            ventaId = "venta-1",
            productoId = "prod-1",
            nombreProducto = "Test Product",
            cantidad = 2,
            precioUnitario = 15.0,
            subtotal = 30.0
        )
        return Venta(
            id = "venta-1",
            fecha = java.time.Instant.now(),
            total = 30.0,
            metodoPago = cu.stockcuba.app.domain.model.MetodoPago.EFECTIVO,
            items = listOf(item),
            clienteId = null
        )
    }
}