package cu.stockcuba.app.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pantalla Dashboard - Centro de Mando Moderno e Inmersivo.
 */
@Composable
fun DashboardScreen(
    onNavigateToNuevaVenta: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNuevaVenta,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nueva Venta", fontWeight = FontWeight.Bold) },
                containerColor = StockCubaColors.VerdeExito,
                contentColor = Color(0xFF001E1C),
                shape = Shape.Grande
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = StockCubaColors.VerdeExito) }
                is DashboardUiState.Error -> PantallaErrorDashboard(state.message)
                is DashboardUiState.Success -> DashboardContenidoFull(
                    state = state,
                    onRangeChange = { viewModel.setTimeRange(it) },
                    onExportar = {
                        scope.launch {
                            viewModel.exportarReporteDiario().onSuccess {
                                launch { snackbarHostState.showSnackbar("Reporte guardado en Descargas/StockCuba") }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardContenidoFull(
    state: DashboardUiState.Success,
    onRangeChange: (DashboardTimeRange) -> Unit,
    onExportar: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(StockCubaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
    ) {
        // --- 1. SALUDO Y SELECTOR ---
        item {
            HeaderDashboardModerno(
                currentRange = state.timeRange,
                onRangeChange = onRangeChange,
                onExportar = onExportar
            )
        }

        // --- 2. META DIARIA (T65) ---
        item {
            MetaDelDiaCard(
                progreso = state.progresoMeta,
                totalActual = state.totalVendido,
                meta = state.metaVenta
            )
        }

        // --- 3. MÉTRICAS PRINCIPALES ---
        item {
            GridMetricas(state)
        }

        // --- 4. BALANCE DE PAGOS ---
        item {
            BalancePagosCard(
                efectivo = state.montoEfectivo,
                transferencia = state.montoTransferencia
            )
        }

        // --- 5. VALOR DEL INVENTARIO (IPB/IPC) (T66) ---
        item {
            ValorInventarioCard(
                ipb = state.valorInventarioVenta,
                ipc = state.valorInventarioCosto,
                ganancia = state.gananciaProyectada
            )
        }

        // --- 6. ALERTAS DE STOCK ---
        if (state.listaProductosBajoStock.isNotEmpty()) {
            item {
                AlertaStockBajoModerno(productos = state.listaProductosBajoStock)
            }
        }

        // --- 6. ACTIVIDAD RECIENTE ---
        item {
            ActividadRecienteSection(ventas = state.ventasRecientes)
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun HeaderDashboardModerno(
    currentRange: DashboardTimeRange,
    onRangeChange: (DashboardTimeRange) -> Unit,
    onExportar: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Panel de Control",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Estado actual de tu negocio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExportar) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Exportar Excel",
                            modifier = Modifier.padding(8.dp).size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(Modifier.width(8.dp))
                
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                    shadowElevation = 2.dp
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = cu.stockcuba.app.R.mipmap.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.padding(4.dp).fillMaxSize()
                    )
                }
            }
        }

        // Selector de Rango (Chips)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)) {
            items(DashboardTimeRange.entries) { range ->
                val isSelected = currentRange == range
                FilterChip(
                    selected = isSelected,
                    onClick = { onRangeChange(range) },
                    label = { Text(range.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    shape = Shape.Full,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun MetaDelDiaCard(progreso: Float, totalActual: Double, meta: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Crecimiento vs Ayer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (totalActual >= meta) "¡Meta Superada! 🚀" else "Camino a la meta",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "${(progreso * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (progreso >= 1f) StockCubaColors.VerdeExito else MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Barra de progreso animada
            val animProgreso by animateFloatAsState(
                targetValue = progreso.coerceIn(0f, 1f),
                animationSpec = tween(1000, easing = FastOutSlowInEasing)
            )
            
            LinearProgressIndicator(
                progress = { animProgreso },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(Shape.Full),
                color = if (progreso >= 1f) StockCubaColors.VerdeExito else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            if (meta > 0) {
                Text(
                    text = "Ayer vendiste ${meta.formatoCUP()}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GridMetricas(state: DashboardUiState.Success) {
    val items = listOf(
        MetricItem("Total Ventas", state.totalVendido.formatoCUP(), state.tendenciaTotal, Icons.Default.Payments, StockCubaColors.VerdeExito),
        MetricItem("Cant. Ventas", state.cantidadVentas.toString(), state.tendenciaVentas, Icons.Default.ConfirmationNumber, Color(0xFF6366F1)),
        MetricItem("Ticket Prom.", state.ticketPromedio.formatoCUP(), "", Icons.Default.TrendingUp, Color(0xFF8B5CF6)),
        MetricItem("Top Producto", state.productoMasVendido?.nombreProducto ?: "—", state.productoMasVendido?.let { "${it.cantidadTotal} vendidos" } ?: "", Icons.Default.Star, Color(0xFFF59E0B))
    )

    Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        items.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                row.forEach { item ->
                    CardMetricaModerna(item, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CardMetricaModerna(item: MetricItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(130.dp),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Md), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = item.color.copy(alpha = 0.1f), shape = CircleShape) {
                    Icon(item.icon, null, modifier = Modifier.padding(6.dp).size(16.dp), tint = item.color)
                }
                if (item.trend.isNotEmpty()) {
                    Text(
                        item.trend, 
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (item.trend.startsWith("+")) StockCubaColors.VerdeExito else if (item.trend.startsWith("-")) StockCubaColors.CoralAlerta else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column {
                Text(
                    text = item.value, 
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = item.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BalancePagosCard(efectivo: Double, transferencia: Double) {
    val total = efectivo + transferencia
    val pEfectivo = if (total > 0) (efectivo / total).toFloat() else 0.5f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Lg)) {
            Text("Balance de Caja", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(Shape.Full)) {
                Box(modifier = Modifier.fillMaxHeight().weight(pEfectivo.coerceAtLeast(0.01f)).background(Color(0xFF2DD4BF)))
                Box(modifier = Modifier.fillMaxHeight().weight((1f - pEfectivo).coerceAtLeast(0.01f)).background(Color(0xFF6366F1)))
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2DD4BF)))
                        Spacer(Modifier.width(8.dp))
                        Text("Efectivo", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(efectivo.formatoCUP(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Transferencia", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF6366F1)))
                    }
                    Text(transferencia.formatoCUP(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun ValorInventarioCard(ipb: Double, ipc: Double, ganancia: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Lg)) {
            Text("Valor del Inventario", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("IPB (Venta)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ipb.formatoCUP(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("IPC (Costo)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(ipc.formatoCUP(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ganancia Proyectada", style = MaterialTheme.typography.bodyMedium)
                Text(ganancia.formatoCUP(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = StockCubaColors.VerdeExito)
            }
        }
    }
}

@Composable
fun AlertaStockBajoModerno(productos: List<cu.stockcuba.app.domain.model.Producto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = StockCubaColors.CoralAlerta.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, StockCubaColors.CoralAlerta.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Md), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = StockCubaColors.CoralAlerta, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Alertas de Inventario (${productos.size})", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = StockCubaColors.CoralAlerta)
            }
            
            productos.take(3).forEach { producto ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(producto.nombre, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("${producto.stockActual} unid.", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = StockCubaColors.CoralAlerta)
                }
            }
        }
    }
}

@Composable
fun ActividadRecienteSection(ventas: List<Venta>) {
    Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Actividad Reciente", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            TextButton(onClick = { /* Navegar a historial */ }) {
                Text("Ver todo")
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
            }
        }
        
        if (ventas.isEmpty()) {
            Text("No hay ventas registradas todavía", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            ventas.forEach { venta ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(StockCubaSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), shape = CircleShape) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, modifier = Modifier.padding(8.dp).size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Venta #${venta.id.take(6).uppercase()}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(venta.metodoPago.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(venta.total.formatoCUP(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaErrorDashboard(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = StockCubaColors.CoralAlerta)
            Text(message, textAlign = TextAlign.Center)
        }
    }
}

data class MetricItem(val title: String, val value: String, val trend: String, val icon: ImageVector, val color: Color)
