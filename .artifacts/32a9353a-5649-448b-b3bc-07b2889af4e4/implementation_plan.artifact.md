# Implementación de Rentabilidad en Dashboard y Reportes

Este plan detalla los cambios para incluir los Gastos (Costo de Mercancía Vendida) y la Ganancia Real en el Dashboard y en el Reporte Mensual de Excel, además de incrementar la versión de la app a 1.1.2.

## Recordatorio: Bloqueo de Negocios
El sistema de bloqueo funciona de la siguiente manera:
1. **Verificación**: Durante la sincronización o al inicio, la app llama a `SupabaseBusinessRepository#verificarEstadoRemoto()`.
2. **Consulta**: Se consulta la tabla `negocios` en Supabase filtrando por el `businessId` del cliente.
3. **Persistencia**: Si el campo `estado` en la base de datos es distinto de `"ACTIVO"`, este estado se guarda localmente en el `AjustesDataStore`.
4. **Bloqueo**: La `MainActivity` observa este flujo y, si el estado no es `"ACTIVO"`, muestra una pantalla de bloqueo que impide usar la aplicación hasta que el administrador (tú) cambie el estado en Supabase.

---

## Cambios Propuestos

### [Capa de UI y Presentación]

#### [MODIFY] [DashboardUiState.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/dashboard/DashboardUiState.kt)
- Añadir `totalGastos: Double` y `gananciaReal: Double` al estado `Success`.

#### [MODIFY] [DashboardViewModel.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/dashboard/DashboardViewModel.kt)
- Calcular `totalGastos` sumando `costoUnitario * cantidad` de todos los items de las ventas en el periodo seleccionado.
- Calcular `gananciaReal` como la resta de `totalVendido - totalGastos`.

#### [MODIFY] [DashboardScreen.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/dashboard/DashboardScreen.kt)
- Añadir un nuevo componente `RentabilidadCard` que muestre los Gastos y la Ganancia Real del periodo.
- Insertar esta tarjeta en la lista del Dashboard, preferiblemente después del balance de caja.

### [Capa de Datos y Reportes]

#### [MODIFY] [ReportRepositoryImpl.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/data/repository/ReportRepositoryImpl.kt)
- En `generarReporteMensualXlsx`, calcular el costo total de las ventas del mes.
- Añadir filas al "Resumen Mensual" para "Costo de Mercancía Vendida" y "Ganancia Real del Mes".

### [Configuración del Proyecto]

#### [MODIFY] [build.gradle.kts](file:///D:/StockCuba/StockCuba2/app/build.gradle.kts)
- Incrementar `versionPatch` de `1` a `2` para establecer la versión en **1.1.2**.

---

## Plan de Verificación

### Pruebas Manuales
- Abrir el Dashboard y verificar que aparezcan los nuevos valores de Gastos y Ganancia.
- Cambiar el rango de tiempo (Hoy, Semana, Mes) y validar que los cálculos se actualicen.
- Generar un Reporte Mensual y comprobar que el archivo Excel incluya las nuevas filas en el resumen.
- Verificar en "Acerca de" que la versión mostrada sea **1.1.2**.
