package cu.stockcuba.app.domain.model

data class Categoria(
    val id: String,
    val nombre: String,
    val color: Int // ARGB color int para UI (ej. 0xFF3700B3)
)