package cu.stockcuba.app.presentation.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.InsightTipo
import cu.stockcuba.app.domain.model.Moneda
import cu.stockcuba.app.domain.model.ProductInsight
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.RolUsuario
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.presentation.common.ScIconButton
import cu.stockcuba.app.presentation.common.ScLabel
import cu.stockcuba.app.presentation.common.ScLevelBar
import cu.stockcuba.app.presentation.common.ScMetricCell
import cu.stockcuba.app.presentation.common.ScMoney
import cu.stockcuba.app.presentation.common.ScPanel
import cu.stockcuba.app.presentation.common.ScSectionHeader
import cu.stockcuba.app.presentation.common.ScStatusPill
import cu.stockcuba.app.presentation.common.ScTone
import cu.stockcuba.app.presentation.security.Mode
import cu.stockcuba.app.presentation.security.PinEntryScreen
import cu.stockcuba.app.presentation.security.RoleSelectionDialog
import cu.stockcuba.app.presentation.theme.ScColors
import cu.stockcuba.app.presentation.theme.Shape
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Panel de Control · Centro de mando del negocio.
 *
 * Jerarquía: primero el dinero del periodo, después la caja, después los
 * indicadores del dueño (rentabilidad, inventario, tendencia), y al final lo
 * accionable: stock bajo, actividad reciente y cierre.
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
            title = {
                Text(
                    "Confirmar cierre de ${if (pendingCierreRange == DashboardTimeRange.MES) "mes" else "jornada"}",
                    fontWeight = FontWeight.Bold
                )
            },
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
                TextButton(
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
                    }
                ) {
                    Text("Realizar cierre")
                }
            },
            dismissButton = {
                Row {
                    if (isDueno) {
                        TextButton(onClick = {
                            showCierreDialog = false
                            onNavigateToGastos()
                        }) {
                            Text("Anotar gastos")
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
        containerColor = ScColors.canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToNuevaVenta,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nueva venta", fontWeight = FontWeight.SemiBold) },
                containerColor = ScColors.acento,
                contentColor = ScColors.sobreAcento,
                shape = Shape.Mediano
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ScColors.canvas)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ScColors.acento)
                }
                is DashboardUiState.Error -> PantallaErrorDashboard(state.message)
                is DashboardUiState.Success -> DashboardContenidoFull(
                    state = state,
                    onRangeChange = { viewModel.setTimeRange(it) },
                    onExportar = {
                        scope.launch {
                            viewModel.exportarReporteDiario().fold(
                                onSuccess = { _ ->
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

// =====================================================================================
// CONTENIDO
// =====================================================================================

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
    val moneda = state.monedaBase

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CabeceraPanel(
                currentRange = state.timeRange,
                rolActual = state.rolActual,
                nombreVendedor = state.nombreVendedor,
                onRangeChange = onRangeChange,
                onExportar = onExportar,
                onCambiarRol = onCambiarRol,
                onCerrarTurno = onCerrarTurno
            )
        }

        // 1. El dinero del periodo: la cifra protagonista.
        item { HeroVentaPanel(state = state, moneda = moneda) }

        // 2. La caja de hoy: cobros pendientes y cómo entró el dinero.
        item { CajaPanel(state = state, moneda = moneda, isDueno = isDueno) }

        // 3. Tendencia (solo dueño).
        if (isDueno && state.ventasSemanales.isNotEmpty()) {
            item { TendenciaPanel(data = state.ventasSemanales, moneda = moneda) }
        }

        // 4. Rentabilidad del periodo (solo dueño).
        if (isDueno) {
            item {
                RentabilidadPanel(
                    costos = state.totalGastos,
                    gastosOperativos = state.totalGastosOperativos,
                    ganancia = state.gananciaReal,
                    moneda = moneda,
                    onNavigateToGastos = onNavigateToGastos
                )
            }
        }

        // 5. Valor del inventario (solo dueño).
        if (isDueno) {
            item {
                ValorInventarioPanel(
                    ipb = state.valorInventarioVenta,
                    ipc = state.valorInventarioCosto,
                    ganancia = state.gananciaProyectada,
                    moneda = moneda
                )
            }
        }

        // 6. Insights de rentabilidad (solo dueño).
        if (isDueno && state.listaInsights.isNotEmpty()) {
            item { InsightsRow(insights = state.listaInsights) }
        }

        // 7. Ranking de vendedores (solo dueño).
        if (isDueno && state.eficienciaVendedores.isNotEmpty()) {
            item { RankingPanel(eficiencia = state.eficienciaVendedores, moneda = moneda) }
        }

        // 8. Stock bajo: lo más accionable, con enlace a inventario.
        if (state.listaProductosBajoStock.isNotEmpty()) {
            item {
                StockBajoPanel(
                    productos = state.listaProductosBajoStock,
                    onClick = onNavigateToInventario
                )
            }
        }

        // 9. Actividad reciente.
        item {
            ActividadPanel(
                ventas = state.ventasRecientes,
                moneda = moneda,
                onVerTodo = onNavigateToHistorial
            )
        }

        // 10. Cierre del periodo.
        item {
            val isClosed = if (state.timeRange == DashboardTimeRange.MES) {
                state.ultimoCierreMensual != null
            } else {
                state.ultimoCierre != null
            }
            CierrePanel(
                label = if (state.timeRange == DashboardTimeRange.MES) "mes" else "día",
                isClosed = isClosed,
                onCierre = onCierre
            )
        }
    }
}

// =====================================================================================
// CABECERA
// =====================================================================================

@Composable
private fun CabeceraPanel(
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

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Panel",
                    style = MaterialTheme.typography.headlineLarge,
                    color = ScColors.texto
                )
                Text(
                    text = if (rolActual == RolUsuario.VENDEDOR && nombreVendedor.isNotBlank()) {
                        "Turno de $nombreVendedor"
                    } else {
                        "El estado de tu negocio, hoy"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScColors.textoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                Surface(
                    onClick = { showProfileMenu = true },
                    shape = Shape.Full,
                    color = ScColors.panelAlto,
                    border = BorderStroke(1.dp, ScColors.borde)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(ScColors.acento, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isDueno) "Dueño" else "Vendedor",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ScColors.texto
                        )
                    }
                }

                DropdownMenu(expanded = showProfileMenu, onDismissRequest = { showProfileMenu = false }) {
                    if (rolActual != RolUsuario.DUENO) {
                        DropdownMenuItem(
                            text = { Text("Ver como dueño") },
                            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) },
                            onClick = {
                                onCambiarRol(RolUsuario.DUENO)
                                showProfileMenu = false
                            }
                        )
                    }
                    if (rolActual != RolUsuario.VENDEDOR) {
                        DropdownMenuItem(
                            text = { Text("Ver como vendedor") },
                            leadingIcon = { Icon(Icons.Default.Sell, null) },
                            onClick = {
                                onCambiarRol(RolUsuario.VENDEDOR)
                                showProfileMenu = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Cerrar turno") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                        onClick = {
                            onCerrarTurno()
                            showProfileMenu = false
                        }
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            ScIconButton(
                icon = Icons.Default.Download,
                contentDescription = "Descargar reporte",
                onClick = onExportar
            )
        }

        Spacer(Modifier.height(14.dp))

        SelectorRango(
            current = currentRange,
            isDueno = isDueno,
            onRangeChange = onRangeChange
        )
    }
}

/** Control segmentado de periodo: el activo se marca con borde jade, no con un bloque de color. */
@Composable
private fun SelectorRango(
    current: DashboardTimeRange,
    isDueno: Boolean,
    onRangeChange: (DashboardTimeRange) -> Unit
) {
    val opciones = DashboardTimeRange.entries.filter { isDueno || it != DashboardTimeRange.MES }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(ScColors.panel, Shape.Mediano)
            .border(1.dp, ScColors.borde, Shape.Mediano)
            .padding(3.dp)
    ) {
        opciones.forEach { range ->
            val selected = range == current
            Surface(
                onClick = { onRangeChange(range) },
                modifier = Modifier.weight(1f),
                shape = Shape.Pequeno,
                color = if (selected) ScColors.panelAlto else Color.Transparent,
                border = if (selected) BorderStroke(1.dp, ScColors.acento.copy(alpha = 0.45f)) else null
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) ScColors.acento else ScColors.textoSecundario
                    )
                }
            }
        }
    }
}

