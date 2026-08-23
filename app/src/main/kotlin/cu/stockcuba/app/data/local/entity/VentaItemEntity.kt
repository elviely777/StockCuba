package cu.stockcuba.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "venta_items",
    primaryKeys = ["id"],
    foreignKeys = [
        ForeignKey(
            entity = VentaEntity::class,
            parentColumns = ["id"],
            childColumns = ["venta_id"],
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
        Index("venta_id"),
        Index("producto_id")
    ]
)
data class VentaItemEntity(
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "venta_id")
    val ventaId: String,

    @ColumnInfo(name = "producto_id")
    val productoId: String,

    @ColumnInfo(name = "nombre_producto")
    val nombreProducto: String, // snapshot

    @ColumnInfo(name = "cantidad")
    val cantidad: Int,

    @ColumnInfo(name = "precio_unitario")
    val precioUnitario: Double,

    @ColumnInfo(name = "subtotal")
    val subtotal: Double
)