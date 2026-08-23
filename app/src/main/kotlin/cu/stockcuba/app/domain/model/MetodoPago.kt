package cu.stockcuba.app.domain.model

enum class MetodoPago {
    EFECTIVO,
    TRANSFERENCIA,
    MIXTO
}

fun MetodoPago.nombre(): String = when (this) {
    MetodoPago.EFECTIVO -> "Efectivo"
    MetodoPago.TRANSFERENCIA -> "Transferencia"
    MetodoPago.MIXTO -> "Mixto"
}