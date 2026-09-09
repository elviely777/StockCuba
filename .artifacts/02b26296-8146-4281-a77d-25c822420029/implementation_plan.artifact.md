# Plan de Implementación: Roles de Usuario y Multi-moneda

Este plan detalla la reestructuración del Dashboard para admitir roles (Dueño vs. Vendedor) y la implementación de precios en múltiples monedas para los productos.

## User Review Required

> [!IMPORTANT]
> - **Migración de Base de Datos:** La adición del campo `moneda` en los productos requiere una migración de Room. Por defecto, los productos existentes se marcarán como `CUP` (o la moneda principal configurada).
> - **Seguridad de Roles:** El cambio de perfil de "Vendedor" a "Dueño" requerirá el PIN ya configurado en la aplicación. Si no hay PIN, el acceso será libre (se recomienda configurar uno).
> - **Tasas de Cambio:** Se añadirá una sección simple en Ajustes para definir el valor de USD, MLC y EUR respecto a la moneda base.

## Proposed Changes

### 1. Seguridad y Perfiles de Usuario

#### [NEW] [RolUsuario.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/domain/model/RolUsuario.kt)
* Definición del enum `RolUsuario { DUENO, VENDEDOR }`.

#### [MODIFY] [AjustesDataStore.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/ajustes/AjustesDataStore.kt)
* Añadir `ROL_ACTUAL_KEY` y métodos para persistir y leer el rol activo.

#### [MODIFY] [DashboardScreen.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/dashboard/DashboardScreen.kt)
* Integrar el rol en el estado de la UI.
* Ocultar condicionalmente los componentes: `MetaDelDiaCard`, `RentabilidadCard`, `ValorInventarioCard`, y el desglose de ganancias en `GridMetricas` cuando el rol sea `VENDEDOR`.
* Añadir un selector de perfil en la cabecera del Dashboard.

---

### 2. Soporte Multi-moneda en Productos

#### [MODIFY] [Producto.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/domain/model/Producto.kt)
* Añadir propiedad `moneda: Moneda`.

#### [MODIFY] [ProductoEntity.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/data/local/entity/ProductoEntity.kt)
* Añadir columna `moneda` (String).

#### [MODIFY] [Converters.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/data/local/entity/Converters.kt)
* Añadir TypeConverters para `Moneda`.

#### [MODIFY] [FormularioProductoScreen.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/productos/FormularioProductoScreen.kt)
* Añadir selector de moneda al lado de los campos de `Precio Venta` y `Costo`.

#### [MODIFY] [AjustesScreen.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/ajustes/AjustesScreen.kt)
* Añadir sección para configurar "Tasas de Cambio del Día".

---

### 3. Lógica de Negocio y Consolidación

#### [MODIFY] [DashboardViewModel.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/dashboard/DashboardViewModel.kt)
* Ajustar cálculos de totales para convertir precios de diferentes monedas a la moneda base usando las tasas configuradas.

## Verification Plan

### Automated Tests
* Pruebas unitarias para la conversión de monedas en el ViewModel.
* Pruebas de UI para verificar que los datos sensibles desaparecen al cambiar a rol `VENDEDOR`.

### Manual Verification
1. Entrar a Ajustes y definir tasas de cambio (ej. 1 USD = 350 CUP).
2. Crear un producto en USD y otro en CUP.
3. Vender ambos y verificar que el Dashboard sume correctamente el valor convertido.
4. Cambiar el perfil a "Vendedor" y confirmar que las ganancias y costos ya no son visibles.
