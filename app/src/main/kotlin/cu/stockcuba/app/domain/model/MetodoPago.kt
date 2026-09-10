package cu.stockcuba.app.domain.model

enum class MetodoPago {
    EFECTIVO,
    TRANSFERENCIA,
    MIXTO,
    CREDITO
}

fun MetodoPago.nombre(): String = when (this) {
    MetodoPago.EFECTIVO -> "Efectivo"
    MetodoPago.TRANSFERENCIA -> "Transferencia"
    MetodoPago.MIXTO -> "Mixto"
    MetodoPago.CREDITO -> "Crédito"
}