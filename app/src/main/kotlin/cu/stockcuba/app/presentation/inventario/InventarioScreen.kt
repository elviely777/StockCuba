package cu.stockcuba.app.presentation.inventario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.TipoMovimientoInventario
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import cu.stockcuba.app.presentation.dashboard.formatoCantidad
import cu.stockcuba.app.presentation.navigation.Screen
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    onAjuste: (Producto) -> Unit,
    onHistorial: (Producto) -> Unit,
    viewModel: InventarioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val queryText = remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
                actions = {
                    // Chip filtro stock
                    val currentState = uiState
                    FiltroStockChips(filtroActual = when (currentState) {
                        is InventarioUiState.Success -> currentState.filtroStock
                        else -> FiltroStock.TODOS
                    }, onFiltroChange = { viewModel.setFiltroStock(it) })
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is InventarioUiState.Loading -> InventarioCargando()
            is InventarioUiState.Error -> InventarioError(message = state.message, onRetry = { /* viewModel.refrescar() */ })
            is InventarioUiState.Success -> InventarioContenido(
                state = state,
                queryText = queryText,
                onQueryChange = { text ->
                    queryText.value = text
                    viewModel.setQuery(text)
                },
                onAjuste = onAjuste,
                onHistorial = onHistorial,
                padding = padding
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioContenido(
    state: InventarioUiState.Success,
    queryText: androidx.compose.runtime.MutableState<String>,
    onQueryChange: (String) -> Unit,
    onAjuste: (Producto) -> Unit,
    onHistorial: (Producto) -> Unit,
    padding: androidx.compose.foundation.layout.PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(top = StockCubaSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
    ) {
        // ===== BUSCADOR =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StockCubaSpacing.Md)
        ) {
            BasicTextField(
                value = queryText.value,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(StockCubaColors.InputFondo, Shape.Full),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (queryText.value.isEmpty()) {
                                Text(
                                    text = "Buscar producto...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                        if (queryText.value.isNotEmpty()) {
                            IconButton(
                                onClick = { queryText.value = ""; onQueryChange("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        // ===== RESUMEN RÁPIDO =====
        ResumenInventario(productos = state.productos)

        // ===== LISTA DE PRODUCTOS =====
        if (state.productos.isEmpty()) {
            InventarioVacio()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm),
                contentPadding = PaddingValues(StockCubaSpacing.Md)
            ) {
                items(state.productos) { productoConStock ->
                    ProductoInventarioRow(
                        productoConStock = productoConStock,
                        onAjuste = { onAjuste(productoConStock.producto) },
                        onHistorial = { onHistorial(productoConStock.producto) }
                    )
                }
            }
        }
    }
}

/**
 * Chips de filtro de stock en TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltroStockChips(
    filtroActual: FiltroStock,
    onFiltroChange: (FiltroStock) -> Unit
) {
    val showMenu = remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = true,
            onClick = { showMenu.value = true },
            leadingIcon = { Icon(imageVector = Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) },
            label = { Text("${filtroActual.nombre()} ▼", style = MaterialTheme.typography.labelMedium) },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = StockCubaColors.InputFondo,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = Shape.ExtraGrande,
            modifier = Modifier.height(36.dp).padding(end = 16.dp)
        )

        DropdownMenu(expanded = showMenu.value, onDismissRequest = { showMenu.value = false }) {
            FiltroStock.entries.forEach { filtro ->
                val color = when (filtro) {
                    FiltroStock.OK -> StockCubaColors.VerdeExito
                    FiltroStock.BAJO -> Color(0xFFF59E0B)
                    FiltroStock.SIN_STOCK -> StockCubaColors.CoralAlerta
                    FiltroStock.TODOS -> MaterialTheme.colorScheme.onSurface
                }
                DropdownMenuItem(
                    onClick = { onFiltroChange(filtro); showMenu.value = false },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                            Text(filtro.nombre(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                )
            }
        }
    }
}

fun FiltroStock.nombre(): String = when (this) {
    FiltroStock.TODOS -> "Todos"
    FiltroStock.OK -> "OK"
    FiltroStock.BAJO -> "Bajo"
    FiltroStock.SIN_STOCK -> "Sin stock"
}

/**
 * Resumen rápido: tarjetas con conteos.
 */
@Composable
fun ResumenInventario(productos: List<ProductoConStock>) {
    val ok = productos.count { it.stockStatus == StockStatus.OK }
    val bajo = productos.count { it.stockStatus == StockStatus.BAJO }
    val sin = productos.count { it.stockStatus == StockStatus.SIN_STOCK }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StockCubaSpacing.Md),
        horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
    ) {
        ResumenChip("OK", ok.toString(), StockCubaColors.VerdeExito, StockCubaColors.ChipStockAltoFondo)
        ResumenChip("BAJO", bajo.toString(), Color(0xFFF59E0B), Color(0x1AF59E0B))
        ResumenChip("SIN STOCK", sin.toString(), StockCubaColors.CoralAlerta, StockCubaColors.ChipStockBajoFondo)
    }
}

@Composable
fun RowScope.ResumenChip(label: String, count: String, color: Color, bg: Color) {
    Card(
        modifier = Modifier.weight(1f).height(70.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = Shape.Grande
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(StockCubaSpacing.Md)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
                Text(count, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = color)
            }
        }
    }
}

/**
 * Row de producto en inventario con barra de progreso visual.
 */
@Composable
fun ProductoInventarioRow(
    productoConStock: ProductoConStock,
    onAjuste: () -> Unit,
    onHistorial: () -> Unit,
    onEditar: ((Producto) -> Unit)? = null,
    onDetalle: ((Producto) -> Unit)? = null
) {
    val producto = productoConStock.producto
    val status = productoConStock.stockStatus
    val porcentaje = (productoConStock.porcentajeStock * 100).toInt().coerceIn(0, 100)
    val color = productoConStock.stockStatusColor()
    val bg = productoConStock.stockStatusBackground()
    val label = productoConStock.stockStatusLabel()

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = Shape.Grande,
        border = BorderStroke(1.dp, StockCubaColors.BordeSutil)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StockCubaSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
        ) {
            // Fila superior: Nombre + Badge + Menú contextual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(producto.nombre, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Cat: ${producto.categoriaId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Badge estado + menú contextual
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .background(bg, Shape.Pequeno)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color)
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Más opciones", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Menú contextual (DropdownMenu)
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            onClick = { onAjuste(); showMenu = false },
                            text = { Text("Ajustar stock", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                        )
                        DropdownMenuItem(
                            onClick = { onHistorial(); showMenu = false },
                            text = { Text("Ver historial", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                        )
                        if (onDetalle != null) {
                            DropdownMenuItem(
                                onClick = { onDetalle(producto); showMenu = false },
                                text = { Text("Ver detalle", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(imageVector = Icons.Default.RemoveRedEye, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                            )
                        }
                        if (onEditar != null) {
                            DropdownMenuItem(
                                onClick = { onEditar(producto); showMenu = false },
                                text = { Text("Editar producto", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
                            )
                        }
                        androidx.compose.material3.HorizontalDivider()
                        DropdownMenuItem(
                            onClick = { showMenu = false },
                            text = { Text("Cancelar", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                        )
                    }
                }
            }

            // Fila media: Stock actual / mínimo + barra de progreso
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Stock: ${producto.stockActual.formatoCantidad()} / Mín: ${producto.stockMinimo}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Barra de progreso visual
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (porcentaje / 200f).coerceIn(0f, 1f))
                                .height(8.dp)
                                .background(color, Shape.Pequeno)
                            , contentAlignment = Alignment.CenterStart
                        ) {
                            // Indicador de stock mínimo
                            if (producto.stockMinimo > 0) {
                                val minPos = (producto.stockMinimo.toFloat() / (producto.stockMinimo * 2)).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(12.dp)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), Shape.Pequeno)
                                        .offset(x = (minPos * 100).dp)
                                )
                            }
                        }
                    }
                }
            }

            // Fila inferior: Precio + Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(producto.precioVenta.formatoCUP(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)

                Row(horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Xs)) {
                    OutlinedButton(
                        onClick = onHistorial,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = Shape.ExtraGrande
                    ) {
                        Text("Historial", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onAjuste,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = Shape.ExtraGrande
                    ) {
                        Text("Ajuste", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

/**
 * Diálogo de ajuste de inventario (Entrada / Ajuste).
 */
@Composable
fun AjusteInventarioDialog(
    onDismiss: () -> Unit,
    onConfirm: (TipoMovimientoInventario, Int, String?) -> Unit,
    producto: Producto
) {
    val tipo = remember { mutableStateOf(TipoMovimientoInventario.ENTRADA) }
    val cantidadText = remember { mutableStateOf("") }
    val motivoText = remember { mutableStateOf("") }
    val showError = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val cantidad = cantidadText.value.toIntOrNull()
                    if (cantidad != null && cantidad > 0) {
                        onConfirm(tipo.value, cantidad, motivoText.value.takeIf { it.isNotBlank() })
                        onDismiss()
                    } else {
                        showError.value = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = Shape.ExtraGrande
            ) {
                Text("Registrar", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = Shape.ExtraGrande
            ) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        },
        title = { Text("Ajuste de Inventario: ${producto.nombre}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier.padding(StockCubaSpacing.Md).width(320.dp),
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Stock actual: ${producto.stockActual.formatoCantidad()} ${producto.unidadMedida.name.lowercase(java.util.Locale.getDefault())}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Selector tipo movimiento
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
                ) {
                    TipoMovimientoInventario.entries.forEach { t ->
                        val isSelected = tipo.value == t
                        FilterChip(
                            onClick = { tipo.value = t },
                            selected = isSelected,
                            label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = Shape.ExtraGrande,
                            modifier = Modifier.weight(1f).height(44.dp)
                        )
                    }
                }

                // Cantidad
                TextField(
                    value = cantidadText.value,
                    onValueChange = { cantidadText.value = it },
                    label = { Text("Cantidad *", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = StockCubaColors.InputFondo,
                        unfocusedContainerColor = StockCubaColors.InputFondo,
                        errorContainerColor = StockCubaColors.InputFondo,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        errorIndicatorColor = StockCubaColors.CoralAlerta,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = Shape.Grande
                )
                if (showError.value && (cantidadText.value.isEmpty() || cantidadText.value.toIntOrNull() == null || cantidadText.value.toInt() <= 0)) {
                    Text("Ingrese una cantidad válida (>0)", style = MaterialTheme.typography.labelSmall, color = StockCubaColors.CoralAlerta)
                }

                // Motivo opcional
                TextField(
                    value = motivoText.value,
                    onValueChange = { motivoText.value = it },
                    label = { Text("Motivo (opcional)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = StockCubaColors.InputFondo,
                        unfocusedContainerColor = StockCubaColors.InputFondo,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = Shape.Grande
                )
            }
        }
    )
}

/**
 * Estados vacíos/cargando/error.
 */
@Composable
fun InventarioVacio() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)) {
            Card(colors = CardDefaults.cardColors(containerColor = StockCubaColors.VerdeExito.copy(alpha = 0.15f)), shape = Shape.ExtraGrande) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = StockCubaColors.VerdeExito, modifier = Modifier.size(64.dp).padding(StockCubaSpacing.Lg))
            }
            Text("No hay productos", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Agrega productos desde la sección Productos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun InventarioCargando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            Text("Cargando inventario...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InventarioError(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)) {
            Card(colors = CardDefaults.cardColors(containerColor = StockCubaColors.CoralAlertaContainer.copy(alpha = 0.2f)), shape = Shape.Grande) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StockCubaColors.CoralAlerta, modifier = Modifier.size(48.dp).padding(StockCubaSpacing.Lg))
            }
            Text("Error", style = MaterialTheme.typography.headlineSmall, color = StockCubaColors.CoralAlerta)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), shape = Shape.ExtraGrande) {
                Text("Reintentar", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}