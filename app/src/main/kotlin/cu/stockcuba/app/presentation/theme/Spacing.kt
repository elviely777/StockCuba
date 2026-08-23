package cu.stockcuba.app.presentation.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Espaciado basado en DESIGN.md de Stitch.
 * Escala base: 4dp (rhythm unit)
 */
object StockCubaSpacing {
    // ===== ESCALA BASE (4dp rhythm) =====
    val Xxxs = 2.dp   // 0.5x
    val Xxs = 4.dp    // 1x
    val Xs = 8.dp     // 2x
    val Sm = 12.dp    // 3x
    val Md = 16.dp    // 4x (DEFAULT)
    val Lg = 24.dp    // 6x
    val Xl = 32.dp    // 8x
    val Xxl = 48.dp   // 12x
    val Xxxl = 64.dp  // 16x

    // ===== ALIASES SEMÁNTICOS =====
    val screenPadding = PaddingValues(horizontal = Md, vertical = Md)
    val cardPadding = Md
    val sectionGap = Lg
    val itemGap = Md
    val inlineGap = Sm
    val iconGap = Sm

    // ===== EXTENSIONES PARA CONVENIENCIA =====
    val XlDp = Xl
    val LgDp = Lg
    val MdDp = Md
    val SmDp = Sm
    val XsDp = Xs
    val XxsDp = Xxs
    val XxxsDp = Xxxs
    val XxxlDp = Xxxl
}