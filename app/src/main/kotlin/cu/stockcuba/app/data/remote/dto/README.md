# data/remote/dto/ — Data Transfer Objects (Red)

Modelos de serialización **solo para Retrofit/Moshi**. Anotados con `@JsonClass(generateAdapter = true)` y `@Json(name = "...")`.

## Reglas
- **No** exponer fuera de `data` — el dominio usa `domain.model`
- Nombres: `NombreDto` (p.ej. `ProductDto`, `LoginResponseDto`)
- Campos exactamente como vienen del backend (snake_case, nullable según API)
- Sin lógica de negocio, solo datos

## Mapeo
- Conversión a/desde dominio en `data/mapper/DtoMapper.kt`
- Moshi genera adaptadores via KSP (`moshi-kotlin-codegen`)