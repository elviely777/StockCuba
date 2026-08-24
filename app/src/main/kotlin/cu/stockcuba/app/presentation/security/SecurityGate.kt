package cu.stockcuba.app.presentation.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
 * Checks PIN status and shows PinEntryScreen if needed.
 * Emits isUnlocked state to control content visibility.
 */
@Composable
fun SecurityGate(
    securityRepository: SecurityRepository,
    onUnlocked: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var isUnlocked by remember { mutableStateOf<Boolean>(false) }
    var showPinEntry by remember { mutableStateOf<Boolean>(false) }
    var pinEntryMode by remember { mutableStateOf<Mode>(Mode.Verify) }

    // Check security status on composition
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val hasPinResult = securityRepository.hasPin()
        val hasPin = (hasPinResult as? Result.Success<Boolean>)?.value ?: false

        if (!hasPin) {
            // No PIN set - unlock immediately
            isUnlocked = true
            onUnlocked(true)
        } else {
            // PIN set - show PIN entry
            showPinEntry = true
            pinEntryMode = Mode.Verify
        }
    }

    if (isUnlocked) {
        content()
    } else if (showPinEntry) {
        PinEntryScreen(
            mode = pinEntryMode,
            securityRepository = securityRepository,
            onResult = { result ->
                when (result) {
                    is Result.Success -> {
                        if (result.value == true) {
                            // PIN verified successfully
                            isUnlocked = true
                            onUnlocked(true)
                            showPinEntry = false
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
