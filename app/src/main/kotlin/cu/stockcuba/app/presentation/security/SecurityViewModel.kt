package cu.stockcuba.app.presentation.security

import androidx.lifecycle.ViewModel
import cu.stockcuba.app.domain.security.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    val securityRepository: SecurityRepository
) : ViewModel()
