# presentation/theme/ — Tema Material 3 (Stitch-generated)

Tokens de diseño y tema Compose. **Generado desde Stitch** (Google) o mantenido a mano.

## Archivos
- `Color.kt` — paleta `Color(0xFF...)`, light/dark schemes
- `Type.kt` — `Typography` con escalas Material 3 (display, headline, title, body, label)
- `Shape.kt` — `Shapes` (esquinas: small, medium, large, extraLarge)
- `Theme.kt` — `MaterialTheme` + `StockCuba` composable wrapper + `StockCubaPreview`

## Flujo Stitch
1. Diseñar en [stitch.withgoogle.com](https://stitch.withgoogle.com)
2. Exportar → `Design.md` → `create_design_system_from_design_md`
3. Copiar `Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt` aquí
4. Ajustar `StockCuba` wrapper si hace falta (edge-to-edge, status bar)

## Uso
```kotlin
// En cualquier Screen
StockCuba {
    // MaterialTheme.colorScheme, .typography, .shapes disponibles
}
```

## Nota
Los archivos existentes en `ui/theme/` (Color, Theme, Type) **se mueven aquí** y se actualizan imports.