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
import androidx.compose.material.icons.automirrored.filled.Logout
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
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.RolUsuario
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.ProductInsight
import cu.stockcuba.app.domain.model.InsightTipo
import cu.stockcuba.app.presentation.security.PinEntryScreen
import cu.stockcuba.app.presentation.security.Mode
import cu.stockcuba.app.presentation.security.RoleSelectionDialog
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
    onNavigateToHistorial: () -> Unit,
    onNavigateToInventario: () -> Unit,
    onNavigateToGastos: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showPinDialog by remember { mutableStateOf(false) }
    var showRoleSwitchDialog by remember { mutableStateOf(false) }
    var forcedRoleForSwitch by remember { mutableStateOf<RolUsuario?>(null) }
    var showCierreDialog by remember { mutableStateOf(false) }
    var pendingCierreRange by remember { mutableStateOf<DashboardTimeRange?>(null) }

    // Mostrar selección de rol al inicio si no está definido
    if (uiState is DashboardUiState.Success) {
        val state = uiState as DashboardUiState.Success
        if (state.rolActual == RolUsuario.UNDEFINED) {
            RoleSelectionDialog(
                onRoleSelected = { rol, nombre ->
                    viewModel.cambiarRolYVendedor(rol, nombre)
                }
            )
        }
    }

    if (showRoleSwitchDialog) {
        RoleSelectionDialog(
            onRoleSelected = { rol, nombre ->
                viewModel.cambiarRolYVendedor(rol, nombre)
                showRoleSwitchDialog = false
                forcedRoleForSwitch = null
            },
            onDismiss = { 
                showRoleSwitchDialog = false
                forcedRoleForSwitch = null
            },
            forcedRole = forcedRoleForSwitch
        )
    }

    if (showCierreDialog && pendingCierreRange != null) {
        val isDueno = (uiState as? DashboardUiState.Success)?.rolActual == RolUsuario.DUENO
        
        AlertDialog(
            onDismissRequest = { showCierreDialog = false },
            title = { Text("Confirmar Cierre de ${if (pendingCierreRange == DashboardTimeRange.MES) "Mes" else "Jornada"}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDueno) {
                        Text("¿Has registrado todos los gastos operativos (luz, salarios, etc.) de este periodo?")
                    } else {
                        Text("¿Confirmas que deseas finalizar la jornada y generar el reporte de ventas?")
                    }
                    Text(
                        "Al cerrar, el balance quedará guardado oficialmente y no podrá ser modificado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val range = pendingCierreRange
                        showCierreDialog = false
                        scope.launch {
                            val result = if (range == DashboardTimeRange.MES) {
                                viewModel.realizarCierreMensual()
                            } else {
                                viewModel.realizarCierreDelDia()
                            }
                            result.fold(
                                onSuccess = { launch { snackbarHostState.showSnackbar("Cierre realizado con éxito") } },
                                onFailure = { launch { snackbarHostState.showSnackbar("Error al realizar cierre") } }
                            )
                        }
                    },
                    shape = Shape.Grande
                ) {
                    Text("Realizar Cierre")
                }
            },
            dismissButton = {
                Row {
                    if (isDueno) {
                        TextButton(onClick = { 
                            showCierreDialog = false
                            onNavigateToGastos()
                        }) {
                            Text("Anotar Gastos")
                        }
                    }
                    TextButton(onClick = { showCierreDialog = false }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

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
                            viewModel.exportarReporteDiario().fold(
                                onSuccess = { uri ->
                                    launch { snackbarHostState.showSnackbar("Reporte guardado en Descargas/StockCuba") }
                                },
                                onFailure = { error ->
                                    val msg = when (error) {
                                        is DomainError.DatabaseError -> error.cause?.message ?: "Error de base de datos"
                                        is DomainError.InvalidOperation -> error.reason
                                        is DomainError.NetworkError -> error.cause?.message ?: "Error de red"
                                        is DomainError.Unknown -> error.message
                                        else -> error.toString()
                                    }
                                    launch { snackbarHostState.showSnackbar("Error: $msg") }
                                }
                            )
                        }
                    },
                    onCierre = {
                        pendingCierreRange = state.timeRange
                        showCierreDialog = true
                    },
                    onNavigateToHistorial = onNavigateToHistorial,
                    onNavigateToInventario = onNavigateToInventario,
                    onNavigateToGastos = onNavigateToGastos,
                    onCambiarRol = { rol ->
                        if (rol == RolUsuario.DUENO) {
                            scope.launch {
                                val hasPin = (viewModel.securityRepository.hasPin() as? Result.Success)?.value ?: false
                                if (hasPin) {
                                    showPinDialog = true
                                } else {
                                    viewModel.cambiarRol(RolUsuario.DUENO)
                                }
                            }
                        } else {
                            // Al cambiar a vendedor desde el menú, pedimos identificación directa
                            forcedRoleForSwitch = RolUsuario.VENDEDOR
                            showRoleSwitchDialog = true
                        }
                    },
                    onCerrarTurno = {
                        forcedRoleForSwitch = null
                        viewModel.cambiarRol(RolUsuario.UNDEFINED)
                    }
                )
            }
        }

        if (showPinDialog) {
            PinEntryScreen(
                mode = Mode.Verify,
                securityRepository = viewModel.securityRepository,
                onResult = { result ->
                    if (result is Result.Success && result.value) {
                        viewModel.cambiarRol(RolUsuario.DUENO)
                    }
                    showPinDialog = false
                }
            )
        }
    }
}

