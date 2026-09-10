package cu.stockcuba.app.domain.model

import java.time.Instant

data class Gasto(
    val id: String,
    val concepto: String,
    val tipo: TipoGasto,
    val monto: Double,
    val moneda: Moneda,
    val fecha: Instant
)
