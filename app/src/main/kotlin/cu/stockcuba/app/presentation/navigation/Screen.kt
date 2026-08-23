package cu.stockcuba.app.presentation.navigation

/**
 * Rutas de navegación selladas (type-safe).
 * Cada ruta puede llevar argumentos tipados.
 */
sealed interface Screen {
    val route: String

    // ===== BOTTOM NAV DESTINATIONS (Top-level) =====
    data object Dashboard : Screen {
        override val route = "dashboard"
    }

    data object Ventas : Screen {
        override val route = "ventas"
    }

    data object Productos : Screen {
        override val route = "productos"
    }

    data object Inventario : Screen {
        override val route = "inventario"
    }

    data object Mas : Screen {
        override val route = "mas"
    }

    // ===== SUB-DESTINATIONS (Push onto stack) =====
    data object ListaProductos : Screen {
        override val route = "productos/lista"
    }

    data class DetalleProducto(val productoId: String) : Screen {
        override val route = "productos/detalle/$productoId"
        companion object {
            const val ROUTE_PATTERN = "productos/detalle/{productoId}"
        }
    }

    data object NuevaVenta : Screen {
        override val route = "ventas/nueva"
    }

    data object HistorialVentas : Screen {
        override val route = "ventas/historial"
    }

    data class DetalleVenta(val ventaId: String) : Screen {
        override val route = "ventas/detalle/$ventaId"
        companion object {
            const val ROUTE_PATTERN = "ventas/detalle/{ventaId}"
        }
    }

    data object Ajustes : Screen {
        override val route = "mas/ajustes"
    }

    data object Clientes : Screen {
        override val route = "mas/clientes"
    }

    data object AgregarProducto : Screen {
        override val route = "productos/agregar"
    }

    data class FormularioProducto(
        val modo: String, // "crear" | "editar"
        val productoId: String? = null
    ) : Screen {
        override val route = "productos/formulario/$modo/${productoId ?: ""}"
        companion object {
            const val ROUTE_PATTERN = "productos/formulario/{modo}/{productoId?}"
        }
    }

    data object EditarProducto : Screen {
        override val route = "productos/editar"
    }

    // ===== INVENTARIO SUB-DESTINATIONS =====
    data class AjusteInventario(val productoId: String) : Screen {
        override val route = "inventario/ajuste/$productoId"
        companion object {
            const val ROUTE_PATTERN = "inventario/ajuste/{productoId}"
        }
    }

    data class HistorialMovimientos(val productoId: String) : Screen {
        override val route = "inventario/historial/$productoId"
        companion object {
            const val ROUTE_PATTERN = "inventario/historial/{productoId}"
        }
    }
}