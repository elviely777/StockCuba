package cu.stockcuba.app.presentation.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.data.supabase.SupabaseBusinessRepository
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VinculacionViewModel @Inject constructor(
    private val businessRepository: SupabaseBusinessRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val businessInfo: StateFlow<VinculacionState> = combine(
        businessRepository.isVinculado,
        businessRepository.businessId,
        businessRepository.posId
    ) { isVinculado, bId, pId ->
        VinculacionState(
            isVinculado = isVinculado,
            businessId = bId ?: "",
            posId = pId ?: ""
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VinculacionState())

    fun vincular(businessId: String, posNombre: String) {
        if (businessId.isBlank() || posNombre.isBlank()) {
            _error.value = "Todos los campos son obligatorios"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val result = businessRepository.vincular(businessId.trim(), posNombre.trim())
                
                _isLoading.value = false
                when (result) {
                    is Result.Failure -> {
                        val errorMsg = when (result.error) {
                            is DomainError.NetworkError -> "Error de red: ${result.error.cause?.message ?: "Timeout o sin conexión"}"
                            is DomainError.DatabaseError -> "Error de base de datos: ${result.error.cause?.message}"
                            else -> "Error: ${result.error}"
                        }
                        _error.value = errorMsg
                    }
                    is Result.Success -> {
                        // Éxito, el estado se actualiza via businessInfo flow
                    }
                }
            } catch (e: Throwable) {
                _isLoading.value = false
                _error.value = "Error crítico: ${e.message}"
            }
        }
    }

    fun desvincular() {
        viewModelScope.launch {
            businessRepository.desvincular()
        }
    }

    fun clearError() {
        _error.value = null
    }
}

data class VinculacionState(
    val isVinculado: Boolean = false,
    val businessId: String = "",
    val posId: String = ""
)
