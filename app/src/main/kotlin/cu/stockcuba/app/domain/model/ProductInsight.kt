package cu.stockcuba.app.domain.model

/**
 * Representa un análisis inteligente sobre la rentabilidad o estado de un producto.
 */
data class ProductInsight(
    val productoId: String,
    val nombre: String,
    val tipo: InsightTipo,
    val valorPrimario: String, // Ej: "+25% margen" o "$1,200 ganancia"
    val mensaje: String
)

enum class InsightTipo {
    /** Producto con alta ganancia acumulada y buen volumen. */
    ESTRELLA,
    
    /** Producto con margen de ganancia muy bajo (< 15%). */
    ALERTA_MARGEN,
    
    /** Producto con stock alto pero sin ventas en el periodo. */
    ESTANCADO,
    
    /** Producto con ventas constantes pero margen mejorable. */
    POTENCIAL
}
