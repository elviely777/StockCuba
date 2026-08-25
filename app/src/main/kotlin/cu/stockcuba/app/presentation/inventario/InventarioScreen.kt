package cu.stockcuba.app.presentation.inventario

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    onAjuste: (Producto) -> Unit,
    onHistorial: (Producto) -> Unit,
    viewModel: InventarioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            InventarioHeader(
                totalArticulos = (uiState as? InventarioUiState.Success)?.totalArticulos ?: 0,
                query = (uiState as? InventarioUiState.Success)?.query ?: "",
                onQueryChange = { viewModel.setQuery(it) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is InventarioUiState.Loading -> InventarioCargando()
                is InventarioUiState.Error -> InventarioError(state.message, onRetry = { viewModel.limpiarFiltros() })
                is InventarioUiState.Success -> {
                    // ===== 1. RESUMEN RÁPIDO (OK, BAJO, SIN) =====
                    ResumenInventarioModerno(
                        ok = state.totalOk,
                        bajo = state.totalBajo,
                        sin = state.totalSinStock
                    )

                    // ===== 2. FILTROS DE ESTADO (HORIZONTAL) =====
                    FiltrosStockBarra(
                        filtroActual = state.filtroStock,
                        onFiltroChange = { viewModel.setFiltroStock(it) }
                    )

                    // ===== 3. LISTA DE PRODUCTOS =====
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (state.productos.isEmpty()) {
                            InventarioVacio(
                                isFiltered = state.query.isNotEmpty() || state.filtroStock != FiltroStock.TODOS,
                                onClear = { viewModel.limpiarFiltros() }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(StockCubaSpacing.Lg),
                                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
                            ) {
                                items(state.productos, key = { it.producto.id }) { item ->
                                    ProductoInventarioCardModerno(
                                        item = item,
                                        onAjuste = { onAjuste(item.producto) },
                                        onHistorial = { onHistorial(item.producto) }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(40.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioHeader(
    totalArticulos: Int,
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StockCubaSpacing.Lg, vertical = StockCubaSpacing.Md)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Inventario",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        "$totalArticulos artículos en catálogo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = Shape.Full
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).size(24.dp)
                    )
                }
            }

            // Barra de búsqueda moderna
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar por nombre...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(18.dp))
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = Shape.Grande,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun ResumenInventarioModerno(ok: Int, bajo: Int, sin: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = StockCubaSpacing.Lg, vertical = StockCubaSpacing.Md),
        horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
    ) {
        MetricInventario(
            label = "OK",
            count = ok,
            color = StockCubaColors.VerdeExito,
            modifier = Modifier.weight(1f)
        )
        MetricInventario(
            label = "BAJO",
            count = bajo,
            color = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
        MetricInventario(
            label = "CRÍTICO",
            count = sin,
            color = StockCubaColors.CoralAlerta,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MetricInventario(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(64.dp),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = color)
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun FiltrosStockBarra(
    filtroActual: FiltroStock,
    onFiltroChange: (FiltroStock) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = StockCubaSpacing.Lg, vertical = StockCubaSpacing.Sm),
        horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
    ) {
        items(FiltroStock.entries) { filtro ->
            val isSelected = filtroActual == filtro
            val color = when (filtro) {
                FiltroStock.OK -> StockCubaColors.VerdeExito
                FiltroStock.BAJO -> Color(0xFFF59E0B)
                FiltroStock.SIN_STOCK -> StockCubaColors.CoralAlerta
                FiltroStock.TODOS -> MaterialTheme.colorScheme.primary
            }

            Surface(
                onClick = { onFiltroChange(filtro) },
                modifier = Modifier.height(34.dp),
                shape = Shape.Full,
                color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (isSelected) color else Color.Transparent)
            ) {
                Text(
                    text = when(filtro) {
                        FiltroStock.TODOS -> "Todos"
                        FiltroStock.OK -> "Existencia OK"
                        FiltroStock.BAJO -> "Stock Bajo"
                        FiltroStock.SIN_STOCK -> "Agotados"
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProductoInventarioCardModerno(
    item: ProductoConStock,
    onAjuste: () -> Unit,
    onHistorial: () -> Unit
) {
    val statusColor = item.stockStatusColor()
    val statusBg = item.stockStatusBackground()
    val statusLabel = item.stockStatusLabel()
    val producto = item.producto

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Barra lateral de estado
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(statusColor)
            )

            Column(modifier = Modifier.padding(StockCubaSpacing.Md).weight(1f)) {
                // Cabecera: Nombre + Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = producto.nombre,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Unidad: ${producto.unidadMedida.name.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = statusBg,
                        shape = Shape.Full
                    ) {
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Info de Stock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Existencia Actual",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = producto.stockActual.formatoCantidad(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (item.stockStatus == StockStatus.SIN_STOCK) StockCubaColors.CoralAlerta else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Mínimo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = producto.stockMinimo.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Acciones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
                ) {
                    OutlinedButton(
                        onClick = onHistorial,
                        modifier = Modifier.weight(1f),
                        shape = Shape.Grande,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Historial", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onAjuste,
                        modifier = Modifier.weight(1f),
                        shape = Shape.Grande,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ajustar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun InventarioVacio(isFiltered: Boolean, onClear: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = Shape.Full,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isFiltered) Icons.Default.SearchOff else Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (isFiltered) "Sin resultados" else "Sin existencias",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            if (isFiltered) "Prueba con otro término o filtro de estado" else "No hay productos con inventario registrado todavía.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isFiltered) {
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onClear) {
                Text("Limpiar todos los filtros")
            }
        }
    }
}

@Composable
fun InventarioCargando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun InventarioError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = StockCubaColors.CoralAlerta)
        Spacer(Modifier.height(16.dp))
        Text("Error de Inventario", style = MaterialTheme.typography.titleLarge)
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, shape = Shape.Grande) { Text("Volver a intentar") }
    }
}
