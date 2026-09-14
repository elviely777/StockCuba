# Implementación de Imágenes de Productos

Añadir la capacidad de capturar, almacenar y visualizar imágenes para cada producto en el inventario, utilizando Supabase Storage para la persistencia en la nube y Room para la caché local.

## User Review Required

> [!IMPORTANT]
> Se requiere crear un bucket llamado `product-images` en el panel de Supabase con acceso público de lectura para que las URLs funcionen correctamente.
> ¿Prefieres usar una librería externa para el selector de imágenes o el `ActivityResultLauncher` nativo de Compose? (Usaré el nativo por defecto por ser más ligero).

## Proposed Changes

### Capa de Datos (Persistencia)

#### [MODIFY] [ProductoEntity.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/data/local/entity/ProductoEntity.kt)
- Añadir campo `imagen_url: String? = null`.

#### [MODIFY] [StockCubaDatabase.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/data/local/database/StockCubaDatabase.kt)
- Incrementar versión a 14.
- Añadir `AutoMigration(from = 13, to = 14)`.

#### [MODIFY] [Producto.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/domain/model/Producto.kt)
- Añadir campo `imagenUrl: String?`.

#### [MODIFY] [ProductoMapper.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/data/mapper/ProductoMapper.kt)
- Actualizar mapeo entre Entity y Domain para incluir la imagen.

---

### Infraestructura (Supabase Storage)

#### [NEW] [StorageRepository.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/domain/repository/StorageRepository.kt)
- Interfaz para subir archivos.

#### [NEW] [SupabaseStorageRepository.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/data/supabase/SupabaseStorageRepository.kt)
- Implementación usando Ktor para subir imágenes al bucket de Supabase.

---

### Capa de Presentación (UI/UX)

#### [MODIFY] [FormularioProductoViewModel.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/productos/FormularioProductoViewModel.kt)
- Añadir lógica para manejar el URI de la imagen seleccionada.
- Implementar la subida antes de guardar el producto.

#### [MODIFY] [FormularioProductoScreen.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/productos/FormularioProductoScreen.kt)
- Añadir un componente visual para mostrar la imagen seleccionada.
- Botones para "Tomar Foto" y "Seleccionar de Galería".

#### [MODIFY] [ListaProductosScreen.kt](file:///D:/StockCuba/StockCuba2/app/src/main/kotlin/cu/stockcuba/app/presentation/productos/ListaProductosScreen.kt)
- Mostrar una miniatura de la imagen en cada item de la lista usando `AsyncImage` de Coil.

## Verification Plan

### Manual Verification
1. Abrir formulario de producto.
2. Seleccionar una imagen de la galería.
3. Guardar el producto y verificar que se sube a Supabase.
4. Verificar que la imagen aparece en la lista principal.
5. Editar un producto y cambiar su imagen.
