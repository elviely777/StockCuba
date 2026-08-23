package cu.stockcuba.app.presentation.security

import androidx.lifecycle.ViewModel
import cu.stockcuba.app.domain.security.SecurityRepository
import cu.stockcuba.app.presentation.security.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    val securityRepository: SecurityRepository,
    val biometricAuthenticator: BiometricAuthenticator
) : ViewModel()
