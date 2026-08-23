package cu.stockcuba.app.presentation.ajustes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.window.DialogProperties
import cu.stockcuba.app.domain.feedback.FeedbackRepository
import cu.stockcuba.app.presentation.ajustes.Moneda
import cu.stockcuba.app.domain.security.SecurityRepository
import cu.stockcuba.app.presentation.navigation.Screen
import cu.stockcuba.app.presentation.security.BiometricAuthenticator
import cu.stockcuba.app.presentation.security.PinEntryScreen
import cu.stockcuba.app.presentation.security.Mode
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    onBack: () -> Unit,
    navController: NavController,
    viewModel: AjustesViewModel = hiltViewModel()
) {
    val securityRepository = viewModel.securityRepository
    val biometricAuthenticator = viewModel.biometricAuthenticator
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialog states
    var showResetDialog by remember { mutableStateOf(false) }
    var resetConfirmationText by remember { mutableStateOf("") }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var pinSetupMode by remember { mutableStateOf<Mode>(Mode.Setup) }
    var showBiometricToggleDialog by remember { mutableStateOf(false) }

    // SAF launcher for importing database file
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                viewModel.importarBaseDatos(selectedUri).onFailure { error ->
                    scope.launch { snackbarHostState.showSnackbar("Error al importar: ${error.toString()}") }
                }
            }
        }
    }

    // Navigation callback for reset completion
    viewModel.onResetComplete = {
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is AjustesUiState.Loading -> AjustesCargando()
            is AjustesUiState.Error -> AjustesError(message = state.message)
            is AjustesUiState.Success -> AjustesContenido(
                state = state,
                onNombreChange = { viewModel.guardarNombreNegocio(it) },
                onDireccionChange = { viewModel.guardarDireccion(it) },
                onTelefonoChange = { viewModel.guardarTelefono(it) },
                onMonedaChange = { viewModel.guardarMoneda(it) },
                onImpuestoChange = { viewModel.guardarImpuesto(it) },
                onTemaChange = { viewModel.guardarTema(it) },
                onSeguridadChange = { viewModel.guardarSeguridadBiometrica(it) },
                onExportar = {
                    scope.launch {
                        viewModel.exportarBaseDatos().onSuccess { uri ->
                            scope.launch { snackbarHostState.showSnackbar("Exportado a ${uri.toString()}") }
                        }.onFailure { error ->
                            scope.launch { snackbarHostState.showSnackbar("Error al exportar: ${error.toString()}") }
                        }
                    }
                },
                onImportar = {
                    importLauncher.launch(arrayOf("application/x-sqlite3"))
                },
                onReiniciar = { showResetDialog = true },
                onPinSetup = { pinSetupMode = Mode.Setup; showPinSetupDialog = true },
                onPinChange = { pinSetupMode = Mode.Verify; showPinSetupDialog = true },
                onBiometricToggle = { showBiometricToggleDialog = true },
                onFeedback = {
                    scope.launch {
                        viewModel.sendFeedback().onSuccess {
                            scope.launch { snackbarHostState.showSnackbar("Correo abierto para enviar feedback") }
                        }.onFailure { error ->
                            scope.launch { snackbarHostState.showSnackbar("No hay app de correo: ${error.toString()}") }
                        }
                    }
                },
                padding = padding
            )
            else -> { }
        }

        // ===== RESET CONFIRMATION DIALOG (T29) =====
        if (showResetDialog) {
            ResetConfirmationDialog(
                onDismiss = { showResetDialog = false; resetConfirmationText = "" },
                onConfirm = {
                    viewModel.reiniciarDatos(resetConfirmationText)
                    showResetDialog = false
                    resetConfirmationText = ""
                },
                confirmationText = resetConfirmationText,
                onTextChange = { resetConfirmationText = it }
            )
        }

        // ===== PIN SETUP/CHANGE DIALOG (T41) =====
        if (showPinSetupDialog) {
            PinEntryScreen(
                mode = pinSetupMode,
                securityRepository = securityRepository,
                biometricAuthenticator = biometricAuthenticator,
                onResult = { result ->
                    when (result) {
                        is cu.stockcuba.app.domain.model.Result.Success -> {
                            if (result.value) {
                                showPinSetupDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (pinSetupMode == Mode.Setup)
                                            "PIN configurado correctamente"
                                        else
                                            "PIN cambiado correctamente"
                                    )
                                }
                            }
                        }
                        is cu.stockcuba.app.domain.model.Result.Failure -> {
                            scope.launch { snackbarHostState.showSnackbar("Error: ${result.error.toString()}") }
                        }
                    }
                }
            )
        }

        // ===== BIOMETRIC TOGGLE DIALOG (T41) =====
        if (showBiometricToggleDialog) {
            val state = uiState as? AjustesUiState.Success
            BiometricToggleDialog(
                onDismiss = { showBiometricToggleDialog = false },
                currentEnabled = state?.seguridadBiometrica ?: false,
                onToggle = { enabled ->
                    scope.launch { viewModel.toggleBiometric(enabled) }
                    showBiometricToggleDialog = false
                },
                requiresPin = (state?.tienePin ?: false) == false
            )
        }
    }
}

