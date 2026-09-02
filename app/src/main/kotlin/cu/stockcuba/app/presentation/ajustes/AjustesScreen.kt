package cu.stockcuba.app.presentation.ajustes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.window.DialogProperties
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.presentation.navigation.Screen
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialog states
    var showResetDialog by remember { mutableStateOf(false) }
    var resetConfirmationText by remember { mutableStateOf("") }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var pinSetupMode by remember { mutableStateOf<Mode>(Mode.Setup) }
    var showPinRemoveDialog by remember { mutableStateOf(false) }

    // SAF launcher for importing database file
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { selectedUri ->
            scope.launch {
                viewModel.importarBaseDatos(selectedUri).onSuccess {
                    scope.launch { snackbarHostState.showSnackbar("Datos importados con éxito") }
                }.onFailure { error ->
                    scope.launch { snackbarHostState.showSnackbar("Error al importar: $error") }
                }
            }
        }
    }

    // Navigation callback for reset completion
    viewModel.onResetComplete = {
        navController.navigate(Screen.Dashboard.route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        topBar = {
            AjustesHeader(onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            when (val state = uiState) {
                is AjustesUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = StockCubaColors.VerdeExito) }
                is AjustesUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = StockCubaColors.CoralAlerta) }
                is AjustesUiState.Success -> {
                    AjustesContenidoModerno(
                        state = state,
                        viewModel = viewModel,
                        onExportar = {
                            scope.launch {
                                viewModel.exportarBaseDatos().onSuccess {
                                    scope.launch { snackbarHostState.showSnackbar("Copia guardada con éxito") }
                                }.onFailure { error ->
                                    scope.launch { snackbarHostState.showSnackbar("Error al exportar: $error") }
                                }
                            }
                        },
                        onImportar = { importLauncher.launch(arrayOf("*/*")) },
                        onReiniciar = { showResetDialog = true },
                        onPinSetup = { pinSetupMode = Mode.Setup; showPinSetupDialog = true },
                        onPinChange = { pinSetupMode = Mode.Verify; showPinSetupDialog = true },
                        onPinRemove = { showPinRemoveDialog = true },
                        onExportarInventario = {
                            scope.launch {
                                viewModel.exportarReporteInventario().onSuccess {
                                    scope.launch { snackbarHostState.showSnackbar("Reporte de inventario guardado") }
                                }.onFailure { error ->
                                    scope.launch { snackbarHostState.showSnackbar("Error al exportar inventario: $error") }
                                }
                            }
                        },
                        onFeedback = {
                            scope.launch {
                                viewModel.sendFeedback().onFailure { 
                                    scope.launch { snackbarHostState.showSnackbar("No se encontró app de correo") }
                                }
                            }
                        },
                        onSembrar = {
                            scope.launch {
                                viewModel.sembrarDatosPrueba().fold(
                                    onSuccess = { 
                                        launch { snackbarHostState.showSnackbar("Datos de prueba generados") }
                                    },
                                    onFailure = { error ->
                                        launch { snackbarHostState.showSnackbar("Error: $error") }
                                    }
                                )
                            }
                        },
                        onNavigateToVinculacion = {
                            navController.navigate(Screen.VinculacionNegocio.route)
                        }
                    )
                }
                else -> {}
            }
        }

        // --- DIÁLOGOS ---
        if (showResetDialog) {
            ResetConfirmationDialog(
                onDismiss = { showResetDialog = false; resetConfirmationText = "" },
                onConfirm = {
                    viewModel.reiniciarDatos(resetConfirmationText)
                    showResetDialog = false
                },
                confirmationText = resetConfirmationText,
                onTextChange = { resetConfirmationText = it }
            )
        }

        if (showPinSetupDialog) {
            PinEntryScreen(
                mode = pinSetupMode,
                securityRepository = viewModel.securityRepository,
                onResult = { result ->
                    if (result is Result.Success && result.value) {
                        showPinSetupDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(if (pinSetupMode == Mode.Setup) "PIN configurado" else "PIN actualizado")
                        }
                    }
                }
            )
        }

        if (showPinRemoveDialog) {
            AlertDialog(
                onDismissRequest = { showPinRemoveDialog = false },
                title = { Text("¿Eliminar PIN de seguridad?", fontWeight = FontWeight.Bold) },
                text = { Text("La aplicación ya no estará protegida. Cualquier persona con acceso al teléfono podrá ver tus ventas e inventario.") },
                confirmButton = {
                    Button(
                        onClick = { 
                            viewModel.eliminarPin()
                            showPinRemoveDialog = false
                            scope.launch { snackbarHostState.showSnackbar("PIN eliminado correctamente") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StockCubaColors.CoralAlerta),
                        shape = Shape.Grande
                    ) {
                        Text("Eliminar PIN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinRemoveDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesHeader(onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
            }
            Text(
                "Configuración", 
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun AjustesContenidoModerno(
    state: AjustesUiState.Success,
    viewModel: AjustesViewModel,
    onExportar: () -> Unit,
    onImportar: () -> Unit,
    onReiniciar: () -> Unit,
    onPinSetup: () -> Unit,
    onPinChange: () -> Unit,
    onPinRemove: () -> Unit,
    onExportarInventario: () -> Unit,
    onFeedback: () -> Unit,
    onSembrar: () -> Unit,
    onNavigateToVinculacion: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(StockCubaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
    ) {
        // --- LOGO DE LA APP ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = StockCubaSpacing.Md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp),
                    shadowElevation = 4.dp
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = cu.stockcuba.app.R.mipmap.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.padding(12.dp).fillMaxSize()
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "StockCuba",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Gestión Inteligente",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- SECCIÓN 1: NEGOCIO ---
        item {
            SeccionAjustesModerna(titulo = "Mi Negocio", icono = Icons.Default.Storefront) {
                CampoAjusteModerno(
                    value = state.nombreNegocio,
                    onValueChange = { viewModel.guardarNombreNegocio(it) },
                    label = "Nombre Comercial",
                    icon = Icons.Default.Business,
                    error = state.validationErrors["nombre"]
                )
                CampoAjusteModerno(
                    value = state.direccion,
                    onValueChange = { viewModel.guardarDireccion(it) },
                    label = "Dirección / Localización",
                    icon = Icons.Default.LocationOn
                )
                CampoAjusteModerno(
                    value = state.telefono,
                    onValueChange = { viewModel.guardarTelefono(it) },
                    label = "Teléfono de Contacto",
                    icon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone,
                    error = state.validationErrors["telefono"]
                )
                
                Text("Preferencia de Moneda", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                SelectorAjusteModerno(
                    label = state.moneda.name,
                    icon = Icons.Default.MonetizationOn,
                    onClick = { /* Podría abrir un dialog con opciones */ }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                FilaAccionAjuste(
                    titulo = "Centralización y Sincronización",
                    subtitulo = if (state.isVinculado) "Vinculado a: ${state.businessId}" else "Vincular mi negocio a la nube",
                    icon = Icons.Default.CloudSync,
                    color = if (state.isVinculado) StockCubaColors.VerdeExito else MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToVinculacion
                )
            }
        }

        // --- SECCIÓN 2: APARIENCIA ---
        item {
            SeccionAjustesModerna(titulo = "Apariencia", icono = Icons.Default.Palette, color = Color(0xFF8B5CF6)) {
                Text("Tema Visual", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                SelectorAjusteModerno(
                    label = when(state.tema) {
                        "DARK" -> "Modo Oscuro"
                        "LIGHT" -> "Modo Claro"
                        else -> "Seguir Sistema"
                    },
                    icon = when(state.tema) {
                        "DARK" -> Icons.Default.DarkMode
                        "LIGHT" -> Icons.Default.LightMode
                        else -> Icons.Default.BrightnessAuto
                    },
                    onClick = { 
                        val next = when(state.tema) {
                            "SYSTEM" -> "DARK"
                            "DARK" -> "LIGHT"
                            else -> "SYSTEM"
                        }
                        viewModel.guardarTema(next)
                    }
                )
            }
        }

        // --- SECCIÓN 3: SEGURIDAD ---
        item {
            SeccionAjustesModerna(titulo = "Seguridad", icono = Icons.Default.Shield, color = StockCubaColors.VerdeExito) {
                FilaAccionAjuste(
                    titulo = if (state.tienePin) "Cambiar PIN de Acceso" else "Configurar PIN de Seguridad",
                    subtitulo = "Protege el acceso a tus finanzas e inventario",
                    icon = Icons.Default.Lock,
                    onClick = if (state.tienePin) onPinChange else onPinSetup
                )
                if (state.tienePin) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    FilaAccionAjuste(
                        titulo = "Eliminar PIN",
                        subtitulo = "Quitar la protección de acceso",
                        icon = Icons.Default.LockOpen,
                        color = StockCubaColors.CoralAlerta,
                        onClick = onPinRemove
                    )
                }
            }
        }

        // --- SECCIÓN 4: DATOS ---
        item {
            SeccionAjustesModerna(titulo = "Gestión de Datos", icono = Icons.Default.Storage, color = Color(0xFFF59E0B)) {
                FilaAccionAjuste(
                    titulo = "Copia de Seguridad",
                    subtitulo = "Exportar base de datos completa",
                    icon = Icons.Default.CloudUpload,
                    onClick = onExportar
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                FilaAccionAjuste(
                    titulo = "Reporte de Inventario (Excel)",
                    subtitulo = "Exportar existencias y valor (IPB/IPC)",
                    icon = Icons.Default.TableChart,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onExportarInventario
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                FilaAccionAjuste(
                    titulo = "Restaurar Datos",
                    subtitulo = "Importar desde un archivo externo",
                    icon = Icons.Default.CloudDownload,
                    onClick = onImportar
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                FilaAccionAjuste(
                    titulo = "Borrado Total",
                    subtitulo = "Eliminar toda la información del negocio",
                    icon = Icons.Default.DeleteForever,
                    color = StockCubaColors.CoralAlerta,
                    onClick = onReiniciar
                )
            }
        }

        // --- SECCIÓN 5: SOPORTE ---
        item {
            SeccionAjustesModerna(titulo = "Acerca de", icono = Icons.Default.Info) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Versión de la App", style = MaterialTheme.typography.bodyMedium)
                    Text(state.appVersion, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                FilaAccionAjuste(
                    titulo = "Enviar Sugerencia",
                    subtitulo = "Ayúdanos a mejorar StockCuba",
                    icon = Icons.Default.Email,
                    onClick = onFeedback
                )
            }
        }

        // --- SECCIÓN 6: DESARROLLO (PRUEBAS) ---
        item {
            SeccionAjustesModerna(
                titulo = "Modo Desarrollador", 
                icono = Icons.Default.BugReport, 
                color = Color(0xFF64748B)
            ) {
                FilaAccionAjuste(
                    titulo = "Generar Datos de Prueba",
                    subtitulo = "Inserta productos, clientes y ventas de ejemplo",
                    icon = Icons.Default.Science,
                    color = Color(0xFF64748B),
                    onClick = onSembrar
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun SeccionAjustesModerna(
    titulo: String,
    icono: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(StockCubaSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha = 0.1f), shape = CircleShape) {
                    Icon(icono, contentDescription = null, tint = color, modifier = Modifier.padding(8.dp).size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(titulo, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp))
            }
            content()
        }
    }
}

@Composable
fun CampoAjusteModerno(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun SelectorAjusteModerno(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = Shape.Grande,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun FilaAccionAjuste(
    titulo: String,
    subtitulo: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = Shape.Grande
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = color)
                Text(subtitulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ResetConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmationText: String,
    onTextChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Reiniciar aplicación?", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                Text("Esta acción eliminará todos tus productos, ventas y clientes. No se puede deshacer.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = onTextChange,
                    label = { Text("Escribe REINICIAR") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmationText == "REINICIAR",
                colors = ButtonDefaults.buttonColors(containerColor = StockCubaColors.CoralAlerta),
                shape = Shape.Grande
            ) {
                Text("Borrar Todo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
