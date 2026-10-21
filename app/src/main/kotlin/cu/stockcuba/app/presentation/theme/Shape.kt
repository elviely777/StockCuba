package cu.stockcuba.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas StockCuba · vocabulario "instrumento de mostrador".
 *
 * - Paneles de datos: 16dp (extraLarge del theme) — contienen información, no son botones.
 * - Controles (botones, campos, chips de acción): 12dp — precisos, no pastillas.
 * - Etiquetas pequeñas: 8dp.
 * - Solo los estados (chips de stock) y los avatares/indicadores usan pastilla completa.
 *
 * La intención es que un botón no se confunda con una tarjeta y que la app no
 * se lea como una pila de pastillas idénticas.
 */
object Shape {

    /** 6dp — marcas, badges diminutos, rieles. */
    val Suave = RoundedCornerShape(6.dp)

    /** 8dp — etiquetas pequeñas, miniaturas. */
    val Pequeno = RoundedCornerShape(8.dp)

    /** 12dp — controles: botones, campos, filas pulsables. */
    val Mediano = RoundedCornerShape(12.dp)

    /** 16dp — paneles de datos (por defecto del sistema). */
    val Grande = RoundedCornerShape(16.dp)

    /** 20dp — hojas y modales inferiores. */
    val ExtraGrande = RoundedCornerShape(20.dp)

    /** Pastilla completa — solo chips de estado, avatares y el indicador del dock. */
    val Full = RoundedCornerShape(percent = 50)

    val StockCubaShapes = Shapes(
        extraSmall = Suave,
        small = Pequeno,
        medium = Mediano,
        large = Grande,
        extraLarge = ExtraGrande
    )
}
