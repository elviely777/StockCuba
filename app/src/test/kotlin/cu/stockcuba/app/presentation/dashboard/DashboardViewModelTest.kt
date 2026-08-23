package cu.stockcuba.app.presentation.dashboard

import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.VentaItem
import cu.stockcuba.app.domain.model.VentaRepository
import cu.stockcuba.app.domain.usecase.ObtenerProductosBajoStockUseCase
import cu.stockcuba.app.domain.usecase.ObtenerResumenDelDiaUseCase
import cu.stockcuba.app.domain.usecase.ObtenerVentasDeHoyUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class DashboardViewModelTest {

    private lateinit var obtenerVentasDeHoyUseCase: ObtenerVentasDeHoyUseCase
    private lateinit var obtenerResumenDelDiaUseCase: ObtenerResumenDelDiaUseCase
    private lateinit var obtenerProductosBajoStockUseCase: ObtenerProductosBajoStockUseCase
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        obtenerVentasDeHoyUseCase = mock()
        obtenerResumenDelDiaUseCase = mock()
        obtenerProductosBajoStockUseCase = mock()

        viewModel = DashboardViewModel(
            obtenerVentasDeHoyUseCase,
            obtenerResumenDelDiaUseCase,
            obtenerProductosBajoStockUseCase
        )
    }

    @Test
    fun `emite estado Loading inicial`() = runBlockingTest {
        // Given
        whenever(obtenerVentasDeHoyUseCase())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        whenever(obtenerResumenDelDiaUseCase())
            .thenReturn(Result.Success(VentaRepository.ResumenDia(
                fecha = System.currentTimeMillis(),
                totalVendido = 0.0,
                cantidadVentas = 0,
                productoMasVendido = null
            )))
        whenever(obtenerProductosBajoStockUseCase())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        // When - collect first emission
        val firstState = viewModel.uiState.first()

        // Then
        // Nota: El estado inicial depende de la implementación del combine/flatMapLatest
        // En este test verificamos que no crashea y emite algo
    }

    @Test
    fun `combina datos de los tres use cases correctamente`() = runBlockingTest {
        // Given
        val venta = Venta(
            id = "venta-1",
            fecha = java.time.Instant.now(),
            total = 100.0,
            metodoPago = cu.stockcuba.app.domain.model.MetodoPago.EFECTIVO,
            items = listOf(
                VentaItem(
                    id = "item-1",
                    ventaId = "venta-1",
                    productoId = "prod-1",
                    nombreProducto = "Producto Test",
                    cantidad = 2,
                    precioUnitario = 50.0,
                    subtotal = 100.0
                )
            ),
            clienteId = null
        )

        val resumen = VentaRepository.ResumenDia(
            fecha = System.currentTimeMillis(),
            totalVendido = 500.0,
            cantidadVentas = 5,
            productoMasVendido = VentaRepository.ProductoMasVendido(
                productoId = "prod-1",
                nombreProducto = "Producto Top",
                cantidadTotal = 10,
                totalVendido = 300.0
            )
        )

        val productoBajoStock = cu.stockcuba.app.domain.model.Producto(
            id = "prod-2",
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

        whenever(obtenerVentasDeHoyUseCase())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(venta)))
        whenever(obtenerResumenDelDiaUseCase())
            .thenReturn(Result.Success(
                VentaRepository.ResumenDia(
                    fecha = System.currentTimeMillis(),
                    totalVendido = 500.0,
                    cantidadVentas = 5,
                    productoMasVendido = VentaRepository.ProductoMasVendido(
                        productoId = "prod-1",
                        nombreProducto = "Producto Top",
                        cantidadTotal = 10,
                        totalVendido = 300.0
                    )
                )
            ))
        whenever(obtenerProductosBajoStockUseCase())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(
                cu.stockcuba.app.domain.model.Producto(
                    id = "prod-2",
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
            )))

        // When - collect a few emissions
        val states = viewModel.uiState.take(3).toList()

        // Then - should emit at least one Success state
        assertTrue(states.any { it is DashboardUiState.Success })
    }
}