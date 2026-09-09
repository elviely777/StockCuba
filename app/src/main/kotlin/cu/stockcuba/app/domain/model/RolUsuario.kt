package cu.stockcuba.app.domain.model

/**
 * Roles de usuario para control de acceso y visibilidad de datos sensibles.
 */
enum class RolUsuario {
    /**
     * Acceso total a finanzas, costos, ganancias e inventario valorado.
     */
    DUENO,

    /**
     * Acceso limitado a ventas, lista de productos y stock actual.
     * No ve costos ni márgenes de ganancia.
     */
    VENDEDOR
}
