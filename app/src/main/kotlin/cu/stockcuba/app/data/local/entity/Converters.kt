package cu.stockcuba.app.data.local.entity

import androidx.room.TypeConverter
import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.TipoMovimientoInventario
import cu.stockcuba.app.domain.model.UnidadMedida
import java.time.Instant

class Converters {

    @TypeConverter
    fun fromInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun fromUnidadMedida(value: String?): UnidadMedida? = value?.let { UnidadMedida.valueOf(it) }

    @TypeConverter
    fun toUnidadMedida(value: UnidadMedida?): String? = value?.name

    @TypeConverter
    fun fromMetodoPago(value: String?): MetodoPago? = value?.let { MetodoPago.valueOf(it) }

    @TypeConverter
    fun toMetodoPago(value: MetodoPago?): String? = value?.name

    @TypeConverter
    fun fromTipoMovimiento(value: String?): TipoMovimientoInventario? = value?.let { TipoMovimientoInventario.valueOf(it) }

    @TypeConverter
    fun toTipoMovimiento(value: TipoMovimientoInventario?): String? = value?.name
}