---
name: StockCuba · Mostrador
version: 2.0
modo_principal: oscuro
---

# StockCuba · Sistema de diseño "Mostrador"

## Brand & Style

StockCuba es una herramienta de mostrador: se usa de pie, con una mano, en una tienda
con luz variable y con prisa. El diseño no busca parecer un panel SaaS: busca parecer un
**instrumento**. Superficie oscura y calmada, paneles con borde de un pixel, jerarquía
tipográfica fuerte y color que siempre significa algo.

- **Modo oscuro principal**: fondo de tinta azul-negro (no negro puro) para reducir fatiga
  en jornadas largas y evitar el look "OLED genérico".
- **Modo claro secundario**: papel frío (no crema) con los mismos acentos oscurecidos para
  mantener contraste AA. No es una inversión automática del oscuro.
- **Profundidad tonal, no sombras**: la elevación se comunica con capas (lienzo → panel →
  panel alto) y un borde de 1px. No hay sombras difusas en el sistema.
- **Un momento audaz por pantalla**: la cifra del dinero del periodo. El resto guarda silencio.

## Colores

### Paleta oscura (principal)

| Token | Hex | Uso |
|---|---|---|
| `canvas` | `#050D13` | Lienzo de la app |
| `panel` | `#0A1720` | Paneles y superficies nivel 1 |
| `panelAlto` | `#102330` | Campos, controles, chips |
| `panelHundido` | `#0D1E29` | Rieles de barras, zonas hundidas |
| `borde` | `#1B303C` | Borde de 1px de todo el sistema |
| `bordeFuerte` | `#264555` | Foco y selección |
| `texto` | `#E9F1F4` | Texto primario |
| `textoSecundario` | `#93A8B3` | Etiquetas y metadata |
| `textoDebil` | `#5C7481` | Placeholders e inactivos |
| `acento` (jade) | `#3BE3B0` | Acción primaria, stock sano, positivo |
| `sobreAcento` | `#03211A` | Texto sobre jade |
| `dinero` (ámbar) | `#F2B544` | Importes, totales, atención |
| `alerta` (coral) | `#FF7A6B` | Riesgo, agotado, error |
| `info` (azul) | `#7FB2FF` | Información neutra, transferencias |

### Paleta clara (secundaria)

| Token | Hex | Uso |
|---|---|---|
| `canvas` | `#F2F5F6` | Papel frío |
| `panel` | `#FFFFFF` | Paneles |
| `panelAlto` | `#EAF0F2` | Campos y controles |
| `borde` | `#D6E1E5` | Bordes de 1px |
| `texto` | `#08161D` | Texto primario |
| `textoSecundario` | `#4C6570` | Etiquetas |
| `acento` | `#07795C` | Acción / positivo |
| `dinero` | `#8A5A00` | Importes |
| `alerta` | `#B3261E` | Riesgo |
| `info` | `#1D5FA8` | Información |

**Regla**: ninguna pantalla escribe un color literal. Todo sale de `ScColors` (tokens
semánticos que leen el tema activo) o de `MaterialTheme.colorScheme`.

## Tipografía

**Inter** incluida en la app como fuente variable (`res/font/inter_variable.ttf`).
Una sola familia; el carácter viene del contraste de pesos y del tracking negativo en
tamaños grandes. Cifras siempre tabulares (`tnum`) para que los importes no "bailen".

| Rol | Estilo | Tamaño / interlínea |
|---|---|---|
| Cifra protagonista | `displayLarge` ExtraBold, -0.03em | 40 / 46 |
| Cifra secundaria | `displaySmall` Bold | 28 / 34 |
| Título de pantalla | `headlineLarge` Bold, -0.02em | 26 / 32 |
| Título de panel | `titleMedium` SemiBold | 16 / 22 |
| Texto de fila | `bodyMedium` Regular | 15 / 22 |
| Metadata | `bodySmall` Regular | 13 / 20 |
| Etiqueta de dato | `labelMedium` Medium, +0.04em | 12 / 16 |

## Espaciado y rejilla

Base 4dp. Márgenes laterales de pantalla **20dp**; separación entre paneles **16dp**;
relleno interno de panel **16–18dp**. Contenido agrupado en módulos, no en una lista de
tarjetas sueltas. Objetivos táctiles ≥ 44dp.

## Formas

- `Shape.Mediano` (12dp): controles (botones, campos, filas pulsables).
- `Shape.Grande` (16dp): paneles de datos.
- `Shape.Pequeno` (8dp): miniaturas, cajas de icono.
- `Shape.Full`: solo pastillas de estado, avatares e indicadores.

Los botones **no** son pastillas: son rectángulos redondeados de 12dp, para que un botón
no se confunda con una tarjeta.

## Componentes

Biblioteca compartida en `presentation/common/StockCubaUi.kt`:

- `ScPanel` — superficie base con borde de 1px y sin sombra.
- `ScScreenHeader` — título grande + subtítulo + acciones.
- `ScSectionHeader` — título de sección con acción ("Ver todo").
- `ScMoney` — importe tabular + unidad pequeña al lado.
- `ScMetricCell` — celda de métrica para retículas con divisores.
- `ScStatusPill` — estado con punto de color (jade/ámbar/coral/azul).
- `ScLevelBar` — barra de nivel animada (stock, metas).
- `ScPrimaryButton` / `ScSecondaryButton` / `ScIconButton`.
- `ScSearchField` — buscador con fondo de panel, sin línea de Material.
- `ScEmptyState` — estado vacío que explica y ofrece el siguiente paso.
- `ScBottomDock` — dock inferior con marca jade en el destino activo.

## Navegación

Dock inferior de cinco destinos: Panel · Ventas · Productos · Inventario · Más.
Una regla de 1px separa el dock del contenido; el destino activo se marca con una barra
jade de 2px sobre el icono y el color jade en icono y etiqueta.

## Movimiento

Mínimo y funcional: barras de nivel que animan su valor (420ms), marca del dock (180ms),
cambios de selección. Sin animaciones de entrada por sección ni hover decorativo.

## Anti-patrones (prohibidos en este sistema)

- Emojis como iconos.
- Sombras difusas y tarjetas con la misma elevación para todo.
- Pastillas para todo (botones, tarjetas, chips idénticos).
- Colores literales en pantallas.
- Estados vacíos mudos ("—", pantalla en blanco).
- Etiquetas en MAYÚSCULAS con tracking como decoración.
- Texto por debajo de 13sp en cuerpo.
