package cu.stockcuba.app.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.repository.CategoriaRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListaProductosViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    // Filtros actuales
    private val _filtros = MutableStateFlow(ProductosFiltros())
    val filtros = _filtros

    // StateFlow combinado: productos filtrados + categorías
    val uiState = combine(
        productoRepository.getAll(),
        categoriaRepository.getActivas(),
        _filtros
    ) { allProductos, categorias, filtros ->
        val filtered = allProductos.filter { producto ->
            val matchesQuery = filtros.query.isBlank() ||
                producto.nombre.lowercase(java.util.Locale.getDefault()).contains(filtros.query.lowercase(java.util.Locale.getDefault())) ||
                producto.descripcion?.lowercase(java.util.Locale.getDefault())?.contains(filtros.query.lowercase(java.util.Locale.getDefault())) == true

            val matchesCategoria = filtros.categoriaId == null ||
                producto.categoriaId == filtros.categoriaId

            matchesQuery && matchesCategoria
        }.sortedBy { it.nombre }

        ListaProductosUiState.Success(
            productos = filtered,
            categorias = categorias,
            query = filtros.query,
            categoriaSeleccionada = filtros.categoriaId,
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
}