// =====================================================================================
// 1. HERO: EL DINERO DEL PERIODO
// =====================================================================================

@Composable
private fun HeroVentaPanel(state: DashboardUiState.Success, moneda: Moneda) {
    val etiqueta = when (state.timeRange) {
        DashboardTimeRange.HOY -> "Vendido hoy"
        DashboardTimeRange.SEMANA -> "Vendido esta semana"
        DashboardTimeRange.MES -> "Vendido este mes"
    }
    val tendencia = state.tendenciaTotal
    val tone = when {
        tendencia.startsWith("+") -> ScTone.Positive
        tendencia.startsWith("-") -> ScTone.Danger
        else -> ScTone.Neutral
    }
    val meta = state.metaVenta
    val progreso = if (meta > 0) (state.totalVendido / meta).toFloat() else 0f
    val superada = progreso >= 1f

    ScPanel(
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScLabel(etiqueta)
            Spacer(Modifier.weight(1f))
            ScStatusPill(
                text = when {
                    tendencia.isBlank() || tendencia == "—" -> "Sin referencia"
                    tendencia.startsWith("+") -> "Crece $tendencia vs ayer"
                    tendencia.startsWith("-") -> "Baja $tendencia vs ayer"
                    else -> tendencia
                },
                tone = tone
            )
        }

        Spacer(Modifier.height(10.dp))

        ScMoney(
            value = monto(state.totalVendido),
            unit = moneda.name,
            style = MaterialTheme.typography.displayLarge,
            color = ScColors.dinero
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column {
                ScLabel("Ventas")
                Spacer(Modifier.height(2.dp))
                Text(
                    text = state.cantidadVentas.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = ScColors.texto
                )
            }
            Column {
                ScLabel("Ticket promedio")
                Spacer(Modifier.height(2.dp))
                Text(
                    text = monto(state.ticketPromedio),
                    style = MaterialTheme.typography.titleLarge,
                    color = ScColors.texto
                )
            }
        }

        if (meta > 0) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScLabel(if (superada) "Superaste ayer" else "Contra lo de ayer")
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(progreso.coerceIn(0f, 9.99f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (superada) ScColors.acento else ScColors.dinero
                )
            }
            Spacer(Modifier.height(6.dp))
            ScLevelBar(
                progress = progreso,
                tone = if (superada) ScTone.Positive else ScTone.Money
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Ayer: ${monto(meta)} ${moneda.name}",
                style = MaterialTheme.typography.bodySmall,
                color = ScColors.textoDebil
            )
        }
    }
}

