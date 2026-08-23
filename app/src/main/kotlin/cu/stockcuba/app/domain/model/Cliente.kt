package cu.stockcuba.app.domain.model

import java.time.Instant

data class Cliente(
    val id: String,
    val nombre: String,
    val telefono: String?,
    val notas: String?
)