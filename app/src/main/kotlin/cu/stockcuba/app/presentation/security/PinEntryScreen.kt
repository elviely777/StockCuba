package cu.stockcuba.app.presentation.security

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.security.SecurityRepository
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * PIN entry screen with setup and verify modes, exponential backoff (T39).
 * Supports 4-6 digit PIN, setup requires confirmation, verify has 5 attempt limit with backoff.
 */
@Composable
fun PinEntryScreen(
    mode: Mode,
    securityRepository: SecurityRepository,
    onResult: (Result<Boolean>) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attemptCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isLockedOut by remember { mutableStateOf(false) }
    var lockoutEndTime by remember { mutableStateOf<Long>(0) }

    val maxAttempts = 5
    val baseDelayMs = 1000L

    // Calculate backoff delay for current attempt
    val currentDelayMs = (baseDelayMs * 2.0.pow(attemptCount - 1)).toLong().coerceAtMost(16000L)

    // Handle lockout countdown
    androidx.compose.runtime.LaunchedEffect(isLockedOut, lockoutEndTime) {
        if (isLockedOut) {
            val remaining = lockoutEndTime - System.currentTimeMillis()
            if (remaining > 0) {
                delay(remaining)
            }
            isLockedOut = false
            attemptCount = 0
            errorMessage = null
        }
    }

    val canSubmit = when (mode) {
        Mode.Setup -> pin.length in 4..6 && confirmPin.length in 4..6 && !isLoading && !isLockedOut
        Mode.Verify -> pin.length in 4..6 && !isLoading && !isLockedOut
    }

    val title = when (mode) {
        Mode.Setup -> "Configurar PIN"
        Mode.Verify -> "Ingresar PIN"
    }

    val subtitle = when (mode) {
        Mode.Setup -> "Crea un PIN de 4-6 dígitos para proteger la app"
        Mode.Verify -> "Introduce tu PIN para acceder"
    }

    val actionLabel = when (mode) {
        Mode.Setup -> "Guardar"
        Mode.Verify -> "Verificar"
    }

    // Handle submit action
    fun handleSubmit() {
        errorMessage = null
        isLoading = true

        scope.launch {
            try {
                when (mode) {
                    Mode.Setup -> {
                        if (pin != confirmPin) {
                            errorMessage = "Los PINs no coinciden"
                            isLoading = false
                            return@launch
                        }
                        val result = securityRepository.setPin(pin)
                        isLoading = false
                        onResult(result.map { true })
                    }
                    Mode.Verify -> {
                        val result = securityRepository.verifyPin(pin)
                        isLoading = false
                        
                        when (result) {
                            is Result.Success -> {
                                if (result.value) {
                                    // Success - reset attempt count
                                    attemptCount = 0
                                    onResult(Result.Success(true))
                                } else {
                                    // Failed attempt
                                    attemptCount++
                                    errorMessage = "PIN incorrecto"
                                    
                                    if (attemptCount >= maxAttempts) {
                                        // Lockout with exponential backoff
                                        val delay = currentDelayMs
                                        lockoutEndTime = System.currentTimeMillis() + delay
                                        isLockedOut = true
                                        errorMessage = "Demasiados intentos fallidos. Bloqueado por ${delay / 1000}s"
                                    }
                                    onResult(Result.Success(false))
                                }
                            }
                            is Result.Failure -> {
                                errorMessage = result.error.toString()
                                onResult(result)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Error desconocido"
                onResult(Result.Failure(DomainError.DatabaseError(e)))
            }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(StockCubaSpacing.Xl)
        ) {
            Column(
                modifier = Modifier.padding(StockCubaSpacing.Xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
            ) {
                // Logo
                Surface(
                    color = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(72.dp),
                    shadowElevation = 4.dp
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = cu.stockcuba.app.R.mipmap.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.padding(8.dp).fillMaxSize()
                    )
                }

                // Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Xs)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // PIN dots indicator
                PinDotsIndicator(
                    length = when (mode) {
                        Mode.Setup -> pin.length
                        Mode.Verify -> pin.length
                    },
                    maxLength = 6
                )

                // PIN input field
                PinInputField(
                    value = pin,
                    onValueChange = { pin = it.filter { it.isDigit() }.take(6) },
                    label = when (mode) {
                        Mode.Setup -> "Nuevo PIN"
                        Mode.Verify -> "PIN"
                    },
                    errorMessage = errorMessage,
                    isLoading = isLoading
                )

                // Confirm PIN field (setup mode only)
                if (mode == Mode.Setup) {
                    PinInputField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter { it.isDigit() }.take(6) },
                        label = "Confirmar PIN",
                        errorMessage = if (pin.isNotEmpty() && confirmPin.isNotEmpty() && pin != confirmPin) "Los PINs no coinciden" else null,
                        isLoading = isLoading
                    )
                }

                // Attempt counter / Lockout message
                if (isLockedOut) {
                    val remainingSeconds = ((lockoutEndTime - System.currentTimeMillis()) / 1000).coerceAtLeast(1)
                    Text(
                        text = "Demasiados intentos. Intenta de nuevo en $remainingSeconds segundos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                } else if (attemptCount > 0 && mode == Mode.Verify) {
                    Text(
                        text = "Intento $attemptCount de $maxAttempts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Action button
                Button(
                    onClick = { handleSubmit() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = canSubmit
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual indicator showing PIN length as dots.
 */
@Composable
private fun PinDotsIndicator(length: Int, maxLength: Int = 6) {
    val outlineColor = androidx.compose.material3.MaterialTheme.colorScheme.outline
    Row(
        horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm),
        modifier = Modifier.padding(vertical = StockCubaSpacing.Md)
    ) {
        (1..maxLength).forEach { index ->
            val filled = index <= length
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (filled) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(
                            color = outlineColor,
                            radius = 6.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

/**
 * PIN input field with password visual transformation.
 */
@Composable
private fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorMessage: String?,
    isLoading: Boolean
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done
        ),
        visualTransformation = PasswordVisualTransformation(),
        shape = Shape.Grande,
        isError = errorMessage != null,
        supportingText = { if (errorMessage != null) Text(errorMessage) },
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        enabled = !isLoading
    )
}

/**
 * PIN entry mode.
 */
sealed class Mode {
    data object Setup : Mode()
    data object Verify : Mode()
}
