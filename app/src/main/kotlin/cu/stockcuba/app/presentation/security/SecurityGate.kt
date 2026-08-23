package cu.stockcuba.app.presentation.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.security.SecurityRepository
import cu.stockcuba.app.presentation.security.PinEntryScreen
import cu.stockcuba.app.presentation.security.Mode
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SecurityGate composable that protects sensitive screens (T38).
 * Checks PIN/biometric status, shows BiometricPrompt or PinEntryScreen,
 * emits isUnlocked state to control content visibility.
 * Receives BiometricAuthenticator via Hilt injection.
 */
@Composable
fun SecurityGate(
    securityRepository: SecurityRepository,
    biometricAuthenticator: BiometricAuthenticator,
    onUnlocked: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var isUnlocked by remember { mutableStateOf<Boolean>(false) }
    var showBiometric by remember { mutableStateOf<Boolean>(false) }
    var showPinEntry by remember { mutableStateOf<Boolean>(false) }
    var pinEntryMode by remember { mutableStateOf<Mode>(Mode.Verify) }
    var biometricCancelled by remember { mutableStateOf<Boolean>(false) }

    // Check security status on composition
    val coroutineScope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val hasPinResult = securityRepository.hasPin()
        val biometricEnabledResult = securityRepository.getBiometricEnabled()
        
        val hasPin = (hasPinResult as? Result.Success<Boolean>)?.value ?: false
        val biometricEnabled = (biometricEnabledResult as? Result.Success<Boolean>)?.value ?: false

        if (!hasPin) {
            // No PIN set - unlock immediately
            isUnlocked = true
            onUnlocked(true)
        } else if (biometricEnabled && !biometricCancelled) {
            // PIN set and biometric enabled - try biometric first
            showBiometric = true
        } else {
            // PIN set, no biometric or biometric cancelled - show PIN entry
            showPinEntry = true
            pinEntryMode = Mode.Verify
        }
    }

    if (isUnlocked) {
        content()
    } else if (showBiometric) {
        BiometricPromptWrapper(
            biometricAuthenticator = biometricAuthenticator,
            onResult = { result ->
                when (result) {
                    is Result.Success -> {
                        if (result.value) {
                            // Biometric success
                            isUnlocked = true
                            onUnlocked(true)
                            showBiometric = false
                        } else {
                            // Biometric failed - fallback to PIN
                            showBiometric = false
                            showPinEntry = true
                            pinEntryMode = Mode.Verify
                        }
                    }
                    is Result.Failure -> {
                        // Biometric error - fallback to PIN
                        showBiometric = false
                        showPinEntry = true
                        pinEntryMode = Mode.Verify
                    }
                }
            },
            onCancel = {
                // User cancelled biometric - fallback to PIN
                biometricCancelled = true
                showBiometric = false
                showPinEntry = true
                pinEntryMode = Mode.Verify
            }
        )
    } else if (showPinEntry) {
        PinEntryScreen(
            mode = pinEntryMode,
            securityRepository = securityRepository,
            biometricAuthenticator = biometricAuthenticator,
            onResult = { result ->
                when (result) {
                    is Result.Success -> {
                        if (result.value == true) {
                            // PIN verified successfully
                            isUnlocked = true
                            onUnlocked(true)
                            showPinEntry = false
                        } else {
                            // PIN incorrect - PinEntryScreen handles backoff internally
                        }
                    }
                    is Result.Failure -> {
                        // Error - stay on PIN entry
                    }
                }
            }
        )
    } else {
        // Loading state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Wrapper for BiometricPrompt authentication in Compose.
 */
@Composable
private fun BiometricPromptWrapper(
    biometricAuthenticator: BiometricAuthenticator,
    onResult: (cu.stockcuba.app.domain.model.Result<Boolean>) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    androidx.compose.runtime.DisposableEffect(context) {
        // Find FragmentActivity from context
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is androidx.fragment.app.FragmentActivity) {
                biometricAuthenticator.initialize(currentContext)
                break
            }
            currentContext = currentContext.baseContext
        }
        
        // Start authentication
        biometricAuthenticator.authenticate(onResult)
        
        onDispose {
            biometricAuthenticator.cancel()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = Shape.Grande,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.padding(StockCubaSpacing.Xl)
        ) {
            Column(
                modifier = Modifier.padding(StockCubaSpacing.Xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Autenticación biométrica",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Toca el sensor de huella o usa Face ID",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "O cancela para usar PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}