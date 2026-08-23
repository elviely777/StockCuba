# presentation/common/ — Componentes Compose Reutilizables

Widgets genéricos, sin lógica de negocio, compartidos entre features.

## Qué va aquí
- `Button.kt` — `PrimaryButton`, `SecondaryButton`, `IconButton`, estados (loading, disabled)
- `TextField.kt` — `OutlinedTextField` wrapper con label, error, placeholder
- `Card.kt` — `ProductCard`, `InfoCard`, `ClickableCard`
- `AppBar.kt` — `TopAppBar`, `BottomAppBar` configurables
- `Loading.kt` — `CircularProgress`, `ShimmerPlaceholder`, `EmptyState`
- `ErrorMessage.kt` — `SnackbarHost`, `InlineError`, `ErrorDialog`
- `Image.kt` — `AsyncImage` (Coil) con placeholders, error, contentDescription
- `Divider.kt`, `Spacer.kt`, `Chip.kt`, `Badge.kt`, `Avatar.kt`

## Qué NO va aquí
- Componentes ligados a un feature específico → `presentation/feature/<feature>/`
- Lógica de ViewModel, repositorios, use cases

## Convenciones
- `@Composable` functions públicas
- `Modifier` como primer parámetro opcional (`modifier: Modifier = Modifier`)
- `enabled`, `onClick`, `colors` como parámetros nombrados
- Preview `@Preview` en cada componente