package cu.stockcuba.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "abonos",
    foreignKeys = [
        ForeignKey(
            entity = ClienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["cliente_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("cliente_id"),
        Index("fecha")
    ]
)
data class AbonoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "cliente_id")
    val clienteId: String,

    @ColumnInfo(name = "monto")
    val monto: Double,

    @ColumnInfo(name = "fecha")
    val fecha: Long, // Epoch millis

    @ColumnInfo(name = "metodo_pago")
    val metodoPago: String, // MetodoPago.name

    @ColumnInfo(name = "notas")
    val notas: String = "",

    @ColumnInfo(name = "sync_status", defaultValue = "'PENDING'")
    val syncStatus: String = "PENDING",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
