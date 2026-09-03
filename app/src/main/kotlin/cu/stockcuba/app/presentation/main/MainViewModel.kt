package cu.stockcuba.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.repository.BusinessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val businessRepository: BusinessRepository
) : ViewModel() {

    init {
        // Verificar el estado del negocio en Supabase al iniciar la app
        viewModelScope.launch {
            businessRepository.verificarEstadoRemoto()
        }
    }

    val isVinculado: StateFlow<Boolean> = businessRepository.isVinculado
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val estadoNegocio: StateFlow<String> = businessRepository.estadoNegocio
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "ACTIVO"
        )
}
