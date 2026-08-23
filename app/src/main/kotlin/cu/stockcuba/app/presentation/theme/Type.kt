package cu.stockcuba.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Tipografía Material 3 replicando la escala de DESIGN.md.
 * Fuente: Inter (exclusiva para máxima legibilidad en móvil).
 *
 * Jerarquía: Contraste extremo de pesos.
 * - Headers: Bold (700) / ExtraBold (800) - anclan la página
 * - Body: Regular (400) - alta legibilidad en listas densas
 * - Labels semánticos: Medium (500) / SemiBold (600) - distinguen datos de texto estático
 */
object Typography {

    private val Inter = FontFamily.Default // Se carga via Google Fonts o local

    val StockCubaTypography = Typography(
        // ===== DISPLAY =====
        // displayLarge: 48px, ExtraBold (800), lineHeight 56px, letterSpacing -0.02em
        displayLarge = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 48.sp,
            lineHeight = 56.sp,
            letterSpacing = (-0.02).em
        ),
        // displayMedium: 40px, ExtraBold (800)
        displayMedium = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 40.sp,
            lineHeight = 48.sp,
            letterSpacing = (-0.01).em
        ),
        // displaySmall: 36px, Bold (700)
        displaySmall = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),

        // ===== HEADLINE =====
        // headlineLarge: 32px, Bold (700), lineHeight 40px, letterSpacing -0.01em
        headlineLarge = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.01).em
        ),
        // headlineMedium: 28px, Bold (700), lineHeight 36px (mobile)
        headlineMedium = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        // headlineSmall: 24px, Bold (700), lineHeight 32px
        headlineSmall = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),

        // ===== TITLE =====
        // titleLarge: 22px, SemiBold (600)
        titleLarge = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        // titleMedium: 18px, SemiBold (600) - body-lg weight
        titleMedium = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 28.sp
        ),
        // titleSmall: 16px, Medium (500)
        titleSmall = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),

        // ===== BODY =====
        // bodyLarge: 18px, Regular (400), lineHeight 28px
        bodyLarge = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 28.sp
        ),
        // bodyMedium: 16px, Regular (400), lineHeight 24px
        bodyMedium = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        // bodySmall: 14px, Regular (400)
        bodySmall = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),

        // ===== LABEL =====
        // labelLarge: 14px, SemiBold (600), lineHeight 20px, letterSpacing 0.01em
        labelLarge = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.01.em
        ),
        // labelMedium: 12px, Medium (500), lineHeight 16px, letterSpacing 0.04em
        labelMedium = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.04.em
        ),
        // labelSmall: 11px, Medium (500)
        labelSmall = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.04.em
        )
    )
}