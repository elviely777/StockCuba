# data/local/dao/ — Data Access Objects (DAOs)

Interfaces anotadas con `@Dao` que definen operaciones de BD.

## Convenciones
- Un DAO por entidad principal (o grupo relacionado)
- Métodos `suspend` para escrituras; `Flow<>` para lecturas reactivas
- Nombres: `NombreDao` (p.ej. `ProductDao`, `InventoryDao`)
- Queries complejas en `@Query` con parámetros nombrados (`:productId`)

## Ejemplo
```kotlin
@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("SELECT * FROM products WHERE id = :id")
    fun getById(id: String): Flow<ProductEntity?>

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}
```