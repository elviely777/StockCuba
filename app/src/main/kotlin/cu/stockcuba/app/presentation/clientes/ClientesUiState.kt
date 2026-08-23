package cu.stockcuba.app.presentation.clientes

import cu.stockcuba.app.domain.model.Cliente
import kotlinx.coroutines.flow.Flow

/**
 * Estado de UI para la pantalla de Clientes.
 */
sealed interface ClientesUiState {
    data class Success(
        val clientes: List<Cliente> = emptyList(),
        val query: String = "",
        val isLoading: Boolean = false
    ) : ClientesUiState

    data object Loading : ClientesUiState

    data class Error(val message: String) : ClientesUiState

    companion object {
        val empty = Success()
    }
}

/**
 * Estado para formulario de cliente (crear/editar).
 */
sealed interface FormularioClienteUiState {
    data class Editing(
        val nombre: String = "",
        val telefono: String = "",
        val notas: String = "",
        val isLoading: Boolean = false,
        val errors: Map<String, String> = emptyMap(),
        val isEditing: Boolean = false,
        val clienteId: String? = null
    ) : FormularioClienteUiState

    data object Saving : FormularioClienteUiState

    data object Saved : FormularioClienteUiState

    data class Error(val message: String) : FormularioClienteUiState
}