package cu.stockcuba.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cierres_mensuales",
    indices = [
        Index("anio", "mes", unique = true)
    ]
)
data class CierreMensualEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "mes")
    val mes: Int,

    @ColumnInfo(name = "anio")
    val anio: Int,

    @ColumnInfo(name = "total_recaudado")
    val totalRecaudado: Double,

    @ColumnInfo(name = "total_efectivo")
    val totalEfectivo: Double,

    @ColumnInfo(name = "total_transferencia")
    val totalTransferencia: Double,

    @ColumnInfo(name = "cantidad_ventas")
    val cantidadVentas: Int,

    @ColumnInfo(name = "ipb")
    val ipb: Double,

    @ColumnInfo(name = "ipc")
    val ipc: Double,

    @ColumnInfo(name = "notas")
    val notas: String = "",

    @ColumnInfo(name = "fecha_cierre")
    val fechaCierre: Long
)
