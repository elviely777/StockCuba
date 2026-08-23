package cu.stockcuba.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Colores semánticos basados en DESIGN.md de Stitch.
 * Modo oscuro es el principal (diseño inmersivo para largas jornadas de inventario).
 */

// ===== COLORES BASE (Brand) =====
// Fondo principal: Deep Navy/Charcoal
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
val BordeSutil = Color(0xFF334155)   // 1px inner border para cards
val DivisorLista = Color(0xFF334155) // 0.5px horizontal dividers

// ===== TIPOGRAFÍA =====
val TextoPrimario = Color(0xFFF8FAFC)   // Near white - máxima legibilidad
val TextoSecundario = Color(0xFF94A3B8) // Muted gray - labels y metadata
val TextoMuted = Color(0xFF64748B)      // Aún más sutil

// ===== ESTADOS FUNCIONALES (Chips/Badges) =====
val ChipStockAltoFondo = Color(0x1A2DD4BF) // 10% Teal
val ChipStockAltoTexto = Color(0xFF2DD4BF)
val ChipStockBajoFondo = Color(0x1AFB7185)  // 10% Coral
val ChipStockBajoTexto = Color(0xFFFB7185)
val ChipStockMedioFondo = Color(0x1A94A3B8) // 10% Gray
val ChipStockMedioTexto = Color(0xFF94A3B8)

// ===== FAB / ACCIONES PRINCIPALES =====
val FabPrimarioFondo = Color(0xFF2DD4BF)
val FabPrimarioTexto = Color(0xFF0F172A)
val FabPrimarioGlow = Color(0x1A2DD4BF) // Para drop-shadow glow

// ===== INPUTS =====
val InputFondo = Color(0xFF1E293B)
val InputBorde = Color(0xFF334155)
val InputBordeFocus = Color(0xFF2DD4BF) // 2px Teal bottom border/ring on focus
val InputPlaceholder = Color(0xFF64748B)

// ===== MODALS / POPOVERS (Level 2) =====
val ModalFondo = Color(0xB30F172A) // 70% opacity fondo base
val ModalBlur = 20 // backdrop-blur 20px
val ModalSombraGlow = Color(0x1A2DD4BF) // Teal o Coral drop-shadow glow

// ===== MAPEO MATERIAL 3 COLOR SCHEME (DARK) =====
// Estos se usan directamente en darkColorScheme()

/**
 * Esquema de colores Dark (principal) para Material 3.
 * Mapea los tokens de DESIGN.md a los slots semánticos de M3.
 */
val EsquemaColorDark = mapOf(
    // Primary - Teal (Éxito/Crecimiento)
    "primary" to VerdeExito,                    // #2DD4BF
    "onPrimary" to Color(0xFF001E1C),           // Sobre primary
    "primaryContainer" to VerdeExitoContainer,  // #0F766E
    "onPrimaryContainer" to Color(0xFFD1FAF5),

    // Secondary - Coral (Alerta) - usado como secondary en M3
    "secondary" to CoralAlerta,                 // #FB7185
    "onSecondary" to Color(0xFF4D0019),
    "secondaryContainer" to CoralAlertaContainer, // #9F1239
    "onSecondaryContainer" to Color(0xFFFCE7EC),

    // Tertiary - Brand/Accent adicional
    "tertiary" to Color(0xFF6366F1),            // Indigo para acentos adicionales
    "onTertiary" to Color(0xFFFFFFFF),
    "tertiaryContainer" to Color(0xFF312E81),
    "onTertiaryContainer" to Color(0xFFE0E7FF),

    // Error - Semantic error
    "error" to Color(0xFFFCA5A5),               // Red-300
    "onError" to Color(0xFF7F1D1D),
    "errorContainer" to Color(0xFF991B1B),
    "onErrorContainer" to Color(0xFFFEF2F2),

    // Surface - Fondos principales
    "surface" to FondoCanvas,                   // #051424
    "onSurface" to TextoPrimario,               // #F8FAFC
    "surfaceDim" to Color(0xFF010F1F),          // surface-dim
    "surfaceBright" to Color(0xFF1E293B),       // SuperficieL1
    "surfaceContainerLowest" to FondoPrincipal, // #0F172A
    "surfaceContainerLow" to Color(0xFF0D1C2D),
    "surfaceContainer" to SuperficieL1,         // #1E293B
    "surfaceContainerHigh" to Color(0xFF1C2B3C),
    "surfaceContainerHighest" to SuperficieL2,  // #334155

    // Outline
    "outline" to BordeSutil,                    // #334155
    "outlineVariant" to Color(0xFF475569),      // Slate-600

    // Inverse
    "inverseSurface" to TextoPrimario,
    "inverseOnSurface" to FondoCanvas,
    "inversePrimary" to VerdeExitoHover,

    // Background (deprecated en M3, usar surface)
    "background" to FondoCanvas,
    "onBackground" to TextoPrimario,

    // Surface variant
    "surfaceVariant" to SuperficieL1,
    "onSurfaceVariant" to TextoSecundario,

    // Scrim
    "scrim" to Color(0xFF000000)
)

/**
 * Esquema de colores Light (fallback) - derivado del dark invirtiendo.
 * Mantenido por compatibilidad pero no es el diseño principal.
 */
val EsquemaColorLight = mapOf(
    "primary" to Color(0xFF0F766E),       // Teal-800
    "onPrimary" to Color(0xFFFFFFFF),
    "primaryContainer" to Color(0xFFCCFBF1),
    "onPrimaryContainer" to Color(0xFF00201C),

    "secondary" to Color(0xFF9F1239),     // Rose-800
    "onSecondary" to Color(0xFFFFFFFF),
    "secondaryContainer" to Color(0xFFFCE7EC),
    "onSecondaryContainer" to Color(0xFF4D0019),

    "tertiary" to Color(0xFF312E81),      // Indigo-800
    "onTertiary" to Color(0xFFFFFFFF),
    "tertiaryContainer" to Color(0xFFE0E7FF),
    "onTertiaryContainer" to Color(0xFF1E1B4B),

    "error" to Color(0xFFB91C1C),
    "onError" to Color(0xFFFFFFFF),
    "errorContainer" to Color(0xFFFEF2F2),
    "onErrorContainer" to Color(0xFF7F1D1D),

    "surface" to Color(0xFFF8FAFC),       // Slate-50
    "onSurface" to Color(0xFF0F172A),
    "surfaceDim" to Color(0xFFF1F5F9),
    "surfaceBright" to Color(0xFFFFFFFF),
    "surfaceContainerLowest" to Color(0xFFFFFFFF),
    "surfaceContainerLow" to Color(0xFFF1F5F9),
    "surfaceContainer" to Color(0xFFE2E8F0),
    "surfaceContainerHigh" to Color(0xFFCBD5E1),
    "surfaceContainerHighest" to Color(0xFF94A3B8),

    "outline" to Color(0xFF94A3B8),
    "outlineVariant" to Color(0xFFCBD5E1),

    "inverseSurface" to Color(0xFF0F172A),
    "inverseOnSurface" to Color(0xFFF8FAFC),
    "inversePrimary" to Color(0xFF2DD4BF),

    "background" to Color(0xFFF8FAFC),
    "onBackground" to Color(0xFF0F172A),

    "surfaceVariant" to Color(0xFFE2E8F0),
    "onSurfaceVariant" to Color(0xFF475569),

    "scrim" to Color(0xFF000000)
)