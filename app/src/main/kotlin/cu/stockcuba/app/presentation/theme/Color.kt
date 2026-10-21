package cu.stockcuba.app.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * StockCuba · Sistema de diseño "Mostrador" — v2
 *
 * Concepto: un instrumento de mostrador. La pantalla es una superficie de tinta
 * profunda y calmada; los datos viven en paneles con borde de un pixel; el color
 * siempre significa algo (dinero = ámbar, salud de stock = jade, riesgo = coral).
 * La tipografía hace el trabajo pesado: Inter, cifras tabulares y pesos altos.
 *
 * Reglas:
 *  - Ningún color literal en las pantallas. Todo sale de [ScColors] o de
 *    MaterialTheme.colorScheme.
 *  - El modo oscuro es el principal; el claro está igual de cuidado.
 *  - El acento jade es para acción/positivo. El ámbar es para dinero y para
 *    "atención" (no es un error). El coral es solo para riesgo real.
 */

// =====================================================================================
// PALETA OSCURA (principal)
// =====================================================================================

object PaletaOscura {
    /** Lienzo principal: tinta azul-negro, no negro puro (evita el look OLED genérico). */
    val Canvas = Color(0xFF050D13)

    /** Panel nivel 1: tarjetas, filas agrupadas. */
    val Panel = Color(0xFF0A1720)

    /** Panel nivel 2: campos, controles elevados, zonas pulsables. */
    val PanelAlto = Color(0xFF102330)

    /** Panel hundido: rieles, pistas de barras, separadores rellenos. */
    val PanelHundido = Color(0xFF0D1E29)

    /** Borde de 1px: la única "sombra" del sistema. */
    val Borde = Color(0xFF1B303C)
    val BordeFuerte = Color(0xFF264555)

    val Texto = Color(0xFFE9F1F4)
    val TextoSecundario = Color(0xFF93A8B3)
    val TextoDebil = Color(0xFF5C7481)

    /** Jade: acción primaria, stock sano, tendencia positiva. */
    val Acento = Color(0xFF3BE3B0)
    val SobreAcento = Color(0xFF03211A)
    val AcentoContenedor = Color(0xFF0B4A3B)
    val SobreAcentoContenedor = Color(0xFFB9F7E4)

    /** Ámbar: dinero, totales, cobros pendientes, atención. */
    val Dinero = Color(0xFFF2B544)
    val SobreDinero = Color(0xFF241703)
    val DineroContenedor = Color(0xFF463306)
    val SobreDineroContenedor = Color(0xFFFFE2A6)

    /** Coral: riesgo, agotado, error, acción destructiva. */
    val Alerta = Color(0xFFFF7A6B)
    val SobreAlerta = Color(0xFF2B0703)
    val AlertaContenedor = Color(0xFF4A100B)
    val SobreAlertaContenedor = Color(0xFFFFDAD3)

    /** Azul: información neutra, tasas de cambio, enlaces. */
    val Info = Color(0xFF7FB2FF)
    val SobreInfo = Color(0xFF04182E)
    val InfoContenedor = Color(0xFF14304F)
    val SobreInfoContenedor = Color(0xFFD6E6FF)

    val Scrim = Color(0xFF000000)
}

// =====================================================================================
// PALETA CLARA (secundaria, igual de cuidada)
// =====================================================================================

object PaletaClara {
    /** Papel frío, no crema: se lee a plena luz sin verse "hueso". */
    val Canvas = Color(0xFFF2F5F6)
    val Panel = Color(0xFFFFFFFF)
    val PanelAlto = Color(0xFFEAF0F2)
    val PanelHundido = Color(0xFFE3EBEE)

    val Borde = Color(0xFFD6E1E5)
    val BordeFuerte = Color(0xFFB9C9CF)

    val Texto = Color(0xFF08161D)
    val TextoSecundario = Color(0xFF4C6570)
    val TextoDebil = Color(0xFF78909B)

