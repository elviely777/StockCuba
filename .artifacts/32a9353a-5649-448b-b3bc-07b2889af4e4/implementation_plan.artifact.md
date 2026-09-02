# Finalización de la Migración a Supabase y Limpieza del Proyecto

Este plan completa la migración de Firebase a Supabase, estabiliza la capa de datos y añade la integración visual de facturación en el Dashboard.

## Cambios Propuestos

### 1. Estabilización de la Capa de Datos (Supabase)

#### [MODIFY] [SupabaseSyncRepository.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/data/supabase/SupabaseSyncRepository.kt)
- Eliminar importaciones duplicadas.
- Optimizar `subirVenta`: evitar re-crear el objeto `SupabaseVenta` dentro de `setBody`.
- Optimizar `actualizarFacturacionMaestra`: simplificar la lógica de actualización y manejo de errores.
- Asegurar que `ventas_procesadas` se maneje correctamente como una lista de IDs (UUIDs).

#### [MODIFY] [SupabaseBusinessRepository.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/data/supabase/SupabaseBusinessRepository.kt)
- Corregir el bug en `vincular`: generar un único `posId` y usarlo consistentemente para el registro y el guardado local.
- Eliminar código redundante en la serialización manual dentro de `setBody` si es posible.
- Asegurar que `ajustesDataStore.guardarVinculacion` reciba los IDs correctos.

#### [MODIFY] [SupabaseModule.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/di/SupabaseModule.kt)
- Cambiar a una interfaz abstracta con `@Binds` para inyectar `SupabaseBusinessRepository` como `BusinessRepository`.
- Limpiar proveedores innecesarios de repositorios concretos que ya tienen `@Inject`.

### 2. Integración Visual (UI)

#### [MODIFY] [DashboardScreen.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/dashboard/DashboardScreen.kt)
- Implementar el componente `FacturacionEstimadaCard`.
- Integrarlo en el `DashboardContenidoFull` justo después del Balance de Pagos.

### 3. Limpieza Final

#### [MODIFY] [build.gradle.kts](file:///D:/StockCuba/StockCuba2/app/build.gradle.kts)
- Eliminar definitivamente las líneas comentadas de Firebase para limpiar el archivo.

## Plan de Verificación

### Verificación Automatizada
- Ejecutar `gradlew assembleDebug` para asegurar que las inyecciones de Hilt y la compilación son correctas.

### Verificación Manual
- **Vincular Negocio**: Probar la vinculación y verificar en Supabase que el `negocio_id` y `pos_id` coinciden con lo guardado en el dispositivo (Logs).
- **Sincronización**: Realizar una venta y verificar que se sube a `ventas` y `venta_items`.
- **Facturación**: Verificar que `facturacion_maestra` se actualiza y el Dashboard muestra el monto estimado correcto (500 + 3%).
