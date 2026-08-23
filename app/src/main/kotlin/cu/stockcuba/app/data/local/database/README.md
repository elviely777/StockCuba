# data/local/database/ — StockCubaDatabase

Clase abstracta que extiende `RoomDatabase` y expone los DAOs.

## Contenido
- `StockCubaDatabase.kt` — `@Database(entities = [...], version = 1, exportSchema = false)`
- Abstract `productDao(): ProductDao`, etc.
- `Companion object` con `getInstance(context)` (singleton) o provisto por Hilt
- `RoomDatabase.Callback` para poblar datos iniciales / migraciones
- `TypeConverters` si se necesitan

## Migraciones
- `MIGRATION_1_2`, etc. en `companion object`
- `exportSchema = false` para no generar JSON en repo (o `true` si se versiona)