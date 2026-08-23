package cu.stockcuba.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "productos",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("categoria_id"),
        Index("activo"),
        Index("stock_actual")
    ]
)
data class ProductoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "descripcion")
    val descripcion: String?,

    @ColumnInfo(name = "precio_venta")
    val precioVenta: Double,

    @ColumnInfo(name = "costo_unitario")
    val costoUnitario: Double,

    @ColumnInfo(name = "stock_actual")
    val stockActual: Int,

    @ColumnInfo(name = "stock_minimo")
    val stockMinimo: Int,

    @ColumnInfo(name = "unidad_medida")
    val unidadMedida: String, // UnidadMedida.name via TypeConverter

    @ColumnInfo(name = "categoria_id")
    val categoriaId: String,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long, // Instant epoch millis via TypeConverter

    @ColumnInfo(name = "activo", defaultValue = "1")
    val activo: Boolean = true,

    @ColumnInfo(name = "fecha_actualizacion")
    val fechaActualizacion: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "sync_status", defaultValue = "'SYNCED'")
    val syncStatus: String = "SYNCED"
) {
    companion object {
        const val SYNC_STATUS_SYNCED = "SYNCED"
        const val SYNC_STATUS_PENDING = "PENDING"
        const val SYNC_STATUS_CONFLICT = "CONFLICT"
    }
}