package cu.stockcuba.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gastos",
    indices = [
        Index("fecha"),
        Index("tipo")
    ]
)
data class GastoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "concepto")
    val concepto: String,

    @ColumnInfo(name = "tipo")
    val tipo: String, // TipoGasto.name via TypeConverter

    @ColumnInfo(name = "monto")
    val monto: Double,

    @ColumnInfo(name = "moneda")
    val moneda: String, // Moneda.name via TypeConverter

    @ColumnInfo(name = "fecha")
    val fecha: Long, // Epoch millis

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis()
)
