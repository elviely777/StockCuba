package cu.stockcuba.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shapes de Material 3 basados en DESIGN.md.
 *
 * Lenguaje de formas: Amigable y moderno.
 * - Default (cards/contenedores): 16px (rounded-lg) - suaviza datos técnicos
 * - Interactivos (botones, search bars): 24px (rounded-xl) / full pill - invitan interacción
 * - Utilitarios (tags, checkboxes): 4px (soft) - precisión funcional
 */
object Shape {

    private val RedondeoSuave = 4.dp      // rounded-sm: 0.25rem = 4px
    private val RedondeoDefault = 8.dp    // rounded: 0.5rem = 8px
    private val RedondeoMedio = 12.dp     // rounded-md: 0.75rem = 12px
    private val RedondeoGrande = 16.dp    // rounded-lg: 1rem = 16px (DEFAULT para cards)
    private val RedondeoExtraGrande = 24.dp // rounded-xl: 1.5rem = 24px (botones, FAB, search)
    private val RedondeoFull = 9999.dp    // rounded-full: 9999px (pills, chips, FAB circular)

    val StockCubaShapes = Shapes(
        // Extra small - 4px: Tags, badges, checkboxes, chips pequeños
        extraSmall = RoundedCornerShape(RedondeoSuave),

        // Small - 8px: Inputs, botones secundarios, chips
        small = RoundedCornerShape(RedondeoDefault),

        // Medium - 12px: Cards secundarias, dialogs, menus
        medium = RoundedCornerShape(RedondeoMedio),

        // Large - 16px: Cards principales, contenedores primarios (DEFAULT del diseño)
        large = RoundedCornerShape(RedondeoGrande),

        // Extra large - 24px: FAB, botones principales, search bars, modales
        extraLarge = RoundedCornerShape(RedondeoExtraGrande)
    )

    // ===== EXTENSIONES PARA USO DIRECTO =====

    /** 4px - Tags, badges, checkboxes, utilitarios precisos */
    val Suave = RoundedCornerShape(RedondeoSuave)

    /** 8px - Inputs, botones secundarios, chips estándar */
    val Pequeno = RoundedCornerShape(RedondeoDefault)

    /** 12px - Cards secundarias, dropdowns, dialogs */
    val Mediano = RoundedCornerShape(RedondeoMedio)

    /** 16px - Cards principales, contenedores de contenido (DEFAULT DISEÑO) */
    val Grande = RoundedCornerShape(RedondeoGrande)

    /** 24px - FAB, botones primarios, search bars, modales (INVITA INTERACCIÓN) */
    val ExtraGrande = RoundedCornerShape(RedondeoExtraGrande)

    /** 9999px - Pills, FAB circular, avatares, badges completamente redondos */
    val Full = RoundedCornerShape(RedondeoFull)
}