// =====================================================================================
// 2. CAJA: COBROS Y FORMA DE PAGO
// =====================================================================================

@Composable
private fun CajaPanel(state: DashboardUiState.Success, moneda: Moneda, isDueno: Boolean) {
    val totalCaja = state.montoEfectivo + state.montoTransferencia
    val pEfectivo = if (totalCaja > 0) (state.montoEfectivo / totalCaja).toFloat() else 0f

    ScPanel(
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            ScMetricCell(
                label = "Por cobrar",
                value = monto(state.totalPorCobrar),
                caption = moneda.name,
                tone = if (state.totalPorCobrar > 0) ScTone.Money else ScTone.Neutral,
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(ScColors.borde)
            )
            ScMetricCell(
                label = if (isDueno) "Ganancia real" else "Efectivo",
                value = monto(if (isDueno) state.gananciaReal else state.montoEfectivo),
                caption = moneda.name,
                tone = ScTone.Positive,
                icon = Icons.Default.TrendingUp,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ScColors.borde)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            ScMetricCell(
                label = "Efectivo",
                value = monto(state.montoEfectivo),
                caption = moneda.name,
                tone = ScTone.Positive,
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(ScColors.borde)
            )
            ScMetricCell(
                label = "Transferencia",
                value = monto(state.montoTransferencia),
                caption = moneda.name,
                tone = ScTone.Info,
                icon = Icons.Default.AccountBalance,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ScColors.borde)
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ScLabel("Cómo entró el dinero")
                Spacer(Modifier.weight(1f))
                Text(
                    text = when {
                        totalCaja <= 0.0 -> "Sin cobros"
                        pEfectivo >= 1f -> "Todo efectivo"
                        pEfectivo <= 0f -> "Todo transferencia"
                        else -> "${(pEfectivo * 100).toInt()}% efectivo"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = ScColors.textoSecundario
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(Shape.Full)
                    .background(ScColors.panelHundido)
            ) {
                if (totalCaja > 0.0 && pEfectivo > 0f) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .weight(pEfectivo)
                            .background(ScColors.acento)
                    )
                }
                if (totalCaja > 0.0 && pEfectivo < 1f) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .weight(1f - pEfectivo)
                            .background(ScColors.info)
                    )
                }
            }
        }
    }
}