    val Acento = Color(0xFF07795C)
    val SobreAcento = Color(0xFFFFFFFF)
    val AcentoContenedor = Color(0xFFBFEFE0)
    val SobreAcentoContenedor = Color(0xFF002019)

    val Dinero = Color(0xFF8A5A00)
    val SobreDinero = Color(0xFFFFFFFF)
    val DineroContenedor = Color(0xFFFFE9BF)
    val SobreDineroContenedor = Color(0xFF2A1C00)

    val Alerta = Color(0xFFB3261E)
    val SobreAlerta = Color(0xFFFFFFFF)
    val AlertaContenedor = Color(0xFFFFDAD6)
    val SobreAlertaContenedor = Color(0xFF410002)

    val Info = Color(0xFF1D5FA8)
    val SobreInfo = Color(0xFFFFFFFF)
    val InfoContenedor = Color(0xFFD8E7FF)
    val SobreInfoContenedor = Color(0xFF001B3C)

    val Scrim = Color(0xFF000000)
}

// =====================================================================================
// COLORES SEMÁNTICOS (dependen del tema activo)
// =====================================================================================

/**
 * Contrato de color de la app. Cualquier pantalla nueva debe leer de aquí
 * (o de MaterialTheme.colorScheme) y nunca de literales.
 */
@Immutable
data class StockCubaSemanticColors(
    val esOscuro: Boolean,
    val canvas: Color,
    val panel: Color,
    val panelAlto: Color,
    val panelHundido: Color,
    val borde: Color,
    val bordeFuerte: Color,
    val texto: Color,
    val textoSecundario: Color,
    val textoDebil: Color,
    val acento: Color,
    val sobreAcento: Color,
    val acentoContenedor: Color,
    val sobreAcentoContenedor: Color,
    val dinero: Color,
    val sobreDinero: Color,
    val dineroContenedor: Color,
    val sobreDineroContenedor: Color,
    val alerta: Color,
    val sobreAlerta: Color,
    val alertaContenedor: Color,
    val sobreAlertaContenedor: Color,
    val info: Color,
    val sobreInfo: Color,
    val infoContenedor: Color,
    val sobreInfoContenedor: Color,
    val scrim: Color
)

val ColoresOscuros = StockCubaSemanticColors(
    esOscuro = true,
    canvas = PaletaOscura.Canvas,
    panel = PaletaOscura.Panel,
    panelAlto = PaletaOscura.PanelAlto,
    panelHundido = PaletaOscura.PanelHundido,
    borde = PaletaOscura.Borde,
    bordeFuerte = PaletaOscura.BordeFuerte,
    texto = PaletaOscura.Texto,
    textoSecundario = PaletaOscura.TextoSecundario,
    textoDebil = PaletaOscura.TextoDebil,
    acento = PaletaOscura.Acento,
    sobreAcento = PaletaOscura.SobreAcento,
    acentoContenedor = PaletaOscura.AcentoContenedor,
    sobreAcentoContenedor = PaletaOscura.SobreAcentoContenedor,
    dinero = PaletaOscura.Dinero,
    sobreDinero = PaletaOscura.SobreDinero,
    dineroContenedor = PaletaOscura.DineroContenedor,
    sobreDineroContenedor = PaletaOscura.SobreDineroContenedor,
    alerta = PaletaOscura.Alerta,
    sobreAlerta = PaletaOscura.SobreAlerta,
    alertaContenedor = PaletaOscura.AlertaContenedor,
    sobreAlertaContenedor = PaletaOscura.SobreAlertaContenedor,
    info = PaletaOscura.Info,
    sobreInfo = PaletaOscura.SobreInfo,
    infoContenedor = PaletaOscura.InfoContenedor,
    sobreInfoContenedor = PaletaOscura.SobreInfoContenedor,
    scrim = PaletaOscura.Scrim
)

