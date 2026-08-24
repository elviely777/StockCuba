package cu.stockcuba.app.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.UnidadMedida
import cu.stockcuba.app.domain.repository.CategoriaRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import cu.stockcuba.app.domain.usecase.CrearProductoUseCase
import cu.stockcuba.app.domain.usecase.ActualizarProductoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FormularioProductoViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val crearProductoUseCase: CrearProductoUseCase,
    private val actualizarProductoUseCase: ActualizarProductoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FormularioProductoUiState>(FormularioProductoUiState.Editing())
    val uiState = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FormularioProductoUiState.Editing())

    init {
        loadCategorias()
    }

    private fun loadCategorias() {
        viewModelScope.launch {
            categoriaRepository.getActivas().firstOrNull()?.let { categorias ->
                _uiState.update { state ->
                    when (state) {
                        is FormularioProductoUiState.Editing -> state.copy(categorias = categorias)
                        else -> state
                    }
                }
            }
        }
    }

    /**
     * Carga un producto existente para edición.
     */
    fun cargarProductoParaEditar(productoId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                when (state) {
                    is FormularioProductoUiState.Editing -> state.copy(isLoading = true)
                    else -> state
                }
            }

            productoRepository.getByIdSync(productoId).onSuccess { producto ->
                _uiState.update { state ->
                    when (state) {
                        is FormularioProductoUiState.Editing -> state.copy(
                            nombre = producto.nombre,
                            descripcion = producto.descripcion ?: "",
                            precioVenta = producto.precioVenta.toString(),
                            costoUnitario = producto.costoUnitario.toString(),
                            stockInicial = producto.stockActual.toString(),
                            stockMinimo = producto.stockMinimo.toString(),
                            unidadMedida = producto.unidadMedida,
                            categoriaId = producto.categoriaId,
                            isEditing = true,
                            productoId = producto.id,
                            isLoading = false
                        )
                        else -> state
                    }
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    when (state) {
                        is FormularioProductoUiState.Editing -> state.copy(
                            isLoading = false,
                            errors = mapOf("general" to error.toString())
                        )
                        else -> state
                    }
                }
            }
        }
    }

    /**
     * Actualiza un campo del formulario.
     */
    fun updateField(field: String, value: String) {
        _uiState.update { state ->
            when (state) {
                is FormularioProductoUiState.Editing -> {
                    val newState = when (field) {
                        "nombre" -> state.copy(nombre = value)
                        "descripcion" -> state.copy(descripcion = value)
                        "precioVenta" -> state.copy(precioVenta = value)
                        "costoUnitario" -> state.copy(costoUnitario = value)
                        "stockInicial" -> state.copy(stockInicial = value)
                        "stockMinimo" -> state.copy(stockMinimo = value)
                        else -> state
                    }
                    // Limpiar error del campo al editar
                    newState.copy(errors = state.errors - field)
                }
                else -> state
            }
        }
    }

    fun updateUnidadMedida(unidad: UnidadMedida) {
        _uiState.update { state ->
            when (state) {
                is FormularioProductoUiState.Editing -> state.copy(unidadMedida = unidad)
                else -> state
            }
        }
    }

    fun updateCategoria(categoriaId: String?) {
        _uiState.update { state ->
            when (state) {
                is FormularioProductoUiState.Editing -> {
                    state.copy(
                        categoriaId = categoriaId,
                        esNuevaCategoria = categoriaId == "otros",
                        errors = state.errors - "categoria"
                    )
                }
                else -> state
            }
        }
    }

    fun updateNuevaCategoriaNombre(nombre: String) {
        _uiState.update { state ->
            when (state) {
                is FormularioProductoUiState.Editing -> state.copy(nuevaCategoriaNombre = nombre, errors = state.errors - "nuevaCategoria")
                else -> state
            }
        }
    }

    /**
     * Valida el formulario.
     */
    private fun validar(state: FormularioProductoUiState.Editing): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (state.nombre.trim().isEmpty()) {
            errors["nombre"] = "El nombre es obligatorio"
        }

        if (state.precioVenta.trim().isEmpty()) {
            errors["precioVenta"] = "El precio de venta es obligatorio"
        } else {
            state.precioVenta.toDoubleOrNull()?.let {
                if (it < 0) errors["precioVenta"] = "El precio debe ser positivo"
            } ?: run { errors["precioVenta"] = "Precio inválido" }
        }

        if (state.costoUnitario.trim().isEmpty()) {
            errors["costoUnitario"] = "El costo es obligatorio"
        } else {
            state.costoUnitario.toDoubleOrNull()?.let {
                if (it < 0) errors["costoUnitario"] = "El costo debe ser positivo"
            } ?: run { errors["costoUnitario"] = "Costo inválido" }
        }

        if (state.stockInicial.trim().isEmpty()) {
            errors["stockInicial"] = "El stock inicial es obligatorio"
        } else {
            state.stockInicial.toIntOrNull()?.let {
                if (it < 0) errors["stockInicial"] = "El stock no puede ser negativo"
            } ?: run { errors["stockInicial"] = "Stock inválido" }
        }

        if (state.stockMinimo.trim().isEmpty()) {
            errors["stockMinimo"] = "El stock mínimo es obligatorio"
        } else {
            state.stockMinimo.toIntOrNull()?.let {
                if (it < 0) errors["stockMinimo"] = "El stock mínimo no puede ser negativo"
            } ?: run { errors["stockMinimo"] = "Stock mínimo inválido" }
        }

        if (state.categoriaId == null || state.categoriaId!!.isBlank()) {
            errors["categoria"] = "Debe seleccionar una categoría"
        } else if (state.esNuevaCategoria && state.nuevaCategoriaNombre.trim().isEmpty()) {
            errors["nuevaCategoria"] = "Especifique el nombre de la categoría"
        }

        return errors
    }

    /**
     * Guarda el producto (crear o actualizar).
     */
    fun guardar() {
        _uiState.update { state ->
            when (state) {
                is FormularioProductoUiState.Editing -> {
                    val errors = validar(state)
                    if (errors.isNotEmpty()) {
                        state.copy(errors = errors)
                    } else {
                        state.copy(isLoading = true, errors = emptyMap())
                    }
                }
                else -> state
            }
        }

        viewModelScope.launch {
            val currentState = _uiState.value
            when (currentState) {
                is FormularioProductoUiState.Editing -> {
                    if (currentState.errors.isEmpty()) {
                        var finalCategoriaId = currentState.categoriaId!!

                        // Si es una nueva categoría (seleccionó "Otros"), crearla primero (T50)
                        if (currentState.esNuevaCategoria) {
                            val nuevaCat = Categoria(
                                id = UUID.randomUUID().toString(),
                                nombre = currentState.nuevaCategoriaNombre.trim(),
                                color = 0xFF6366F1.toInt() // IndigoMarca
                            )
                            val insertResult = categoriaRepository.insert(nuevaCat)
                            if (insertResult is Result.Success) {
                                finalCategoriaId = nuevaCat.id
                            } else {
                                _uiState.update { FormularioProductoUiState.Error("No se pudo crear la categoría") }
                                return@launch
                            }
                        }

                        val producto = Producto(
                            id = currentState.productoId ?: UUID.randomUUID().toString(),
                            nombre = currentState.nombre.trim(),
                            descripcion = currentState.descripcion.trim().takeIf { it.isNotBlank() },
                            precioVenta = currentState.precioVenta.toDoubleOrNull() ?: 0.0,
                            costoUnitario = currentState.costoUnitario.toDoubleOrNull() ?: 0.0,
                            stockActual = currentState.stockInicial.toIntOrNull() ?: 0,
                            stockMinimo = currentState.stockMinimo.toIntOrNull() ?: 0,
                            unidadMedida = currentState.unidadMedida,
                            categoriaId = finalCategoriaId,
                            fechaCreacion = java.time.Instant.now(),
                            activo = true
                        )

                        val result = if (currentState.isEditing) {
                            actualizarProductoUseCase(producto)
                        } else {
                            crearProductoUseCase(producto)
                        }

                        _uiState.update { _ ->
                            when (result) {
                                is Result.Success -> FormularioProductoUiState.Saved
                                is Result.Failure -> FormularioProductoUiState.Error(result.error.toString())
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    fun resetForm() {
        _uiState.update { state ->
            when (state) {
                is FormularioProductoUiState.Editing -> state.copy(
                    nombre = "",
                    descripcion = "",
                    precioVenta = "",
                    costoUnitario = "",
                    stockInicial = "",
                    stockMinimo = "",
                    unidadMedida = UnidadMedida.UNIDAD,
                    categoriaId = state.categorias.firstOrNull()?.id,
                    isEditing = false,
                    productoId = null,
                    errors = emptyMap()
                )
                else -> FormularioProductoUiState.Editing()
            }
        }
    }
}