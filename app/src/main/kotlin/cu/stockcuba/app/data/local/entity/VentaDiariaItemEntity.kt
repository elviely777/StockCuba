package cu.stockcuba.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "venta_diaria_items",
    foreignKeys = [
        ForeignKey(
            entity = VentaDiariaResumenEntity::class,
            parentColumns = ["fecha"],
            childColumns = ["fecha"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["producto_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("fecha"),
        Index("producto_id")
    ]
)
data class VentaDiariaItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "fecha")
    val fecha: Long, // epoch millis at start of day referencing ventas_diarias_resumen

    @ColumnInfo(name = "producto_id")
    val productoId: String,

    @ColumnInfo(name = "nombre_producto")
    val nombreProducto: String,

    @ColumnInfo(name = "cantidad")
    val cantidad: Int,

    @ColumnInfo(name = "precio_unitario")
    val precioUnitario: Double,

    @ColumnInfo(name = "subtotal")
    val subtotal: Double
)