val ColoresClaros = StockCubaSemanticColors(
    esOscuro = false,
    canvas = PaletaClara.Canvas,
    panel = PaletaClara.Panel,
    panelAlto = PaletaClara.PanelAlto,
    panelHundido = PaletaClara.PanelHundido,
    borde = PaletaClara.Borde,
    bordeFuerte = PaletaClara.BordeFuerte,
    texto = PaletaClara.Texto,
    textoSecundario = PaletaClara.TextoSecundario,
    textoDebil = PaletaClara.TextoDebil,
    acento = PaletaClara.Acento,
    sobreAcento = PaletaClara.SobreAcento,
    acentoContenedor = PaletaClara.AcentoContenedor,
    sobreAcentoContenedor = PaletaClara.SobreAcentoContenedor,
    dinero = PaletaClara.Dinero,
    sobreDinero = PaletaClara.SobreDinero,
    dineroContenedor = PaletaClara.DineroContenedor,
    sobreDineroContenedor = PaletaClara.SobreDineroContenedor,
    alerta = PaletaClara.Alerta,
    sobreAlerta = PaletaClara.SobreAlerta,
    alertaContenedor = PaletaClara.AlertaContenedor,
    sobreAlertaContenedor = PaletaClara.SobreAlertaContenedor,
    info = PaletaClara.Info,
    sobreInfo = PaletaClara.SobreInfo,
    infoContenedor = PaletaClara.InfoContenedor,
    sobreInfoContenedor = PaletaClara.SobreInfoContenedor,
    scrim = PaletaClara.Scrim
)

/** Colores semánticos del tema activo. */
val LocalStockCubaColors = staticCompositionLocalOf { ColoresOscuros }

// =====================================================================================
// ACCESO A COLORES
// =====================================================================================

/**
 * Acceso directo a los colores semánticos del tema activo.
 * Uso: `ScColors.acento`, `ScColors.dinero`, `ScColors.panel`...
 */
object ScColors {
    val esOscuro: Boolean @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.esOscuro
    val canvas: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.canvas
    val panel: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.panel
    val panelAlto: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.panelAlto
    val panelHundido: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.panelHundido
    val borde: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.borde
    val bordeFuerte: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.bordeFuerte
    val texto: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.texto
    val textoSecundario: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.textoSecundario
    val textoDebil: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.textoDebil
    val acento: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.acento
    val sobreAcento: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.sobreAcento
    val acentoContenedor: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.acentoContenedor
    val sobreAcentoContenedor: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.sobreAcentoContenedor
    val dinero: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.dinero
    val sobreDinero: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.sobreDinero
    val dineroContenedor: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.dineroContenedor
    val sobreDineroContenedor: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.sobreDineroContenedor
    val alerta: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.alerta
    val sobreAlerta: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.sobreAlerta
    val alertaContenedor: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.alertaContenedor
    val sobreAlertaContenedor: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.sobreAlertaContenedor
    val info: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.info
    val sobreInfo: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.sobreInfo
    val infoContenedor: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.infoContenedor
    val sobreInfoContenedor: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.sobreInfoContenedor
    val scrim: Color @Composable @ReadOnlyComposable get() = LocalStockCubaColors.current.scrim
}

/**
 * Compatibilidad: alias histórico usado por las pantallas existentes.
 * Cada propiedad lee el tema activo, así que las pantallas antiguas
 * se adaptan solas a claro/oscuro.
 */
object StockCubaColors {
    // Marca / fondo
    val FondoPrincipal: Color @Composable @ReadOnlyComposable get() = ScColors.panel
    val FondoCanvas: Color @Composable @ReadOnlyComposable get() = ScColors.canvas

    // Positivo / acción
    val VerdeExito: Color @Composable @ReadOnlyComposable get() = ScColors.acento
    val VerdeExitoHover: Color @Composable @ReadOnlyComposable get() = ScColors.acento
    val VerdeExitoContainer: Color @Composable @ReadOnlyComposable get() = ScColors.acentoContenedor