/**
 * Loading state composable - shows centered CircularProgressIndicator
 */
@Composable
fun AjustesCargando() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .testTag("cargando_progress"),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            Text(
                text = "Cargando ajustes...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Error state composable - shows centered error message with retry button
 */
@Composable
fun AjustesError(
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(StockCubaSpacing.Xl)
            .testTag("error_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StockCubaSpacing.Lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            onRetry?.let { retry ->
                OutlinedButton(
                    onClick = retry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}

/**
 * Reusable text field with inline validation error support
 */
@Composable
fun CampoTextoAjuste(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .testTag("campo_texto_ajuste"),
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = { if (supportingText != null) Text(supportingText) },
        shape = Shape.Grande,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        )
    )
}

/**
 * Main content composable - LazyColumn with 5 sections:
 * Negocio, Apariencia, Seguridad, Base de Datos, Acerca de
 */
@Composable
fun AjustesContenido(
    state: AjustesUiState.Success,
    onNombreChange: (String) -> Unit,
    onDireccionChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onMonedaChange: (Moneda) -> Unit,
    onImpuestoChange: (String) -> Unit,
    onTemaChange: (String) -> Unit,
    onSeguridadChange: (Boolean) -> Unit,
    onExportar: () -> Unit,
    onImportar: () -> Unit,
    onReiniciar: () -> Unit,
    onPinSetup: () -> Unit,
    onPinChange: () -> Unit,
    onBiometricToggle: () -> Unit,
    onFeedback: () -> Unit,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg),
        contentPadding = StockCubaSpacing.screenPadding
    ) {
        // ===== SECCIÓN 1: NEGOCIO =====
        item {
            SeccionAjustes(
                titulo = "Negocio",
                icono = Icons.Default.Business,
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                CampoTextoAjuste(
                    label = "Nombre del Negocio",
                    value = state.nombreNegocio,
                    onValueChange = onNombreChange,
                    isError = state.nombreError != null,
                    supportingText = state.nombreError,
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
                Spacer(modifier = Modifier.height(StockCubaSpacing.Md))
                CampoTextoAjuste(
                    label = "Dirección",
                    value = state.direccion,
                    onValueChange = onDireccionChange,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
                Spacer(modifier = Modifier.height(StockCubaSpacing.Md))
                CampoTextoAjuste(
                    label = "Teléfono",
                    value = state.telefono,
                    onValueChange = onTelefonoChange,
                    isError = state.telefonoError != null,
                    supportingText = state.telefonoError,
                    keyboardType = KeyboardType.Phone,
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
                Spacer(modifier = Modifier.height(StockCubaSpacing.Md))
                SelectorMoneda(
                    monedaActual = state.moneda,
                    onMonedaChange = onMonedaChange
                )
                Spacer(modifier = Modifier.height(StockCubaSpacing.Md))
                CampoTextoAjuste(
                    label = "Impuesto predeterminado (%)",
                    value = "%.0f".format(state.impuesto),
                    onValueChange = onImpuestoChange,
                    isError = state.impuestoError != null,
                    supportingText = state.impuestoError,
                    keyboardType = KeyboardType.Number,
                    leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
            }
        }

        // ===== SECCIÓN 2: APARIENCIA =====
        item {
            SeccionAjustes(
                titulo = "Apariencia",
                icono = Icons.Default.Brightness4,
                iconColor = MaterialTheme.colorScheme.tertiary
            ) {
                SelectorTema(
                    temaActual = state.tema,
                    onTemaChange = onTemaChange
                )
            }
        }

        // ===== SECCIÓN 3: SEGURIDAD =====
        item {
            SeccionAjustes(
                titulo = "Seguridad",
                icono = Icons.Default.Lock,
                iconColor = MaterialTheme.colorScheme.secondary
            ) {
                // Biometric toggle
                FilaAjuste(
                    titulo = "Autenticación biométrica",
                    subtitulo = "Usar huella o rostro para desbloquear",
                    icono = Icons.Default.Fingerprint,
                    accion = {
                        Switch(
                            checked = state.seguridadBiometrica,
                            onCheckedChange = onSeguridadChange,
                            modifier = Modifier.testTag("biometric_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
                HorizontalDivider()
                // PIN setup/change
                FilaAjuste(
                    titulo = if (state.tienePin) "Cambiar PIN" else "Configurar PIN",
                    subtitulo = if (state.tienePin) "Modificar PIN actual" else "Crear PIN de 4-6 dígitos",
                    icono = Icons.Default.Key,
                    accion = {
                        TextButton(
                            onClick = if (state.tienePin) onPinChange else onPinSetup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (state.tienePin) "Cambiar" else "Configurar",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                )
                HorizontalDivider()
                // Biometric toggle dialog trigger
                FilaAjuste(
                    titulo = "Autenticación biométrica avanzada",
                    subtitulo = "Configurar opciones biométricas",
                    icono = Icons.Default.Security,
                    accion = {
                        TextButton(
                            onClick = onBiometricToggle,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Configurar",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                )
            }
        }

        // ===== SECCIÓN 4: BASE DE DATOS =====
        item {
            SeccionAjustes(
                titulo = "Base de Datos",
                icono = Icons.Default.Storage,
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                FilaAjuste(
                    titulo = "Exportar base de datos",
                    subtitulo = "Crear copia de seguridad completa",
                    icono = Icons.Default.Download,
                    accion = {
                        Button(
                            onClick = onExportar,
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shape.Grande
                        ) {
                            Text("Exportar")
                        }
                    }
                )
                HorizontalDivider()
                FilaAjuste(
                    titulo = "Importar base de datos",
                    subtitulo = "Restaurar desde archivo de respaldo",
                    icono = Icons.Default.Upload,
                    accion = {
                        OutlinedButton(
                            onClick = onImportar,
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shape.Grande
                        ) {
                            Text("Importar")
                        }
                    }
                )
                HorizontalDivider()
                FilaAjuste(
                    titulo = "Reiniciar todos los datos",
                    subtitulo = "Elimina productos, ventas, clientes y ajustes (mantiene tema, PIN y biometría)",
                    icono = Icons.Default.DeleteForever,
                    iconColor = MaterialTheme.colorScheme.error,
                    accion = {
                        Button(
                            onClick = onReiniciar,
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shape.Grande,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Reiniciar")
                        }
                    }
                )
            }
        }

        // ===== SECCIÓN 5: ACERCA DE =====
        item {
            SeccionAjustes(
                titulo = "Acerca de",
                icono = Icons.Default.Info,
                iconColor = MaterialTheme.colorScheme.outline
            ) {
                FilaAjuste(
                    titulo = "Versión",
                    subtitulo = "StockCuba ${state.appVersion}",
                    icono = Icons.Default.Info,
                    accion = { /* No action */ }
                )
                HorizontalDivider()
                FilaAjuste(
                    titulo = "Enviar feedback",
                    subtitulo = "Reportar errores o sugerencias",
                    icono = Icons.Default.Mail,
                    accion = {
                        TextButton(
                            onClick = onFeedback,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Enviar",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                )
                HorizontalDivider()
                FilaAjuste(
                    titulo = "Compartir app",
                    subtitulo = "Recomendar a otros comerciantes",
                    icono = Icons.Default.Share,
                    accion = {
                        TextButton(
                            onClick = { /* Share intent */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Compartir",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * Reset confirmation dialog - requires exact "REINICIAR" text
 */
@Composable
fun ResetConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmationText: String,
    onTextChange: (String) -> Unit
) {
    val isValid = confirmationText == "REINICIAR"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar reinicio total") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
            ) {
                Text(
                    text = "Esta acción eliminará TODOS los datos:\n• Productos e inventario\n• Ventas e historial\n• Clientes y proveedores\n• Ajustes de negocio\n\nSe conservarán: tema, PIN y biometría.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CampoTextoAjuste(
                    label = "Escribe REINICIAR para confirmar",
                    value = confirmationText,
                    onValueChange = onTextChange,
                    isError = confirmationText.isNotBlank() && !isValid,
                    supportingText = if (confirmationText.isNotBlank() && !isValid) "Debe escribir exactamente: REINICIAR" else null,
                    keyboardType = KeyboardType.Text,
                    singleLine = true,
                    modifier = Modifier.testTag("reset_confirmation_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Confirmar reinicio")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    )
}

/**
 * Biometric toggle dialog with Switch and PIN requirement warning
 */
@Composable
fun BiometricToggleDialog(
    onDismiss: () -> Unit,
    currentEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    requiresPin: Boolean = false
) {
    var enabled by remember { mutableStateOf(currentEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Autenticación biométrica") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
            ) {
                Text(
                    text = "Permite desbloquear la app usando tu huella digital o reconocimiento facial.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (requiresPin) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = StockCubaSpacing.Sm),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Requiere configurar PIN primero",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { newValue ->
                        enabled = newValue
                        onToggle(newValue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("biometric_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Section container with title and icon
 */
@Composable
private fun SeccionAjustes(
    titulo: String,
    icono: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Card(
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(StockCubaSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

/**
 * Row item for settings with title, subtitle, icon, and trailing action
 */
@Composable
private fun FilaAjuste(
    titulo: String,
    subtitulo: String? = null,
    icono: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    accion: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Xxs)
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitulo?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        accion()
    }
}

/**
 * Moneda selector dropdown
 */
@Composable
private fun SelectorMoneda(
    monedaActual: Moneda,
    onMonedaChange: (Moneda) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val monedas = Moneda.values()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Xs)
    ) {
        Text(
            text = "Moneda",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = Shape.Grande,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = monedaActual.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (expanded) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                monedas.forEach { moneda ->
                    DropdownMenuItem(
                        text = { Text(moneda.name) },
                        onClick = {
                            onMonedaChange(moneda)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Tema selector dropdown
 */
@Composable
private fun SelectorTema(
    temaActual: String,
    onTemaChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val temas = listOf("SYSTEM", "LIGHT", "DARK")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Xs)
    ) {
        Text(
            text = "Tema",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
            shape = Shape.Grande,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
                ) {
                    Icon(
                        imageVector = when (temaActual) {
                            "LIGHT" -> Icons.Default.LightMode
                            "DARK" -> Icons.Default.DarkMode
                            else -> Icons.Default.BrightnessAuto
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when (temaActual) {
                            "SYSTEM" -> "Sistema"
                            "LIGHT" -> "Claro"
                            "DARK" -> "Oscuro"
                            else -> temaActual
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (expanded) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                temas.forEach { tema ->
                    DropdownMenuItem(
                        text = {
                            Text(when (tema) {
                                "SYSTEM" -> "Sistema"
                                "LIGHT" -> "Claro"
                                "DARK" -> "Oscuro"
                                else -> tema
                            })
                        },
                        onClick = {
                            onTemaChange(tema)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
