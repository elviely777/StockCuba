package cu.stockcuba.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ventas",
    foreignKeys = [
        ForeignKey(
            entity = ClienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["cliente_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("cliente_id"),
        Index("fecha"),
        Index("metodo_pago")
    ]
)
data class VentaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "fecha")
    val fecha: Long, // Instant epoch millis

    @ColumnInfo(name = "total")
    val total: Double,

    @ColumnInfo(name = "metodo_pago")
    val metodoPago: String, // MetodoPago.name via TypeConverter

    @ColumnInfo(name = "cliente_id")
    val clienteId: String?,

    @ColumnInfo(name = "monto_efectivo")
    val montoEfectivo: Double = 0.0,

    @ColumnInfo(name = "monto_transferencia")
    val montoTransferencia: Double = 0.0,

    @ColumnInfo(name = "id_transferencia")
    val idTransferencia: String? = null,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "sync_status", defaultValue = "'SYNCED'")
    val syncStatus: String = "SYNCED"
)