    // Alerta / riesgo
    val CoralAlerta: Color @Composable @ReadOnlyComposable get() = ScColors.alerta
    val CoralAlertaHover: Color @Composable @ReadOnlyComposable get() = ScColors.alerta
    val CoralAlertaContainer: Color @Composable @ReadOnlyComposable get() = ScColors.alertaContenedor

    // Superficies
    val SuperficieL1: Color @Composable @ReadOnlyComposable get() = ScColors.panel
    val SuperficieL2: Color @Composable @ReadOnlyComposable get() = ScColors.panelAlto
    val SuperficieElevada: Color @Composable @ReadOnlyComposable get() = ScColors.panelAlto
    val BordeSutil: Color @Composable @ReadOnlyComposable get() = ScColors.borde
    val DivisorLista: Color @Composable @ReadOnlyComposable get() = ScColors.borde

    // Texto
    val TextoPrimario: Color @Composable @ReadOnlyComposable get() = ScColors.texto
    val TextoSecundario: Color @Composable @ReadOnlyComposable get() = ScColors.textoSecundario
    val TextoMuted: Color @Composable @ReadOnlyComposable get() = ScColors.textoDebil

    // Chips de stock
    val ChipStockAltoFondo: Color @Composable @ReadOnlyComposable get() = ScColors.acento.copy(alpha = 0.14f)
    val ChipStockAltoTexto: Color @Composable @ReadOnlyComposable get() = ScColors.acento
    val ChipStockBajoFondo: Color @Composable @ReadOnlyComposable get() = ScColors.alerta.copy(alpha = 0.14f)
    val ChipStockBajoTexto: Color @Composable @ReadOnlyComposable get() = ScColors.alerta
    val ChipStockMedioFondo: Color @Composable @ReadOnlyComposable get() = ScColors.dinero.copy(alpha = 0.14f)
    val ChipStockMedioTexto: Color @Composable @ReadOnlyComposable get() = ScColors.dinero

    // Acción principal (FAB / botón)
    val FabPrimarioFondo: Color @Composable @ReadOnlyComposable get() = ScColors.acento
    val FabPrimarioTexto: Color @Composable @ReadOnlyComposable get() = ScColors.sobreAcento
    val FabPrimarioGlow: Color @Composable @ReadOnlyComposable get() = ScColors.acento.copy(alpha = 0.18f)
    val FabFondo: Color @Composable @ReadOnlyComposable get() = ScColors.acento
    val FabTexto: Color @Composable @ReadOnlyComposable get() = ScColors.sobreAcento
    val FabGlow: Color @Composable @ReadOnlyComposable get() = ScColors.acento.copy(alpha = 0.18f)

    // Inputs
    val InputFondo: Color @Composable @ReadOnlyComposable get() = ScColors.panelAlto
    val InputBorde: Color @Composable @ReadOnlyComposable get() = ScColors.borde
    val InputBordeFocus: Color @Composable @ReadOnlyComposable get() = ScColors.acento
    val InputPlaceholder: Color @Composable @ReadOnlyComposable get() = ScColors.textoDebil

    // Modales
    val ModalFondo: Color @Composable @ReadOnlyComposable get() = ScColors.canvas.copy(alpha = 0.82f)
    val ModalBlur: Int @Composable @ReadOnlyComposable get() = 20
    val ModalSombraGlow: Color @Composable @ReadOnlyComposable get() = ScColors.acento.copy(alpha = 0.12f)

    // Semánticos extra
    val AzulPrincipal: Color @Composable @ReadOnlyComposable get() = ScColors.acento
    val AmarilloOro: Color @Composable @ReadOnlyComposable get() = ScColors.dinero
    val NaranjaAdvertencia: Color @Composable @ReadOnlyComposable get() = ScColors.dinero
    val RojoPeligro: Color @Composable @ReadOnlyComposable get() = ScColors.alerta
    val IndigoMarca: Color @Composable @ReadOnlyComposable get() = ScColors.info
}
