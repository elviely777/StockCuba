package cu.stockcuba.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "nombre")
    val nombre: String,

    @ColumnInfo(name = "ci")
    val ci: String,

    @ColumnInfo(name = "telefono")
    val telefono: String?,

    @ColumnInfo(name = "notas")
    val notas: String?,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "activo", defaultValue = "1")
    val activo: Boolean = true
)