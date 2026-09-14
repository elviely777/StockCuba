package cu.stockcuba.app.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Moneda
import cu.stockcuba.app.domain.repository.CategoriaRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListaProductosViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val ajustesDataStore: AjustesDataStore
) : ViewModel() {

    private val _filtros = MutableStateFlow(ProductosFiltros())
    val filtros = _filtros

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState = combine(
        productoRepository.getAll(),
        categoriaRepository.getActivas(),
        _filtros,
        _selectedIds,
        ajustesDataStore.moneda,
        combine(
            ajustesDataStore.tasaUSD,
            ajustesDataStore.tasaMLC,
            ajustesDataStore.tasaEUR
        ) { usd, mlc, eur ->
            mapOf(Moneda.USD to usd, Moneda.MLC to mlc, Moneda.EUR to eur, Moneda.CUP to 1.0)
        }
    ) { params: Array<Any> ->
        val allProductos = params[0] as List<Producto>
        val allCategorias = params[1] as List<Categoria>
        val filtrosActivos = params[2] as ProductosFiltros
        val idsSeleccionados = params[3] as Set<String>
        val baseCurrency = params[4] as Moneda
        val tasasCambio = params[5] as Map<Moneda, Double>

        val filtered = allProductos.filter { producto ->
            val matchesQuery = filtrosActivos.query.isBlank() ||
                producto.nombre.lowercase(java.util.Locale.getDefault()).contains(filtrosActivos.query.lowercase(java.util.Locale.getDefault())) ||
                producto.descripcion?.lowercase(java.util.Locale.getDefault())?.contains(filtrosActivos.query.lowercase(java.util.Locale.getDefault())) == true

            val matchesCategoria = filtrosActivos.categoriaId == null ||
                producto.categoriaId == filtrosActivos.categoriaId

            matchesQuery && matchesCategoria
        }.sortedBy { it.nombre }

        ListaProductosUiState.Success(
            productos = filtered,
            categorias = allCategorias,
            query = filtrosActivos.query,
            categoriaSeleccionada = filtrosActivos.categoriaId,
            tasas = tasasCambio,
            monedaBase = baseCurrency,
            selectedProductIds = idsSeleccionados,
            isLoading = false
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListaProductosUiState.empty
        )

    fun setQuery(query: String) {
        _filtros.update { it.copy(query = query) }
    }

    fun setCategoria(categoriaId: String?) {
        _filtros.update { it.copy(categoriaId = categoriaId) }
    }

    fun limpiarFiltros() {
        _filtros.update { ProductosFiltros() }
    }

    suspend fun deleteProducto(productoId: String) {
        productoRepository.deleteById(productoId)
    }

    fun toggleSelection(productId: String) {
        _selectedIds.update { 
            if (it.contains(productId)) it - productId else it + productId
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value
            ids.forEach { id ->
                productoRepository.deleteById(id)
            }
            clearSelection()
        }
    }
}