// =====================================================================================
// 3. TENDENCIA (7 DÍAS)
// =====================================================================================

@Composable
private fun TendenciaPanel(data: List<Pair<String, Double>>, moneda: Moneda) {
    val model = remember(data) {
        entryModelOf(data.mapIndexed { index, pair -> entryOf(index, pair.second) })
    }
    val totalSemana = data.sumOf { it.second }

    ScPanel(
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Últimos 7 días",
                    style = MaterialTheme.typography.titleMedium,
                    color = ScColors.texto
                )
                Text(
                    text = "${monto(totalSemana)} ${moneda.name} en total",
                    style = MaterialTheme.typography.bodySmall,
                    color = ScColors.textoSecundario
                )
            }
            Icon(
                imageVector = Icons.Default.AutoGraph,
                contentDescription = null,
                tint = ScColors.acento,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        if (totalSemana <= 0.0) {
            Text(
                text = "Sin ventas en este periodo. El gráfico se llena a medida que cobras.",
                style = MaterialTheme.typography.bodySmall,
                color = ScColors.textoDebil
            )
        } else {
            Chart(
                chart = columnChart(
                    columns = listOf(
                        com.patrykandpatrick.vico.core.component.shape.LineComponent(
                            color = ScColors.acento.toArgb(),
                            thicknessDp = 14f,
                            shape = com.patrykandpatrick.vico.core.component.shape.Shapes.roundedCornerShape(
                                allPercent = 30
                            )
                        )
                    )
                ),
                model = model,
                startAxis = null,
                bottomAxis = rememberBottomAxis(
                    label = com.patrykandpatrick.vico.compose.axis.axisLabelComponent(textSize = 10.sp),
                    valueFormatter = { value, _ -> data.getOrNull(value.toInt())?.first ?: "" },
                    guideline = null
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )
        }
    }
}

// =====================================================================================
// 4. RENTABILIDAD (DUEÑO)
// =====================================================================================

@Composable
private fun RentabilidadPanel(
    costos: Double,
    gastosOperativos: Double,
    ganancia: Double,
    moneda: Moneda,
    onNavigateToGastos: () -> Unit
) {
    val total = costos + gastosOperativos + ganancia
    val margen = if (total > 0) (ganancia / total).toFloat() else 0f

    ScPanel(
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Rentabilidad",
                style = MaterialTheme.typography.titleMedium,
                color = ScColors.texto
            )
            Spacer(Modifier.weight(1f))
            ScStatusPill(
                text = "${(margen * 100).toInt()}% de margen",
                tone = if (margen >= 0.3f) ScTone.Positive else ScTone.Money
            )
            Spacer(Modifier.width(4.dp))
            ScIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Anotar gasto",
                onClick = onNavigateToGastos
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(Shape.Full)
                .background(ScColors.panelHundido)
        ) {
            if (costos > 0) {
                Box(
                    Modifier.fillMaxHeight().weight((costos / total.coerceAtLeast(1.0)).toFloat()).background(
                        ScColors.textoDebil
                    )
                )
            }
            if (gastosOperativos > 0) {
                Box(
                    Modifier.fillMaxHeight()
                        .weight((gastosOperativos / total.coerceAtLeast(1.0)).toFloat())
                        .background(ScColors.alerta)
                )
            }
            if (ganancia > 0) {
                Box(
                    Modifier.fillMaxHeight()
                        .weight((ganancia / total.coerceAtLeast(1.0)).toFloat())
                        .background(ScColors.acento)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        FilaDato("Costo de productos", costos, moneda, ScColors.textoSecundario)
        Spacer(Modifier.height(8.dp))
        FilaDato("Gastos operativos", gastosOperativos, moneda, ScColors.alerta)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ScColors.borde)
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Ganancia real",
                style = MaterialTheme.typography.titleSmall,
                color = ScColors.texto
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${monto(ganancia)} ${moneda.name}",
                style = MaterialTheme.typography.titleMedium,
                color = ScColors.acento
            )
        }
    }
}

@Composable
private fun FilaDato(label: String, valor: Double, moneda: Moneda, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        ScLabel(label)
        Spacer(Modifier.weight(1f))
        Text(
            text = "${monto(valor)} ${moneda.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = ScColors.textoSecundario
        )
    }
}

