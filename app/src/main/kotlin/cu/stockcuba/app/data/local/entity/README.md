# data/local/entity/ — Entidades Room

Modelos de persistencia **solo para Room**. Anotados con `@Entity`, `@PrimaryKey`, `@ColumnInfo`, etc.

## Reglas
- **No** exponer fuera de `data` — el dominio usa `domain.model`
- Nombres: `NombreEntity` (p.ej. `ProductEntity`, `UserEntity`)
- Incluyen campos técnicos: `createdAt`, `updatedAt`, `syncStatus`, etc.
- Relaciones vía `@Relation` o `@Embedded` en DAOs, no en la entidad directamente

## Mapeo
- Conversión a/desde dominio en `data/mapper/EntityMapper.kt`