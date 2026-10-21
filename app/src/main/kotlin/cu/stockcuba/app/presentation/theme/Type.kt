@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package cu.stockcuba.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cu.stockcuba.app.R

/**
 * Tipografía StockCuba · Inter (variable, incluida en la app).
 *
 * Por qué Inter: es una grotesca de interfaz con altura de x generosa y cifras
 * tabulares, lo que hace que los importes en una lista se puedan comparar de un
 * vistazo. Todo el sistema usa una sola familia; el carácter viene del contraste
 * de pesos (400 cuerpo / 600 títulos / 800 cifras) y del tracking negativo en
 * tamaños grandes.
 *
 * Regla: los números siempre con [Cifras] (`tnum`), para que no "bailen" al
 * actualizarse en vivo.
 */

private fun inter(weight: Int): Font = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Familia Inter completa, con los cinco pesos que usa el sistema. */
val FamiliaInter: FontFamily = FontFamily(
    inter(400),
    inter(500),
    inter(600),
    inter(700),
    inter(800)
)

/** Tracking de cifras: tabulares + numerales de altura fija. */
const val Cifras = "tnum"

object Typography {

    val StockCubaTypography = Typography(
        // ===== DISPLAY — solo para la cifra protagonista de una pantalla =====
        displayLarge = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 40.sp,
            lineHeight = 46.sp,
            letterSpacing = (-0.03).em,
            fontFeatureSettings = Cifras
        ),
        displayMedium = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.03).em,
            fontFeatureSettings = Cifras
        ),
        displaySmall = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.02).em,
            fontFeatureSettings = Cifras
        ),

        // ===== HEADLINE — títulos de pantalla y de sección =====
        headlineLarge = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.02).em
        ),
        headlineMedium = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.015).em
        ),
        headlineSmall = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.01).em
        ),

        // ===== TITLE — filas, tarjetas, diálogos =====
        titleLarge = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        titleSmall = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),

        // ===== BODY — texto de lectura y datos secundarios =====
        bodyLarge = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 20.sp
        ),

        // ===== LABEL — etiquetas, chips, botones =====
        labelLarge = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.01.em
        ),
        labelMedium = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.04.em
        ),
        labelSmall = TextStyle(
            fontFamily = FamiliaInter,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.06.em
        )
    )
}
