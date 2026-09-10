package cu.stockcuba.app.data.mapper

import cu.stockcuba.app.data.local.entity.GastoEntity
import cu.stockcuba.app.domain.model.Gasto
import cu.stockcuba.app.domain.model.Moneda
import cu.stockcuba.app.domain.model.TipoGasto
import java.time.Instant

fun GastoEntity.toDomain(): Gasto = Gasto(
    id = id,
    concepto = concepto,
    tipo = TipoGasto.valueOf(tipo),
    monto = monto,
    moneda = Moneda.valueOf(moneda),
    fecha = Instant.ofEpochMilli(fecha)
)

fun Gasto.toEntity(): GastoEntity = GastoEntity(
    id = id,
    concepto = concepto,
    tipo = tipo.name,
    monto = monto,
    moneda = moneda.name,
    fecha = fecha.toEpochMilli()
)
