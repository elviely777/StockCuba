package cu.stockcuba.app.presentation.inventario

import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.TipoMovimientoInventario
import cu.stockcuba.app.domain.repository.InventarioRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class InventarioViewModelTest {

    private lateinit var productoRepository: ProductoRepository
    private lateinit var inventarioRepository: InventarioRepository
    private lateinit var viewModel: InventarioViewModel

    @Before
    fun setup() {
        productoRepository = mock()
        inventarioRepository = mock()
        viewModel = InventarioViewModel(productoRepository, inventarioRepository)
    }

    @Test
    fun `calcula stockStatus correctamente para cada producto`() = runBlockingTest {
        // Given
        val productoOk = Producto(
            id = "prod-1",
            nombre = "OK Product",
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
        val productoBajo = Producto(
            id = "prod-2",
            nombre = "Bajo Stock",
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
        val productoSin = Producto(
            id = "prod-3",
            nombre = "Sin Stock",
            descripcion = null,
            precioVenta = 10.0,
            costoUnitario = 5.0,
            stockActual = 0,
            stockMinimo = 5,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )

        whenever(productoRepository.getAll())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(productoOk, productoBajo, productoSin)))

        // When
        val state = viewModel.uiState.first()

        // Then
        val successState = state as InventarioUiState.Success
        val productoConStock = successState.productos
        assertEquals(3, productoConStock.size)

        val ok = productoConStock.first { it.producto.id == "prod-1" }
        assertEquals(InventarioUiState.StockStatus.OK, ok.stockStatus)

        val bajo = productoConStock.first { it.producto.id == "prod-2" }
        assertEquals(InventarioUiState.StockStatus.BAJO, bajo.stockStatus)

        val sin = productoConStock.first { it.producto.id == "prod-3" }
        assertEquals(InventarioUiState.StockStatus.SIN_STOCK, sin.stockStatus)
    }

    @Test
    fun `filtra por query de busqueda`() = runBlockingTest {
        // Given
        val producto1 = Producto(
            id = "prod-1",
            nombre = "Café 500g",
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
            nombre = "Azúcar 1kg",
            descripcion = null,
            precioVenta = 8.0,
            costoUnitario = 4.0,
            stockActual = 15,
            stockMinimo = 3,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.KG,
            categoriaId = "cat-1",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )

        whenever(productoRepository.getAll())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(producto1, producto2)))

        // When
        viewModel.setQuery("café")
        val state = viewModel.uiState.drop(1).first()

        // Then
        val successState = state as InventarioUiState.Success
        assertEquals("café", successState.query)
        assertEquals(1, successState.productos.size)
        assertEquals("prod-1", successState.productos.first().producto.id)
    }

    @Test
    fun `filtra por filtroStock`() = runBlockingTest {
        // Given
        val productoOk = Producto(
            id = "prod-1",
            nombre = "OK",
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
        val productoBajo = Producto(
            id = "prod-2",
            nombre = "Bajo",
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

        whenever(productoRepository.getAll())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(productoOk, productoBajo)))

        // When - filtro BAJO
        viewModel.setFiltroStock(InventarioUiState.FiltroStock.BAJO)
        var state = viewModel.uiState.drop(1).first()
        var success = state as InventarioUiState.Success
        assertEquals(1, success.productos.size)
        assertEquals("prod-2", success.productos.first().producto.id)

        // When - filtro OK
        viewModel.setFiltroStock(InventarioUiState.FiltroStock.OK)
        state = viewModel.uiState.drop(1).first()
        success = state as InventarioUiState.Success
        assertEquals(1, success.productos.size)
        assertEquals("prod-1", success.productos.first().producto.id)

        // When - filtro TODOS
        viewModel.setFiltroStock(InventarioUiState.FiltroStock.TODOS)
        state = viewModel.uiState.drop(1).first()
        success = state as InventarioUiState.Success
        assertEquals(2, success.productos.size)
    }

    @Test
    fun `registrarMovimiento llama repositorio y actualiza`() = runBlockingTest {
        // Given
        val producto = Producto(
            id = "prod-1",
            nombre = "Test",
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
        whenever(inventarioRepository.registrarMovimiento(any()))
            .thenReturn(cu.stockcuba.app.domain.model.Result.Success(Unit))

        // When
        val result = viewModel.registrarMovimiento(producto, cu.stockcuba.app.domain.model.TipoMovimientoInventario.ENTRADA, 10, "Reabastecimiento")

        // Then
        assertTrue(result is cu.stockcuba.app.domain.model.Result.Success)
        verify(inventarioRepository).registrarMovimiento(argThat { mov ->
            mov.productoId == "prod-1" &&
            mov.tipo == cu.stockcuba.app.domain.model.TipoMovimientoInventario.ENTRADA &&
            mov.cantidad == 10 &&
            mov.motivo == "Reabastecimiento"
        })
    }

    @Test
    fun `registrarMovimiento valida cantidad positiva`() = runBlockingTest {
        // Given
        val producto = Producto(
            id = "prod-1",
            nombre = "Test",
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

        // When - cantidad 0
        val result = viewModel.registrarMovimiento(producto, cu.stockcuba.app.domain.model.TipoMovimientoInventario.ENTRADA, 0, "Test")

        // Then - debería fallar validación
        assertTrue(result is cu.stockcuba.app.domain.model.Result.Failure)
    }
}