package cu.stockcuba.app.presentation.productos

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.UnidadMedida
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioProductoScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    initialModo: String = "crear",
    initialProductoId: String? = null,
    viewModel: FormularioProductoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialModo, initialProductoId) {
        if (initialModo == "editar" && initialProductoId != null) {
            viewModel.cargarProductoParaEditar(initialProductoId)
        } else if (initialModo == "crear") {
            viewModel.resetForm()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (val state = uiState) {
                            is FormularioProductoUiState.Editing -> if (state.isEditing) "Editar Producto" else "Nuevo Producto"
                            else -> "Producto"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (uiState is FormularioProductoUiState.Editing) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.guardar() },
                    icon = { Icon(Icons.Default.Save, contentDescription = null) },
                    text = { Text("Guardar", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) },
                    containerColor = StockCubaColors.VerdeExito,
                    contentColor = Color(0xFF001E1C),
                    shape = Shape.Grande,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is FormularioProductoUiState.Saving -> {
                    PantallaCargando("Guardando producto...")
                }
                is FormularioProductoUiState.Saved -> {
                    PantallaExito(onSave)
                }
                is FormularioProductoUiState.Error -> {
                    PantallaError(state.message, onRetry = { viewModel.resetForm() })
                }
                is FormularioProductoUiState.Editing -> {
                    FormularioContenido(state = state, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun FormularioContenido(
    state: FormularioProductoUiState.Editing,
    viewModel: FormularioProductoViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(StockCubaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
    ) {
        // --- SECCIÓN 1: IDENTIDAD ---
        item {
            SeccionFormulario(titulo = "Información General", icono = Icons.AutoMirrored.Filled.Label) {
                CampoTextoModerno(
                    value = state.nombre,
                    onValueChange = { viewModel.updateField("nombre", it) },
                    label = "Nombre del Producto",
                    placeholder = "Ej. Cerveza Cristal 350ml",
                    icon = Icons.Default.Inventory2,
                    isError = state.errors["nombre"] != null,
                    supportingText = state.errors["nombre"]
                )
                
                CampoTextoModerno(
                    value = state.descripcion,
                    onValueChange = { viewModel.updateField("descripcion", it) },
                    label = "Descripción (Opcional)",
                    placeholder = "Detalles adicionales...",
                    icon = Icons.Default.Description,
                    singleLine = false,
                    maxLines = 3
                )

                Text(
                    "Categoría",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
                SelectorCategoriaModerno(
                    categorias = state.categorias,
                    categoriaSeleccionada = state.categoriaId,
                    onChange = { viewModel.updateCategoria(it) },
                    error = state.errors["categoria"]
                )

                AnimatedVisibility(
                    visible = state.esNuevaCategoria,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = StockCubaSpacing.Md)) {
                        CampoTextoModerno(
                            value = state.nuevaCategoriaNombre,
                            onValueChange = { viewModel.updateNuevaCategoriaNombre(it) },
                            label = "Nombre de la Categoría",
                            placeholder = "Nueva categoría...",
                            icon = Icons.Default.CreateNewFolder,
                            isError = state.errors["nuevaCategoria"] != null,
                            supportingText = state.errors["nuevaCategoria"]
                        )
                    }
                }
            }
        }

        // --- SECCIÓN 2: ECONOMÍA ---
        item {
            SeccionFormulario(titulo = "Precios y Costos", icono = Icons.Default.Payments) {
                Row(horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                    CampoTextoModerno(
                        value = state.precioVenta,
                        onValueChange = { viewModel.updateField("precioVenta", it) },
                        label = "Precio Venta",
                        icon = Icons.Default.Sell,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                        isError = state.errors["precioVenta"] != null,
                        supportingText = state.errors["precioVenta"]
                    )
                    CampoTextoModerno(
                        value = state.costoUnitario,
                        onValueChange = { viewModel.updateField("costoUnitario", it) },
                        label = "Costo",
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                        isError = state.errors["costoUnitario"] != null,
                        supportingText = state.errors["costoUnitario"]
                    )
                }
            }
        }

        // --- SECCIÓN 3: LOGÍSTICA ---
        item {
            SeccionFormulario(titulo = "Inventario", icono = Icons.Default.Warehouse) {
                Text(
                    "Unidad de Medida",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                SelectorUnidadMedidaModerno(
                    unidadActual = state.unidadMedida,
                    onChange = { viewModel.updateUnidadMedida(it) }
                )
                
                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                    CampoTextoModerno(
                        value = state.stockInicial,
                        onValueChange = { viewModel.updateField("stockInicial", it) },
                        label = "Stock Actual",
                        icon = Icons.Default.Inventory,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                        isError = state.errors["stockInicial"] != null,
                        supportingText = state.errors["stockInicial"],
                        helpText = "Cantidad total que tienes ahora mismo en tu negocio."
                    )
                    CampoTextoModerno(
                        value = state.stockMinimo,
                        onValueChange = { viewModel.updateField("stockMinimo", it) },
                        label = "Stock Mínimo",
                        icon = Icons.Default.NotificationImportant,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                        isError = state.errors["stockMinimo"] != null,
                        supportingText = state.errors["stockMinimo"],
                        helpText = "Cantidad mínima para activar alertas de reabastecimiento."
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun SeccionFormulario(
    titulo: String,
    icono: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(StockCubaSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, tint = StockCubaColors.VerdeExito, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(titulo, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            content()
        }
    }
}

@Composable
fun CampoTextoModerno(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    helpText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    var showHelp by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) },
            trailingIcon = if (helpText != null) {
                {
                    IconButton(onClick = { showHelp = !showHelp }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Ayuda",
                            tint = if (showHelp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            shape = Shape.Grande,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = StockCubaColors.VerdeExito,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        } else if (showHelp && helpText != null) {
            Text(
                text = helpText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun SelectorCategoriaModerno(
    categorias: List<Categoria>,
    categoriaSeleccionada: String?,
    onChange: (String?) -> Unit,
    error: String?
) {
    var expanded by remember { mutableStateOf(false) }
    val nombreActual = categorias.find { it.id == categoriaSeleccionada }?.nombre ?: "Seleccionar categoría..."

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = Shape.Grande,
            border = BorderStroke(1.dp, if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(nombreActual, style = MaterialTheme.typography.bodyLarge, color = if (categoriaSeleccionada == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
            categorias.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.nombre) },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = Color(cat.color)) },
                    onClick = { onChange(cat.id); expanded = false }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Otra categoría...", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = StockCubaColors.VerdeExito) },
                onClick = { onChange("otros"); expanded = false }
            )
        }
    }
    if (error != null) {
        Text(error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
    }
}

@Composable
fun SelectorUnidadMedidaModerno(unidadActual: UnidadMedida, onChange: (UnidadMedida) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    OutlinedCard(
        onClick = { expanded = true },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = Shape.Grande,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(unidadActual.name, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }
    
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        UnidadMedida.values().forEach { unidad ->
            DropdownMenuItem(text = { Text(unidad.name) }, onClick = { onChange(unidad); expanded = false })
        }
    }
}

@Composable
fun PantallaCargando(mensaje: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = StockCubaColors.VerdeExito, strokeWidth = 4.dp)
            Spacer(Modifier.height(16.dp))
            Text(mensaje, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PantallaExito(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(modifier = Modifier.size(100.dp), shape = Shape.Full, color = StockCubaColors.VerdeExito.copy(alpha = 0.15f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StockCubaColors.VerdeExito, modifier = Modifier.size(64.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("¡Producto Guardado!", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text("El catálogo ha sido actualizado correctamente.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), shape = Shape.Grande) {
            Text("Continuar")
        }
    }
}

@Composable
fun PantallaError(mensaje: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Error, contentDescription = null, tint = StockCubaColors.CoralAlerta, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Hubo un problema", style = MaterialTheme.typography.titleLarge)
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp), shape = Shape.Grande) {
            Text("Reintentar")
        }
    }
}
