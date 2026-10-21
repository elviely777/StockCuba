package cu.stockcuba.app.presentation.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cu.stockcuba.app.domain.model.RolUsuario
import cu.stockcuba.app.presentation.ajustes.AjustesScreen
import cu.stockcuba.app.presentation.ajustes.GestionCategoriasScreen
import cu.stockcuba.app.presentation.ajustes.VinculacionScreen
import cu.stockcuba.app.presentation.clientes.ClientesScreen
import cu.stockcuba.app.presentation.dashboard.DashboardScreen
import cu.stockcuba.app.presentation.gastos.GestionGastosScreen
import cu.stockcuba.app.presentation.inventario.AjusteInventarioScreen
import cu.stockcuba.app.presentation.inventario.HistorialMovimientosScreen
import cu.stockcuba.app.presentation.inventario.InventarioScreen
import cu.stockcuba.app.presentation.productos.FormularioProductoScreen
import cu.stockcuba.app.presentation.productos.ListaProductosScreen
import cu.stockcuba.app.presentation.security.SecurityGate
import cu.stockcuba.app.presentation.security.SecurityViewModel
import cu.stockcuba.app.presentation.ventas.HistorialVentasScreen
import cu.stockcuba.app.presentation.ventas.NuevaVentaScreen
import cu.stockcuba.app.presentation.ventas.DetalleVentaScreen
import cu.stockcuba.app.presentation.ventas_historicas.VentasHistoricasScreen
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.ScColors
import cu.stockcuba.app.presentation.common.ScBottomDock
import cu.stockcuba.app.presentation.common.ScDockItem
import cu.stockcuba.app.presentation.common.ScEmptyState
import cu.stockcuba.app.presentation.common.ScLabel
import cu.stockcuba.app.presentation.common.ScMoney
import cu.stockcuba.app.presentation.common.ScPanel
import cu.stockcuba.app.presentation.common.ScPrimaryButton
import cu.stockcuba.app.presentation.common.ScScreenHeader
import cu.stockcuba.app.presentation.common.ScSecondaryButton
import cu.stockcuba.app.presentation.common.ScSectionHeader
import cu.stockcuba.app.presentation.common.ScStatusPill
import cu.stockcuba.app.presentation.common.ScTone
import cu.stockcuba.app.presentation.dashboard.DashboardUiState
import cu.stockcuba.app.presentation.dashboard.DashboardViewModel
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warehouse
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.domain.model.Moneda
import cu.stockcuba.app.domain.model.Venta
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * NavGraph principal con Scaffold + Bottom Navigation Bar.
 * Wraps sensitive routes (ventas_root, inventario_root, ajustes) with SecurityGate (T40).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    // Security dependencies (injected via Hilt ViewModel to avoid direct Object to ViewModel cast crash)
    val securityViewModel: SecurityViewModel = hiltViewModel()
    val securityRepository = securityViewModel.securityRepository

    // Dock inferior
    val dockItems = listOf(
        ScDockItem(Screen.Dashboard.route, Icons.Filled.Dashboard, "Panel"),
        ScDockItem(Screen.Ventas.route, Icons.Filled.PointOfSale, "Ventas"),
        ScDockItem(Screen.Productos.route, Icons.Filled.Inventory2, "Productos"),
        ScDockItem(Screen.Inventario.route, Icons.Filled.Warehouse, "Inventario"),
        ScDockItem(Screen.Mas.route, Icons.Filled.MoreHoriz, "Más")
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ScColors.canvas,
        bottomBar = {
            // El POS y la edición de venta son flujos de pantalla completa: llevan
            // su propia barra de cobro, así que el dock global se oculta.
            val ocultarDock = currentDestination?.route?.let { ruta ->
                ruta.startsWith(Screen.NuevaVenta.route) || ruta.startsWith("ventas/editar")
            } ?: false
            if (!ocultarDock) {
                ScBottomDock(
                    items = dockItems,
                    currentRoute = currentDestination?.route,
                    onSelect = { item ->
                        val isSelected = currentDestination?.route?.startsWith(item.route) == true
                        if (isSelected) {
                            // Si ya está seleccionado, volver a la raíz de la pestaña (T28)
                            navController.popBackStack(item.route, inclusive = false)
                        } else {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToNuevaVenta = { navController.navigate(Screen.NuevaVenta.route) },
                    onNavigateToHistorial = { navController.navigate(Screen.HistorialVentas.route) },
                    onNavigateToInventario = { navController.navigate(Screen.Inventario.route) },
                    onNavigateToGastos = { navController.navigate(Screen.Gastos.route) }
                )
            }

            // ===== PROTECTED: Ventas =====
            navigation(startDestination = Screen.Ventas.route, route = "ventas_root") {
                composable(Screen.Ventas.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        VentasScreen(
                            onNuevaVenta = { navController.navigate(Screen.NuevaVenta.route) },
                            onHistorial = { navController.navigate(Screen.HistorialVentas.route) },
                            onDetalleVenta = { ventaId -> navController.navigate(Screen.DetalleVenta(ventaId).route) }
                        )
                    }
                }
                composable(Screen.NuevaVenta.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        NuevaVentaScreen(onComplete = { navController.popBackStack() })
                    }
                }
                composable(Screen.HistorialVentas.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        HistorialVentasScreen(
                            onBack = { navController.popBackStack() },
                            onDetalle = { ventaId -> navController.navigate(Screen.DetalleVenta(ventaId).route) }
                        )
                    }
                }
                composable(
                    route = Screen.DetalleVenta.ROUTE_PATTERN,
                    arguments = listOf(navArgument("ventaId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val ventaId = backStackEntry.arguments?.getString("ventaId") ?: ""
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        DetalleVentaScreen(
                            ventaId = ventaId, 
                            onBack = { navController.popBackStack() },
                            onEditar = { id -> navController.navigate(Screen.EditarVenta(id).route) }
                        )
                    }
                }
                composable(
                    route = Screen.EditarVenta.ROUTE_PATTERN,
                    arguments = listOf(navArgument("ventaId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val ventaId = backStackEntry.arguments?.getString("ventaId") ?: ""
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        NuevaVentaScreen(
                            ventaIdParaEditar = ventaId,
                            onComplete = { navController.popBackStack() }
                        )
                    }
                }
            }

            // ===== Productos =====
            navigation(startDestination = Screen.Productos.route, route = "productos_root") {
                composable(Screen.Productos.route) {
                    ListaProductosScreen(
                        onAgregar = { navController.navigate(Screen.FormularioProducto("crear").route) },
                        onDetalle = { id -> navController.navigate(Screen.DetalleProducto(id).route) },
                        onEditar = { id -> navController.navigate(Screen.FormularioProducto("editar", id).route) }
                    )
                }
                composable(
                    route = Screen.FormularioProducto.ROUTE_PATTERN,
                    arguments = listOf(
                        navArgument("modo") { type = NavType.StringType },
                        navArgument("productoId") { 
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val modo = backStackEntry.arguments?.getString("modo") ?: "crear"
                    val productoId = backStackEntry.arguments?.getString("productoId")
                    FormularioProductoScreen(
                        onSave = { navController.popBackStack() },
                        onCancel = { navController.popBackStack() },
                        initialModo = modo,
                        initialProductoId = productoId
                    )
                }
                composable(
                    route = Screen.DetalleProducto.ROUTE_PATTERN,
                    arguments = listOf(navArgument("productoId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val productoId = backStackEntry.arguments?.getString("productoId") ?: ""
                    DetalleProductoScreen(
                        productoId = productoId,
                        onEditar = { id -> 
                            navController.navigate(Screen.FormularioProducto("editar", id).route) {
                                // Evitar duplicados en el backstack
                                popUpTo(Screen.Productos.route)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // ===== PROTECTED: Inventario =====
            navigation(startDestination = Screen.Inventario.route, route = "inventario_root") {
                composable(Screen.Inventario.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        InventarioScreen(
                            onAjuste = { producto -> navController.navigate(Screen.AjusteInventario(producto.id).route) },
                            onHistorial = { producto -> navController.navigate(Screen.HistorialMovimientos(producto.id).route) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(
                    route = Screen.AjusteInventario.ROUTE_PATTERN,
                    arguments = listOf(navArgument("productoId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val productoId = backStackEntry.arguments?.getString("productoId") ?: ""
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        AjusteInventarioScreen(productoId = productoId, onComplete = { navController.popBackStack() })
                    }
                }
                composable(
                    route = Screen.HistorialMovimientos.ROUTE_PATTERN,
                    arguments = listOf(navArgument("productoId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val productoId = backStackEntry.arguments?.getString("productoId") ?: ""
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        HistorialMovimientosScreen(productoId = productoId, onBack = { navController.popBackStack() })
                    }
                }
            }

            // ===== Más (Ajustes, Clientes, Gastos) =====
            navigation(startDestination = Screen.Mas.route, route = "mas_root") {
                composable(Screen.Mas.route) {
                    val rolActual by securityViewModel.rolActual.collectAsState(RolUsuario.VENDEDOR)
                    MasScreen(
                        rolActual = rolActual,
                        onAjustes = { navController.navigate(Screen.Ajustes.route) },
                        onClientes = { navController.navigate(Screen.Clientes.route) },
                        onGastos = { navController.navigate(Screen.Gastos.route) }
                    )
                }
                composable(Screen.Clientes.route) {
                    ClientesScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Gastos.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        GestionGastosScreen(onBack = { navController.popBackStack() })
                    }
                }
                composable(Screen.Ajustes.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        AjustesScreen(onBack = { navController.popBackStack() }, navController = navController)
                    }
                }
                composable(Screen.VentasHistoricas.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        VentasHistoricasScreen(onBack = { navController.popBackStack() })
                    }
                }
                composable(Screen.Categorias.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        GestionCategoriasScreen(onBack = { navController.popBackStack() })
                    }
                }
                composable(Screen.VinculacionNegocio.route) {
                    VinculacionScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

// --- Placeholder Screens para completar el flujo ---

@Composable
fun VentasScreen(
    onNuevaVenta: () -> Unit,
    onHistorial: () -> Unit,
    onDetalleVenta: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState as? DashboardUiState.Success
    val total = state?.totalVendido ?: 0.0
    val cantidad = state?.cantidadVentas ?: 0
    val moneda = state?.monedaBase ?: Moneda.CUP
    val recientes = state?.ventasRecientes.orEmpty()
        .sortedByDescending { it.fecha }
        .take(5)
    val ticket = if (cantidad > 0) total / cantidad else 0.0
    val montoTotal = "%,.2f".format(Locale.US, total)
    val montoTicket = "%,.2f".format(Locale.US, ticket)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScColors.canvas)
    ) {
        ScScreenHeader(
            title = "Ventas",
            subtitle = "Cobros del día y comprobantes"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScPanel(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScLabel("Cobrado hoy")
                        Spacer(Modifier.weight(1f))
                        ScStatusPill(
                            text = if (cantidad > 0) "En caja" else "Sin movimientos",
                            tone = if (cantidad > 0) ScTone.Positive else ScTone.Neutral
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ScMoney(
                        value = montoTotal,
                        unit = moneda.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = ScColors.dinero
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        Column {
                            ScLabel("Ventas")
                            Text(
                                text = cantidad.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = ScColors.texto
                            )
                        }
                        Column {
                            ScLabel("Ticket promedio")
                            Text(
                                text = "$montoTicket",
                                style = MaterialTheme.typography.titleMedium,
                                color = ScColors.texto
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    ScPrimaryButton(
                        text = "Nueva venta",
                        onClick = onNuevaVenta,
                        icon = Icons.Default.Add
                    )
                    Spacer(Modifier.height(8.dp))
                    ScSecondaryButton(
                        text = "Historial de ventas",
                        onClick = onHistorial,
                        icon = Icons.Default.History
                    )
                }
            }

            if (recientes.isEmpty()) {
                item {
                    ScEmptyState(
                        icon = Icons.Default.Receipt,
                        title = "Aún no hay ventas hoy",
                        message = "Registra una venta y aquí verás cada cobro con su hora y su total.",
                        actionLabel = "Registrar primera venta",
                        onAction = onNuevaVenta
                    )
                }
            } else {
                item {
                    ScSectionHeader(
                        title = "Últimas ventas",
                        actionLabel = "Ver todo",
                        onAction = onHistorial
                    )
                }
                items(recientes, key = { it.id }) { venta ->
                    FilaVentaReciente(
                        venta = venta,
                        moneda = moneda,
                        onClick = { onDetalleVenta(venta.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaVentaReciente(venta: Venta, moneda: Moneda, onClick: () -> Unit) {
    val hora = remember(venta.fecha) {
        DateTimeFormatter.ofPattern("HH:mm").format(venta.fecha.atZone(ZoneId.systemDefault()))
    }
    val metodo = venta.metodoPago.name.lowercase().replaceFirstChar { it.uppercase() }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = Shape.Mediano,
        color = ScColors.panel,
        border = BorderStroke(1.dp, ScColors.borde)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${venta.numeroItems} ${if (venta.numeroItems == 1) "artículo" else "artículos"}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ScColors.texto
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$hora · $metodo",
                    style = MaterialTheme.typography.bodySmall,
                    color = ScColors.textoSecundario
                )
            }
            ScMoney(
                value = "%,.2f".format(Locale.US, venta.total),
                unit = moneda.name,
                style = MaterialTheme.typography.titleLarge,
                color = ScColors.texto
            )
        }
    }
}

@Composable
fun DetalleProductoScreen(productoId: String, onEditar: (String) -> Unit, onBack: () -> Unit) {
    PlaceholderScreen(
        title = "Detalle Producto", 
        subtitle = "ID del producto: $productoId", 
        actionLabel = "Editar Producto", 
        onAction = { onEditar(productoId) }
    )
}

@Composable
fun MasScreen(
    rolActual: RolUsuario,
    onAjustes: () -> Unit, 
    onClientes: () -> Unit, 
    onGastos: () -> Unit
) {
    val isDueno = rolActual == RolUsuario.DUENO

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScColors.canvas)
    ) {
        ScScreenHeader(
            title = "Más",
            subtitle = "Herramientas y configuración del negocio"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScPanel(contentPadding = PaddingValues(0.dp)) {
                if (isDueno) {
                    FilaHerramienta(
                        icon = Icons.Default.Settings,
                        title = "Ajustes",
                        subtitle = "Moneda, tasas, impresora y respaldo",
                        onClick = onAjustes
                    )
                    ScDividerHorizontal()
                }
                FilaHerramienta(
                    icon = Icons.Default.Person,
                    title = "Clientes",
                    subtitle = "Cuentas, saldos y abonos",
                    onClick = onClientes
                )
                if (isDueno) {
                    ScDividerHorizontal()
                    FilaHerramienta(
                        icon = Icons.Default.Payments,
                        title = "Gastos operativos",
                        subtitle = "Luz, salarios y otros costos del mes",
                        onClick = onGastos
                    )
                }
            }

            ScPanel(contentPadding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ScColors.panelAlto, Shape.Mediano),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SC",
                            style = MaterialTheme.typography.titleMedium,
                            color = ScColors.acento
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "StockCuba",
                            style = MaterialTheme.typography.titleSmall,
                            color = ScColors.texto
                        )
                        Text(
                            text = "Inventario y ventas para tu negocio",
                            style = MaterialTheme.typography.bodySmall,
                            color = ScColors.textoSecundario
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    ScStatusPill(
                        text = if (isDueno) "Dueño" else "Vendedor",
                        tone = ScTone.Neutral
                    )
                }
            }
        }
    }
}

@Composable
private fun ScDividerHorizontal() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp)
            .height(1.dp)
            .background(ScColors.borde)
    )
}

@Composable
private fun FilaHerramienta(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ScColors.panelAlto, Shape.Pequeno),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ScColors.acento,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = ScColors.texto
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ScColors.textoSecundario
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = ScColors.textoDebil,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun PlaceholderScreen(title: String, subtitle: String, actionLabel: String, onAction: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg),
            modifier = Modifier.padding(StockCubaSpacing.Xl)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = Shape.Full,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = title, 
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.bodyLarge, 
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(StockCubaSpacing.Md))
            Button(
                onClick = onAction, 
                shape = Shape.Grande,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StockCubaColors.VerdeExito,
                    contentColor = Color(0xFF001E1C)
                )
            ) { 
                Text(actionLabel, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) 
            }
        }
    }
}
