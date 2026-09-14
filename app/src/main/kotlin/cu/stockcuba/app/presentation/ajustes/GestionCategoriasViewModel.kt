package cu.stockcuba.app.presentation.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.CategoriaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CategoriasUiState(
    val categorias: List<Categoria> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GestionCategoriasViewModel @Inject constructor(
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriasUiState())
    val uiState = _uiState.asStateFlow()

    init {
        cargarCategorias()
    }

    private fun cargarCategorias() {
        categoriaRepository.getAll()
            .onStart { _uiState.update { it.copy(isLoading = true) } }
            .onEach { categorias ->
                _uiState.update { it.copy(categorias = categorias, isLoading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun crearCategoria(nombre: String, color: Int) {
        viewModelScope.launch {
            val nueva = Categoria(
                id = UUID.randomUUID().toString(),
                nombre = nombre,
                color = color
            )
            categoriaRepository.insert(nueva)
        }
    }

    fun actualizarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            categoriaRepository.update(categoria)
        }
    }

    fun eliminarCategoria(categoriaId: String, onCantDelete: (Int) -> Unit) {
        viewModelScope.launch {
            // Verificar si hay productos en esta categoría
            val count = categoriaRepository.countProductosInCategoria(categoriaId).first()
            if (count > 0) {
                onCantDelete(count)
                return@launch
            }

            // Impedir eliminar la categoría "Otros" si es la de sistema
            if (categoriaId == "otros") {
                return@launch
            }

            categoriaRepository.deleteById(categoriaId)
        }
    }
}
