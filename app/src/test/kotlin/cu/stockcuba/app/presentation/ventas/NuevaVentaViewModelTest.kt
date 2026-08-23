package cu.stockcuba.app.presentation.ventas

import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.VentaItem
import cu.stockcuba.app.domain.repository.ClienteRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import cu.stockcuba.app.domain.usecase.RegistrarVentaUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class NuevaVentaViewModelTest {

    private lateinit var productoRepository: ProductoRepository
    private lateinit var clienteRepository: ClienteRepository
    private lateinit var registrarVentaUseCase: RegistrarVentaUseCase
    private lateinit var viewModel: NuevaVentaViewModel

    @Before
    fun setup() {
        productoRepository = mock()
        clienteRepository = mock()
        registrarVentaUseCase = mock()
        viewModel = NuevaVentaViewModel(productoRepository, clienteRepository, registrarVentaUseCase)
    }

    @Test
    fun `agregar producto al carrito incrementa cantidad si ya existe`() = runBlockingTest {
        // Given
        val producto = Producto(
            id = "prod-1",
            nombre = "Test Product",
            descripcion = null,
            precioVenta = 10.0,
            costoUnitario = 5.0,
            stockActual = 20,
            stockMinimo = 5,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )

        whenever(productoRepository.getAll())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(producto)))
        whenever(clienteRepository.getActivos())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        // Cargar datos iniciales
        viewModel.uiState.first()

        // When - agregar dos veces
        viewModel.agregarAlCarrito(producto)
        viewModel.agregarAlCarrito(producto)

        // Then
        val state = viewModel.uiState.first() as NuevaVentaUiState.Editing
        assertEquals(1, state.carrito.size)
        assertEquals(2, state.carrito.first().cantidad)
        assertEquals(20.0, state.carrito.first().subtotal, 0.001)
    }

    @Test
    fun `no permite agregar mas de stock disponible`() = runBlockingTest {
        // Given
        val producto = Producto(
            id = "prod-1",
            nombre = "Test Product",
            descripcion = null,
            precioVenta = 10.0,
            costoUnitario = 5.0,
            stockActual = 2,
            stockMinimo = 5,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )

        whenever(productoRepository.getAll())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(producto)))
        whenever(clienteRepository.getActivos())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        viewModel.uiState.first()

        // When - intentar agregar 3 veces (stock = 2)
        viewModel.agregarAlCarrito(producto)
        viewModel.agregarAlCarrito(producto)
        viewModel.agregarAlCarrito(producto) // debería ignorarse

        // Then
        val state = viewModel.uiState.first() as NuevaVentaUiState.Editing
        assertEquals(2, state.carrito.first().cantidad)
    }

    @Test
    fun `calcula totales correctamente`() = runBlockingTest {
        // Given
        val producto1 = Producto(
            id = "prod-1",
            nombre = "Producto 1",
            descripcion = null,
            precioVenta = 10.0,
            costoUnitario = 5.0,
            stockActual = 20,
            stockMinimo = 5,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )
        val producto2 = Producto(
            id = "prod-2",
            nombre = "Producto 2",
            descripcion = null,
            precioVenta = 15.0,
            costoUnitario = 7.0,
            stockActual = 10,
            stockMinimo = 3,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )

        whenever(productoRepository.getAll())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(producto1, producto2)))
        whenever(clienteRepository.getActivos())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        viewModel.uiState.first()

        // When
        viewModel.agregarAlCarrito(producto1) // 1 x 10 = 10
        viewModel.agregarAlCarrito(producto1) // 2 x 10 = 20
        viewModel.agregarAlCarrito(producto2) // 1 x 15 = 15

        // Then
        val state = viewModel.uiState.first() as NuevaVentaUiState.Editing
        val totales = viewModel.calcularTotales(state.carrito)
        assertEquals(35.0, totales.total, 0.001) // 20 + 15
    }

    @Test
    fun `validar venta falla si carrito vacio`() = runBlockingTest {
        // Given
        whenever(productoRepository.getAll())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        whenever(clienteRepository.getActivos())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        viewModel.uiState.first()

        // When
        viewModel.confirmarVenta()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state is NuevaVentaUiState.Editing)
        assertTrue((state as NuevaVentaUiState.Editing).errors.containsKey("carrito"))
    }

    @Test
    fun `validar pago mixto - suma debe ser igual al total`() = runBlockingTest {
        // Given
        val producto = Producto(
            id = "prod-1",
            nombre = "Test",
            descripcion = null,
            precioVenta = 100.0,
            costoUnitario = 50.0,
            stockActual = 10,
            stockMinimo = 2,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )

        whenever(productoRepository.getAll())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(producto)))
        whenever(clienteRepository.getActivos())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        viewModel.uiState.first()

        viewModel.agregarAlCarrito(producto) // 100 total
        viewModel.setMetodoPago(MetodoPago.MIXTO)
        viewModel.setEfectivoRecibido("30")
        viewModel.setTransferenciaMonto("50") // suma = 80 != 100

        // When
        viewModel.confirmarVenta()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state is NuevaVentaUiState.Editing)
        assertTrue((state as NuevaVentaUiState.Editing).errors.containsKey("pagoMixto"))
    }

    @Test
    fun `confirmar venta llama use case y resetea carrito en exito`() = runBlockingTest {
        // Given
        val producto = Producto(
            id = "prod-1",
            nombre = "Test",
            descripcion = null,
            precioVenta = 50.0,
            costoUnitario = 25.0,
            stockActual = 10,
            stockMinimo = 2,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )

        whenever(productoRepository.getAll())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(producto)))
        whenever(clienteRepository.getActivos())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))
        whenever(registrarVentaUseCase(any()))
            .thenReturn(Result.Success(Unit))

        viewModel.uiState.first()
        viewModel.agregarAlCarrito(producto)
        viewModel.setMetodoPago(MetodoPago.EFECTIVO)
        viewModel.setEfectivoRecibido("50")

        // When
        viewModel.confirmarVenta()

        // Then - verificar que se llamó el use case
        verify(registrarVentaUseCase).invoke(any())

        // Estado final debería ser Saved
        val state = viewModel.uiState.drop(1).first() // skip loading
        assertTrue(state is NuevaVentaUiState.Saved)
    }
}