// =====================================================================================
// 5. VALOR DEL INVENTARIO (DUEÑO)
// =====================================================================================

@Composable
private fun ValorInventarioPanel(ipb: Double, ipc: Double, ganancia: Double, moneda: Moneda) {
    ScPanel(
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Valor del inventario",
                    style = MaterialTheme.typography.titleMedium,
                    color = ScColors.texto
                )
                Spacer(Modifier.weight(1f))
                ScStatusPill(text = "Proyectado", tone = ScTone.Neutral)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            ScMetricCell(
                label = "A precio de venta",
                value = monto(ipb),
                caption = moneda.name,
                tone = ScTone.Neutral,
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(ScColors.borde)
            )
            ScMetricCell(
                label = "A precio de costo",
                value = monto(ipc),
                caption = moneda.name,
                tone = ScTone.Neutral,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ScColors.borde)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ganancia proyectada",
                style = MaterialTheme.typography.bodyMedium,
                color = ScColors.textoSecundario
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${monto(ganancia)} ${moneda.name}",
                style = MaterialTheme.typography.titleMedium,
                color = ScColors.acento
            )
        }
    }
}

// =====================================================================================
// 6. INSIGHTS
// =====================================================================================

@Composable
private fun InsightsRow(insights: List<ProductInsight>) {
    Column {
        ScSectionHeader(title = "Insights del negocio")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(insights) { insight ->
                CardInsight(insight)
            }
        }
    }
}

