package cu.stockcuba.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Object wrapper para acceder a los colores semánticos desde Color.kt
 * como StockCubaColors.VerdeExito, StockCubaColors.CoralAlerta, etc.
 */
object StockCubaColors {
    // ===== COLORES BASE (Brand) =====
    val FondoPrincipal = Color(0xFF0F172A)
    val FondoCanvas = Color(0xFF051424)

    // Éxito y Crecimiento: Vibrant Teal
    val VerdeExito = Color(0xFF2DD4BF)
    val VerdeExitoHover = Color(0xFF14B8A6)
    val VerdeExitoContainer = Color(0xFF0F766E)

    // Alerta y Advertencia: Warm Coral
    val CoralAlerta = Color(0xFFFB7185)
    val CoralAlertaHover = Color(0xFFF43F5E)
    val CoralAlertaContainer = Color(0xFF9F1239)

    // ===== SUPERFICIES Y BORDES =====
    val SuperficieL1 = Color(0xFF1E293B) // Elevated card backgrounds
    val SuperficieL2 = Color(0xFF334155) // Hover states / Input fields
    val SuperficieElevada = Color(0xFF1E293B) // Alias para SuperficieL1
    val BordeSutil = Color(0xFF334155)   // 1px inner border para cards
    val DivisorLista = Color(0xFF334155) // 0.5px horizontal dividers

    // ===== TIPOGRAFÍA =====
    val TextoPrimario = Color(0xFFF8FAFC)
    val TextoSecundario = Color(0xFF94A3B8)
    val TextoMuted = Color(0xFF64748B)

    // ===== ESTADOS FUNCIONALES (Chips/Badges) =====
    val ChipStockAltoFondo = Color(0x1A2DD4BF)
    val ChipStockAltoTexto = Color(0xFF2DD4BF)
    val ChipStockBajoFondo = Color(0x1AFB7185)
    val ChipStockBajoTexto = Color(0xFFFB7185)
    val ChipStockMedioFondo = Color(0x1A94A3B8)
    val ChipStockMedioTexto = Color(0xFF94A3B8)

    // ===== FAB / ACCIONES PRINCIPALES =====
    val FabPrimarioFondo = Color(0xFF2DD4BF)
    val FabPrimarioTexto = Color(0xFF0F172A)
    val FabPrimarioGlow = Color(0x1A2DD4BF)
    val FabFondo = FabPrimarioFondo
    val FabTexto = FabPrimarioTexto
    val FabGlow = FabPrimarioGlow

    // ===== INPUTS =====
    val InputFondo = Color(0xFF1E293B)
    val InputBorde = Color(0xFF334155)
    val InputBordeFocus = Color(0xFF2DD4BF)
    val InputPlaceholder = Color(0xFF64748B)

    // ===== MODALS / POPOVERS =====
    val ModalFondo = Color(0xB30F172A)
    val ModalBlur = 20
    val ModalSombraGlow = Color(0x1A2DD4BF)

    // ===== COLORES SEMÁNTICOS ADICIONALES =====
    val AzulPrincipal = Color(0xFF2DD4BF) // Teal como principal
    val AmarilloOro = Color(0xFFFBBF24)   // Amber/Yellow para warnings
    val NaranjaAdvertencia = Color(0xFFF97316) // Orange para warnings
    val RojoPeligro = Color(0xFFEF4444)   // Red para errores críticos
    val IndigoMarca = Color(0xFF6366F1)
}