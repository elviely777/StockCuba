# presentation/navigation/ — Navigation Graph y Rutas

Definición centralizada de navegación Compose.

## Archivos esperados
- `NavGraph.kt` — `NavHost` + `composable(route)` / `navigation(startDestination)`
- `Routes.kt` — `sealed class Route` o `object` con rutas tipadas (`product/{productId}`)
- `NavExtensions.kt` — extensiones `NavController.navigate<Route>()`

## Convenciones
- Rutas con placeholders tipados: `"product/$productId"` → `Route.Product(productId)`
- `NavGraph` recibe `startDestination` y lista de `Feature` composables
- Bottom bar / tabs definidos aquí si aplica
- Deep links: `intentFilter` en `composable(route, deepLinks = [...])`

## Ejemplo
```kotlin
// Routes.kt
sealed class Route(val route: String) {
    object Home : Route("home")
    data class ProductDetail(val productId: String) : Route("product/$productId")
    object Settings : Route("settings")
}

// NavGraph.kt
@Composable
fun AppNavHost(navController: NavHostController, startDestination: String = Route.Home.route) {
    NavHost(navController, startDestination) {
        composable(Route.Home.route) { HomeScreen(onProductClick = { id ->
            navController.navigate(Route.ProductDetail(id).route)
        }) }
        composable(
            route = Route.ProductDetail("").route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.getString()!!
            ProductDetailScreen(productId = productId)
        }
        composable(Route.Settings.route) { SettingsScreen() }
    }
}
```