package cu.stockcuba.app.domain.model

import java.time.Instant

data class Abono(
    val id: String,
    val clienteId: String,
    val monto: Double,
    val fecha: Instant,
    val metodoPago: MetodoPago,
    val notas: String = ""
)