@Composable
private fun CardInsight(insight: ProductInsight) {
    val color = when (insight.tipo) {
        InsightTipo.ESTRELLA -> ScColors.acento
        InsightTipo.ALERTA_MARGEN -> ScColors.alerta
        InsightTipo.ESTANCADO -> ScColors.info
        InsightTipo.POTENCIAL -> ScColors.dinero
    }
    val icono = when (insight.tipo) {
        InsightTipo.ESTRELLA -> Icons.Default.RocketLaunch
        InsightTipo.ALERTA_MARGEN -> Icons.Default.WarningAmber
        InsightTipo.ESTANCADO -> Icons.Default.LockClock
        InsightTipo.POTENCIAL -> Icons.Default.TrendingUp
    }

    Surface(
        modifier = Modifier.width(216.dp).height(124.dp),
        shape = Shape.Grande,
        color = ScColors.panel,
        border = BorderStroke(1.dp, ScColors.borde)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(color.copy(alpha = 0.14f), Shape.Pequeno),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, null, modifier = Modifier.size(14.dp), tint = color)
                }
                Text(
                    text = insight.valorPrimario,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = color
                )
            }
            Column {
                Text(
                    text = insight.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = ScColors.texto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = insight.mensaje,
                    style = MaterialTheme.typography.bodySmall,
                    color = ScColors.textoSecundario,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =====================================================================================
// 7. RANKING DE VENDEDORES (DUEÑO)
// =====================================================================================

@Composable
private fun RankingPanel(
    eficiencia: List<cu.stockcuba.app.domain.repository.VentaRepository.EficienciaVendedor>,
    moneda: Moneda
) {
    Column {
        ScSectionHeader(title = "Rendimiento por vendedor")
        ScPanel(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                ScLabel("Vendedor", Modifier.weight(1f))
                ScLabel("Ventas", Modifier.width(64.dp))
                ScLabel("Total", Modifier.width(96.dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ScColors.borde)
            )
            eficiencia.forEach { vendedor ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(ScColors.panelAlto, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = vendedor.nombre.take(1).uppercase().ifBlank { "?" },
                                style = MaterialTheme.typography.labelMedium,
                                color = ScColors.acento
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = vendedor.nombre.ifBlank { "Desconocido" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ScColors.texto,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = vendedor.cantidadVentas.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ScColors.textoSecundario,
                        modifier = Modifier.width(64.dp)
                    )
                    Text(
                        text = monto(vendedor.totalRecaudado),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ScColors.texto,
                        modifier = Modifier.width(96.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

// =====================================================================================
// 8. STOCK BAJO
// =====================================================================================

@Composable
private fun StockBajoPanel(
    productos: List<cu.stockcuba.app.domain.model.Producto>,
    onClick: () -> Unit
) {
    ScPanel(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clickable { onClick() },
        borderColor = ScColors.alerta.copy(alpha = 0.35f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(ScColors.alerta.copy(alpha = 0.14f), Shape.Pequeno),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = ScColors.alerta,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stock bajo",
                    style = MaterialTheme.typography.titleSmall,
                    color = ScColors.texto
                )
                Text(
                    text = "${productos.size} ${if (productos.size == 1) "producto necesita" else "productos necesitan"} reposición",
                    style = MaterialTheme.typography.bodySmall,
                    color = ScColors.textoSecundario
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = ScColors.textoDebil,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        productos.take(3).forEachIndexed { index, producto ->
            if (index > 0) {
                Spacer(Modifier.height(10.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScColors.texto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (producto.stockActual <= 0) "Agotado" else "${producto.stockActual} unid.",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ScColors.alerta
                )
            }
        }

        if (productos.size > 3) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "y ${productos.size - 3} más",
                style = MaterialTheme.typography.bodySmall,
                color = ScColors.textoDebil
            )
        }
    }
}

// =====================================================================================
// 9. ACTIVIDAD RECIENTE
// =====================================================================================

@Composable
private fun ActividadPanel(ventas: List<Venta>, moneda: Moneda, onVerTodo: () -> Unit) {
    Column {
        ScSectionHeader(
            title = "Actividad reciente",
            actionLabel = if (ventas.isEmpty()) null else "Ver todo",
            onAction = onVerTodo
        )
        if (ventas.isEmpty()) {
            ScPanel(
                modifier = Modifier.padding(horizontal = 20.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    text = "Aún no hay ventas registradas en este periodo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ScColors.textoSecundario
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Cada cobro aparecerá aquí con su hora y su método de pago.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ScColors.textoDebil
                )
            }
        } else {
            ScPanel(
                modifier = Modifier.padding(horizontal = 20.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                ventas.take(4).forEachIndexed { index, venta ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(ScColors.borde)
                        )
                    }
                    FilaVentaReciente(venta = venta, moneda = moneda)
                }
            }
        }
    }
}

@Composable
private fun FilaVentaReciente(venta: Venta, moneda: Moneda) {
    val hora = remember(venta.fecha) {
        DateTimeFormatter.ofPattern("HH:mm").format(venta.fecha.atZone(ZoneId.systemDefault()))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(ScColors.panelAlto, Shape.Pequeno),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = ScColors.textoSecundario,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Venta ${venta.id.take(6).uppercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = ScColors.texto
            )
            Text(
                text = "$hora · ${venta.metodoPago.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = ScColors.textoSecundario
            )
        }
        Text(
            text = "${monto(venta.total)} ${moneda.name}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = ScColors.texto
        )
    }
}

// =====================================================================================
// 10. CIERRE
// =====================================================================================

@Composable
private fun CierrePanel(label: String, isClosed: Boolean, onCierre: () -> Unit) {
    ScPanel(
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = PaddingValues(18.dp),
        borderColor = if (isClosed) ScColors.acento.copy(alpha = 0.35f) else ScColors.borde
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isClosed) "Jornada cerrada" else "Cerrar el $label",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isClosed) ScColors.acento else ScColors.texto
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isClosed) {
                        "El reporte consolidado de este periodo ya fue generado."
                    } else {
                        "Guarda el balance del periodo y genera el reporte consolidado."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ScColors.textoSecundario
                )
            }
            if (isClosed) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ScColors.acento,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (!isClosed) {
            Spacer(Modifier.height(14.dp))
            Surface(
                onClick = onCierre,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = Shape.Mediano,
                color = ScColors.panelAlto,
                border = BorderStroke(1.dp, ScColors.borde)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = ScColors.texto,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Realizar cierre",
                        style = MaterialTheme.typography.labelLarge,
                        color = ScColors.texto
                    )
                }
            }
        }
    }
}

// =====================================================================================
// ERROR
// =====================================================================================

@Composable
fun PantallaErrorDashboard(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(ScColors.alerta.copy(alpha = 0.12f), Shape.Mediano),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = ScColors.alerta
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No se pudo cargar el panel",
                style = MaterialTheme.typography.titleMedium,
                color = ScColors.texto
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ScColors.textoSecundario,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Formato de importe sin moneda: la unidad se dibuja aparte, más pequeña. */
private fun monto(valor: Double): String = "%,.2f".format(java.util.Locale.US, valor)
