package cu.stockcuba.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class VentaWithItems(
    @Embedded val venta: VentaEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "venta_id"
    )
    val items: List<VentaItemEntity>
)