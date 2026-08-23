package cu.stockcuba.app.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cu.stockcuba.app.presentation.ajustes.AjustesScreen
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
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import cu.stockcuba.app.presentation.theme.Shape

/**
 * NavGraph principal con Scaffold + Bottom Navigation Bar.
 * Wraps sensitive routes (ventas_root, inventario_root, ajustes) with SecurityGate (T40).
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Security dependencies (injected via Hilt ViewModel to avoid direct Object to ViewModel cast crash)
    val securityViewModel: SecurityViewModel = hiltViewModel()
    val securityRepository = securityViewModel.securityRepository
    val biometricAuthenticator = securityViewModel.biometricAuthenticator

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
                    val isSelected = navController.currentDestination?.route?.startsWith(item.screen.route) == true
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        selected = isSelected,
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
                DashboardScreen(onNavigateToNuevaVenta = { navController.navigate(Screen.NuevaVenta.route) })
            }

            // ===== PROTECTED: Ventas =====
            navigation(startDestination = Screen.Ventas.route, route = "ventas_root") {
                composable(Screen.Ventas.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        biometricAuthenticator = biometricAuthenticator,
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
                        biometricAuthenticator = biometricAuthenticator,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        NuevaVentaScreen(onComplete = { navController.popBackStack() })
                    }
                }
                composable(Screen.HistorialVentas.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        biometricAuthenticator = biometricAuthenticator,
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
                        biometricAuthenticator = biometricAuthenticator,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        DetalleVentaScreen(ventaId = ventaId)
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
                    DetalleProductoScreen(productoId = productoId)
                }
            }

            // ===== PROTECTED: Inventario =====
            navigation(startDestination = Screen.Inventario.route, route = "inventario_root") {
                composable(Screen.Inventario.route) {
                    SecurityGate(
                        securityRepository = securityRepository,
                        biometricAuthenticator = biometricAuthenticator,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        InventarioScreen(
                            onAjuste = { producto -> navController.navigate(Screen.AjusteInventario(producto.id).route) },
                            onHistorial = { producto -> navController.navigate(Screen.HistorialMovimientos(producto.id).route) }
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
                        biometricAuthenticator = biometricAuthenticator,
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
                        biometricAuthenticator = biometricAuthenticator,
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
                        biometricAuthenticator = biometricAuthenticator,
                        onUnlocked = { /* unlocked */ }
                    ) {
                        AjustesScreen(onBack = { navController.popBackStack() }, navController = navController)
                    }
                }
            }
        }
    }
}

data class BottomNavItem(val screen: Screen, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

// --- Placeholder Screens para completar el flujo ---

@Composable
fun VentasScreen(onNuevaVenta: () -> Unit, onHistorial: () -> Unit, onDetalleVenta: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
        ) {
            Text(text = "Ventas", style = MaterialTheme.typography.displaySmall)
            Button(onClick = onNuevaVenta, shape = Shape.ExtraGrande) { Text("Nueva Venta") }
            Button(onClick = onHistorial, shape = Shape.ExtraGrande) { Text("Historial") }
        }
    }
}

@Composable
fun DetalleVentaScreen(ventaId: String) {
    PlaceholderScreen("Detalle Venta", "Venta #$ventaId", "Cerrar", { })
}

@Composable
fun DetalleProductoScreen(productoId: String) {
    PlaceholderScreen("Detalle Producto", "ID: $productoId", "Editar", { })
}

@Composable
fun MasScreen(onAjustes: () -> Unit, onClientes: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
        ) {
            Text(
                text = "Más", 
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
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

@Composable
fun PlaceholderScreen(title: String, subtitle: String, actionLabel: String, onAction: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)) {
            Text(text = title, style = MaterialTheme.typography.displaySmall)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Button(onClick = onAction, shape = Shape.ExtraGrande) { Text(actionLabel) }
        }
    }
}
