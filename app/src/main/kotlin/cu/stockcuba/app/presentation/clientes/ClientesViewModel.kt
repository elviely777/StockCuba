package cu.stockcuba.app.presentation.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.Cliente
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.ClienteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ClientesViewModel @Inject constructor(
    private val clienteRepository: ClienteRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query

    // StateFlow combinado: clientes filtrados
    val uiState = combine(
        clienteRepository.getActivos(),
        _query
    ) { allClientes, query ->
        val filtered = allClientes.filter { cliente ->
            query.isBlank() ||
                cliente.nombre.lowercase(java.util.Locale.getDefault()).contains(query.lowercase(java.util.Locale.getDefault())) ||
                cliente.telefono?.lowercase(java.util.Locale.getDefault())?.contains(query.lowercase(java.util.Locale.getDefault())) == true
        }.sortedBy { it.nombre }

        ClientesUiState.Success(
            clientes = filtered,
            query = query,
            isLoading = false
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ClientesUiState.empty
        )

    fun setQuery(query: String) {
        _query.value = query
    }

    suspend fun crearCliente(nombre: String, telefono: String?, notas: String?): Result<Unit> {
        val cliente = Cliente(
            id = UUID.randomUUID().toString(),
            nombre = nombre.trim(),
            telefono = telefono?.trim()?.takeIf { it.isNotBlank() },
            notas = notas?.trim()?.takeIf { it.isNotBlank() }
        )
        return clienteRepository.insert(cliente)
    }

    suspend fun actualizarCliente(cliente: Cliente): Result<Unit> {
        return clienteRepository.update(cliente)
    }

    suspend fun eliminarCliente(clienteId: String): Result<Unit> {
        return clienteRepository.deleteById(clienteId)
    }
}