@Composable
fun DashboardContenidoFull(
    state: DashboardUiState.Success,
    onRangeChange: (DashboardTimeRange) -> Unit,
    onExportar: () -> Unit,
    onCierre: () -> Unit,
    onNavigateToHistorial: () -> Unit,
    onNavigateToInventario: () -> Unit,
    onNavigateToGastos: () -> Unit,
    onCambiarRol: (RolUsuario) -> Unit,
    onCerrarTurno: () -> Unit
) {
    val isDueno = state.rolActual == RolUsuario.DUENO

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(StockCubaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
    ) {
        item {
            HeaderDashboardModerno(
                currentRange = state.timeRange,
                rolActual = state.rolActual,
                nombreVendedor = state.nombreVendedor,
                onRangeChange = onRangeChange,
                onExportar = onExportar,
                onCambiarRol = onCambiarRol,
                onCerrarTurno = onCerrarTurno
            )
        }

        // --- 2. META DIARIA (T65) ---
        if (isDueno) {
            item {
                MetaDelDiaCard(
                    progreso = state.progresoMeta,
                    totalActual = state.totalVendido,
                    meta = state.metaVenta
                )
            }
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

        // --- 4b. RENTABILIDAD DEL PERIODO ---
        if (isDueno) {
            item {
                RentabilidadCard(
                    gastos = state.totalGastos,
                    gastosOperativos = state.totalGastosOperativos,
                    ganancia = state.gananciaReal,
                    onNavigateToGastos = onNavigateToGastos
                )
            }
        }

        // --- 5. VALOR DEL INVENTARIO (IPB/IPC) (T66) ---
        if (isDueno) {
            item {
                ValorInventarioCard(
                    ipb = state.valorInventarioVenta,
                    ipc = state.valorInventarioCosto,
                    ganancia = state.gananciaProyectada
                )
            }
        }

        // --- 5b. INSIGHTS DE RENTABILIDAD (DUENO ONLY) ---
        if (isDueno && state.listaInsights.isNotEmpty()) {
            item {
                SeccionInsightsRentabilidad(insights = state.listaInsights)
            }
        }

        // --- 5c. RANKING DE VENDEDORES (DUENO ONLY) ---
        if (isDueno && state.eficienciaVendedores.isNotEmpty()) {
            item {
                RankingVendedoresSection(eficiencia = state.eficienciaVendedores)
            }
        }

        // --- 6. CIERRE DEL DÍA / MES ---
        item {
            val closureLabel = if (state.timeRange == DashboardTimeRange.MES) "Mes" else "Día"
            val isClosed = if (state.timeRange == DashboardTimeRange.MES) 
                state.ultimoCierreMensual != null 
            else 
                state.ultimoCierre != null

            CierreGenericoCard(
                label = closureLabel,
                isClosed = isClosed,
                onCierre = onCierre
            )
        }

        // --- 7. ALERTAS DE STOCK ---
        if (state.listaProductosBajoStock.isNotEmpty()) {
            item {
                AlertaStockBajoModerno(
                    productos = state.listaProductosBajoStock,
                    onClick = onNavigateToInventario
                )
            }
        }

        // --- 8. ACTIVIDAD RECIENTE ---
        item {
            ActividadRecienteSection(
                ventas = state.ventasRecientes,
                onVerTodo = onNavigateToHistorial
            )
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun HeaderDashboardModerno(
    currentRange: DashboardTimeRange,
    rolActual: RolUsuario,
    nombreVendedor: String,
    onRangeChange: (DashboardTimeRange) -> Unit,
    onExportar: () -> Unit,
    onCambiarRol: (RolUsuario) -> Unit,
    onCerrarTurno: () -> Unit
) {
    var showProfileMenu by remember { mutableStateOf(false) }
    val isDueno = rolActual == RolUsuario.DUENO

    Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Panel de Control",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (rolActual == RolUsuario.DUENO) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = Shape.Pequeno,
                        modifier = Modifier.clickable { showProfileMenu = true }
                    ) {
                        Text(
                            text = if (rolActual == RolUsuario.DUENO) "Dueño" else "Vendedor",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Text(
                    text = if (rolActual == RolUsuario.VENDEDOR && nombreVendedor.isNotBlank()) 
                        "Turno de: $nombreVendedor" 
                    else "Estado actual de tu negocio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DropdownMenu(expanded = showProfileMenu, onDismissRequest = { showProfileMenu = false }) {
                    if (rolActual != RolUsuario.DUENO) {
                        DropdownMenuItem(
                            text = { Text("Ver como Dueño") },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) },
                            onClick = { 
                                onCambiarRol(RolUsuario.DUENO)
                                showProfileMenu = false 
                            }
                        )
                    }
                    if (rolActual != RolUsuario.VENDEDOR) {
                        DropdownMenuItem(
                            text = { Text("Ver como Vendedor") },
                            leadingIcon = { Icon(Icons.Default.Sell, null) },
                            onClick = { 
                                onCambiarRol(RolUsuario.VENDEDOR)
                                showProfileMenu = false 
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Cerrar Turno") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                        onClick = {
                            onCerrarTurno()
                            showProfileMenu = false
                        }
                    )
                }
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
                // Vendedor solo ve HOY y SEMANA para evitar cierres mensuales accidentales
                if (isDueno || range != DashboardTimeRange.MES) {
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
fun SeccionInsightsRentabilidad(insights: List<ProductInsight>) {
    Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoGraph, null, tint = StockCubaColors.VerdeExito, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Insights y Rentabilidad", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
            items(insights) { insight ->
                CardInsight(insight)
            }
        }
    }
}

@Composable
fun CardInsight(insight: ProductInsight) {
    val color = when (insight.tipo) {
        InsightTipo.ESTRELLA -> StockCubaColors.VerdeExito
        InsightTipo.ALERTA_MARGEN -> StockCubaColors.CoralAlerta
        InsightTipo.ESTANCADO -> Color(0xFF6366F1)
        InsightTipo.POTENCIAL -> Color(0xFFF59E0B)
    }
    
    val icono = when (insight.tipo) {
        InsightTipo.ESTRELLA -> Icons.Default.RocketLaunch
        InsightTipo.ALERTA_MARGEN -> Icons.Default.WarningAmber
        InsightTipo.ESTANCADO -> Icons.Default.AcUnit
        InsightTipo.POTENCIAL -> Icons.Default.TrendingUp
    }

    Card(
        modifier = Modifier.width(220.dp).height(120.dp),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Md), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = color.copy(alpha = 0.1f), shape = CircleShape) {
                    Icon(icono, null, modifier = Modifier.padding(6.dp).size(14.dp), tint = color)
                }
                Text(insight.valorPrimario, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = color)
            }
            
            Column {
                Text(insight.nombre, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(insight.mensaje, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun GridMetricas(state: DashboardUiState.Success) {
    val isDueno = state.rolActual == RolUsuario.DUENO
    
    val items = mutableListOf(
        MetricItem("Total Ventas", state.totalVendido.formatoCUP(), state.tendenciaTotal, Icons.Default.Payments, StockCubaColors.VerdeExito),
        MetricItem("Cant. Ventas", state.cantidadVentas.toString(), state.tendenciaVentas, Icons.Default.ConfirmationNumber, Color(0xFF6366F1)),
    )
    
    if (isDueno) {
        items.add(MetricItem("Ticket Prom.", state.ticketPromedio.formatoCUP(), "", Icons.Default.TrendingUp, Color(0xFF8B5CF6)))
    }
    
    items.add(MetricItem("Top Producto", state.productoMasVendido?.nombreProducto ?: "—", state.productoMasVendido?.let { "${it.cantidadTotal} vendidos" } ?: "", Icons.Default.Star, Color(0xFFF59E0B)))

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
fun RentabilidadCard(
    gastos: Double, 
    gastosOperativos: Double,
    ganancia: Double,
    onNavigateToGastos: () -> Unit
) {
    val totalEgresos = gastos + gastosOperativos
    val totalVenta = totalEgresos + ganancia
    val pGanancia = if (totalVenta > 0) (ganancia / totalVenta).toFloat() else 0.5f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rentabilidad (Ventas Netas)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                IconButton(onClick = onNavigateToGastos, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(Shape.Full)) {
                // Gastos (Costo Productos)
                Box(modifier = Modifier.fillMaxHeight().weight((gastos / totalVenta.coerceAtLeast(1.0)).toFloat().coerceAtLeast(0.01f)).background(Color(0xFF94A3B8)))
                // Gastos Operativos
                Box(modifier = Modifier.fillMaxHeight().weight((gastosOperativos / totalVenta.coerceAtLeast(1.0)).toFloat().coerceAtLeast(0.01f)).background(StockCubaColors.CoralAlerta))
                // Ganancia en verde
                Box(modifier = Modifier.fillMaxHeight().weight(pGanancia.coerceAtLeast(0.01f)).background(StockCubaColors.VerdeExito))
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
                        Spacer(Modifier.width(8.dp))
                        Text("Costo Productos", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(gastos.formatoCUP(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(StockCubaColors.CoralAlerta))
                        Spacer(Modifier.width(8.dp))
                        Text("Gastos Operativos", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(gastosOperativos.formatoCUP(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = StockCubaColors.CoralAlerta)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ganancia Real", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(StockCubaColors.VerdeExito))
                    }
                    Text(ganancia.formatoCUP(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = StockCubaColors.VerdeExito)
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
fun AlertaStockBajoModerno(productos: List<cu.stockcuba.app.domain.model.Producto>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = StockCubaColors.CoralAlerta.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, StockCubaColors.CoralAlerta.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Md), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = StockCubaColors.CoralAlerta, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Alertas de Inventario (${productos.size})", 
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), 
                        color = StockCubaColors.CoralAlerta
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, 
                    null, 
                    modifier = Modifier.size(16.dp), 
                    tint = StockCubaColors.CoralAlerta.copy(alpha = 0.6f)
                )
            }
            
            productos.take(3).forEach { producto ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(producto.nombre, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("${producto.stockActual} unid.", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = StockCubaColors.CoralAlerta)
                }
            }
            
            if (productos.size > 3) {
                Text(
                    text = "y ${productos.size - 3} productos más...",
                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ActividadRecienteSection(ventas: List<Venta>, onVerTodo: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Actividad Reciente", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            TextButton(onClick = onVerTodo) {
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

@Composable
fun RankingVendedoresSection(eficiencia: List<cu.stockcuba.app.domain.repository.VentaRepository.EficienciaVendedor>) {
    Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Eficiencia de Vendedores", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = Shape.Grande,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(StockCubaSpacing.Md)) {
                // Header
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Vendedor", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Ventas", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Total", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(100.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                
                eficiencia.forEach { vendedor ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = vendedor.nombre.ifBlank { "Desconocido" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = vendedor.cantidadVentas.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = vendedor.totalRecaudado.formatoCUP(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.width(100.dp),
                            textAlign = TextAlign.End,
                            color = StockCubaColors.VerdeExito
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CierreGenericoCard(label: String, isClosed: Boolean, onCierre: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(
            containerColor = if (isClosed) 
                StockCubaColors.VerdeExito.copy(alpha = 0.05f)
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        border = BorderStroke(
            1.dp, 
            if (isClosed) StockCubaColors.VerdeExito.copy(alpha = 0.3f) 
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(StockCubaSpacing.Lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isClosed) "Jornada Cerrada ($label)" else "Finalizar $label",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isClosed) StockCubaColors.VerdeExito else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (isClosed) "Cierre realizado con éxito. El reporte consolidado ya fue generado." 
                    else "Realiza el cierre formal de este periodo y genera el reporte consolidado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isClosed) {
                Surface(
                    color = StockCubaColors.VerdeExito.copy(alpha = 0.1f),
                    shape = Shape.Full
                ) {
                    Icon(
                        Icons.Default.CheckCircle, 
                        null, 
                        modifier = Modifier.padding(8.dp).size(24.dp),
                        tint = StockCubaColors.VerdeExito
                    )
                }
            } else {
                Button(
                    onClick = onCierre,
                    shape = Shape.Mediano,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.LockClock, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar $label")
                }
            }
        }
    }
}

data class MetricItem(val title: String, val value: String, val trend: String, val icon: ImageVector, val color: Color)
