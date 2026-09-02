package cu.stockcuba.app.presentation.navigation

import androidx.compose.foundation.background
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
import cu.stockcuba.app.presentation.ajustes.AjustesScreen
import cu.stockcuba.app.presentation.ajustes.VinculacionScreen
import cu.stockcuba.app.presentation.clientes.ClientesScreen
import cu.stockcuba.app.presentation.dashboard.DashboardScreen
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
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import cu.stockcuba.app.presentation.theme.Shape

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

    // Bottom nav items
    val bottomNavItems = listOf(
        BottomNavItem(Screen.Dashboard, Icons.Filled.Dashboard, "Dashboard"),
        BottomNavItem(Screen.Ventas, Icons.Filled.PointOfSale, "Ventas"),
        BottomNavItem(Screen.Productos, Icons.Filled.Inventory, "Productos"),
        BottomNavItem(Screen.Inventario, Icons.Filled.Inventory, "Inventario"),
        BottomNavItem(Screen.Mas, Icons.Filled.Settings, "Más")
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentDestination?.route?.startsWith(item.screen.route) == true
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                item.icon, 
                                contentDescription = item.label,
                            ) 
                        },
                        label = { 
                            Text(
                                item.label, 
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            ) 
                        },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            if (isSelected) {
                                // Si ya está seleccionado, volver a la raíz de la pestaña (T28)
                                navController.popBackStack(item.screen.route, inclusive = false)
                            } else {
                                navController.navigate(item.screen.route) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToNuevaVenta = { navController.navigate(Screen.NuevaVenta.route) },
                    onNavigateToHistorial = { navController.navigate(Screen.HistorialVentas.route) },
                    onNavigateToInventario = { navController.navigate(Screen.Inventario.route) }
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
                        DetalleVentaScreen(ventaId = ventaId, onBack = { navController.popBackStack() })
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

            // ===== Más (Ajustes, Clientes) =====
            navigation(startDestination = Screen.Mas.route, route = "mas_root") {
                composable(Screen.Mas.route) {
                    MasScreen(
                        onAjustes = { navController.navigate(Screen.Ajustes.route) },
                        onClientes = { navController.navigate(Screen.Clientes.route) }
                    )
                }
                composable(Screen.Clientes.route) {
                    ClientesScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Ajustes.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        AjustesScreen(onBack = { navController.popBackStack() }, navController = navController)
                    }
                }
                composable(Screen.VinculacionNegocio.route) {
                    VinculacionScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

data class BottomNavItem(val screen: Screen, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

// --- Placeholder Screens para completar el flujo ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(onNuevaVenta: () -> Unit, onHistorial: () -> Unit, onDetalleVenta: (String) -> Unit) {
    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = StockCubaSpacing.Lg, vertical = StockCubaSpacing.Md)
                        .statusBarsPadding()
                ) {
                    Text(
                        "Ventas",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        "Gestión de transacciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
            ) {
                Button(
                    onClick = onNuevaVenta, 
                    shape = Shape.ExtraGrande,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = StockCubaSpacing.Xl)
                ) { 
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nueva Venta") 
                }
                Button(
                    onClick = onHistorial, 
                    shape = Shape.ExtraGrande,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = StockCubaSpacing.Xl),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) { 
                    Icon(Icons.Default.History, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Historial") 
                }
            }
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
fun MasScreen(onAjustes: () -> Unit, onClientes: () -> Unit) {
    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = StockCubaSpacing.Lg, vertical = StockCubaSpacing.Md)
                        .statusBarsPadding()
                ) {
                    Text(
                        "Más Opciones",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        "Configuración y herramientas adicionales",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
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
                
                Text(
                    text = "StockCuba", 
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(Modifier.height(StockCubaSpacing.Md))
                
                Button(
                    onClick = onAjustes,
                    shape = Shape.ExtraGrande,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = StockCubaSpacing.Xl),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.size(StockCubaSpacing.Sm))
                    Text("Ajustes")
                }
                Button(
                    onClick = onClientes,
                    shape = Shape.ExtraGrande,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = StockCubaSpacing.Xl),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(Modifier.size(StockCubaSpacing.Sm))
                    Text("Clientes")
                }
            }
        }
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
