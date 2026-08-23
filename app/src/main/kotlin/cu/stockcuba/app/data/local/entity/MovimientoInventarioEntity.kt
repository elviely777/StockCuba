package cu.stockcuba.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimientos_inventario",
    foreignKeys = [
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["producto_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("producto_id"),
        Index("fecha"),
        Index("tipo")
    ]
)
data class MovimientoInventarioEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "producto_id")
    val productoId: String,

    @ColumnInfo(name = "tipo")
    val tipo: String, // TipoMovimientoInventario.name via TypeConverter

    @ColumnInfo(name = "cantidad")
    val cantidad: Int,

    @ColumnInfo(name = "fecha")
    val fecha: Long, // Instant epoch millis

    @ColumnInfo(name = "motivo")
    val motivo: String?,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "sync_status", defaultValue = "'SYNCED'")
    val syncStatus: String = "SYNCED"
)