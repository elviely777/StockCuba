package cu.stockcuba.app.presentation.productos

import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.repository.CategoriaRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ListaProductosViewModelTest {

    private lateinit var productoRepository: ProductoRepository
    private lateinit var categoriaRepository: CategoriaRepository
    private lateinit var viewModel: ListaProductosViewModel

    @Before
    fun setup() {
        productoRepository = mock()
        categoriaRepository = mock()
        viewModel = ListaProductosViewModel(productoRepository, categoriaRepository)
    }

    @Test
    fun `filtra productos por query de busqueda`() = runBlockingTest {
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
        whenever(categoriaRepository.getActivas())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        // When - sin filtro
        viewModel.setQuery("")
        var state = viewModel.uiState.first()
        assertEquals(2, (state as ListaProductosUiState.Success).productos.size)

        // When - filtrar por "café"
        viewModel.setQuery("café")
        state = viewModel.uiState.drop(1).first() // skip initial
        assertEquals(1, (state as ListaProductosUiState.Success).productos.size)
        assertEquals("prod-1", state.productos.first().id)

        // When - filtrar por "azúcar"
        viewModel.setQuery("azúcar")
        state = viewModel.uiState.drop(1).first()
        assertEquals(1, state.productos.size)
        assertEquals("prod-2", state.productos.first().id)
    }

    @Test
    fun `filtra productos por categoria`() = runBlockingTest {
        // Given
        val cat1 = Categoria(id = "cat-1", nombre = "Bebidas", color = 0xFF2DD4BF)
        val cat2 = Categoria(id = "cat-2", nombre = "Alimentos", color = 0xFFF59E0B)

        val producto1 = Producto(
            id = "prod-1",
            nombre = "Café",
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
            nombre = "Pan",
            descripcion = null,
            precioVenta = 5.0,
            costoUnitario = 2.0,
            stockActual = 10,
            stockMinimo = 2,
            unidadMedida = cu.stockcuba.app.domain.model.UnidadMedida.UNIDAD,
            categoriaId = "cat-2",
            fechaCreacion = java.time.Instant.now(),
            activo = true
        )

        whenever(productoRepository.getAll())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(producto1, producto2)))
        whenever(categoriaRepository.getActivas())
            .thenReturn(kotlinx.coroutines.flow.flowOf(listOf(cat1, cat2)))

        // When - sin filtro
        viewModel.setCategoria(null)
        var state = viewModel.uiState.first()
        assertEquals(2, (state as ListaProductosUiState.Success).productos.size)

        // When - filtro por cat-1
        viewModel.setCategoria("cat-1")
        state = viewModel.uiState.drop(1).first()
        assertEquals(1, state.productos.size)
        assertEquals("cat-1", state.productos.first().categoriaId)
    }

    @Test
    fun `limpiarFiltros resetea query y categoria`() = runBlockingTest {
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
        whenever(categoriaRepository.getActivas())
            .thenReturn(kotlinx.coroutines.flow.flowOf(emptyList()))

        // When - aplicar filtros
        viewModel.setQuery("test")
        viewModel.setCategoria("cat-1")
        var state = viewModel.uiState.drop(1).first()
        assertEquals("test", (state as ListaProductosUiState.Success).query)
        assertEquals("cat-1", state.categoriaSeleccionada)

        // When - limpiar
        viewModel.limpiarFiltros()
        state = viewModel.uiState.drop(1).first()
        assertEquals("", state.query)
        assertNull(state.categoriaSeleccionada)
    }
}