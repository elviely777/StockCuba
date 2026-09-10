package cu.stockcuba.app.presentation.security

import androidx.lifecycle.ViewModel
import cu.stockcuba.app.domain.security.SecurityRepository
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    val securityRepository: SecurityRepository,
    private val ajustesDataStore: AjustesDataStore
) : ViewModel() {
    val rolActual: Flow<cu.stockcuba.app.domain.model.RolUsuario> = ajustesDataStore.rolActual
}
