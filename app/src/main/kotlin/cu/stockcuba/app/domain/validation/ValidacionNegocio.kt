package cu.stockcuba.app.domain.validation

import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import java.util.regex.Pattern

/**
 * Valida el nombre del negocio.
 * Reglas: obligatorio, máximo 100 caracteres.
 */
fun validarNombre(nombre: String): Result<String> {
    val trimmed = nombre.trim()
    if (trimmed.isBlank()) {
        return Result.Failure(DomainError.ValidationError("nombre", "El nombre es obligatorio"))
    }
    if (trimmed.length > 100) {
        return Result.Failure(DomainError.ValidationError("nombre", "Máximo 100 caracteres"))
    }
    return Result.Success(trimmed)
}

/**
 * Valida el teléfono en formato cubano.
 * Formatos aceptados: +53 5XXXXXXX, 53 5XXXXXXX, 5XXXXXXX (8 dígitos después del prefijo opcional)
 * Regex: ^(\+53|53)?[0-9]{8}$
 */
fun validarTelefono(telefono: String): Result<String> {
    val trimmed = telefono.trim()
    if (trimmed.isBlank()) {
        return Result.Failure(DomainError.ValidationError("telefono", "Formato: +53 5 XXX XXXX"))
    }
    val cubanPhoneRegex = Pattern.compile("^(\\+53|53)?[0-9]{8}$")
    if (!cubanPhoneRegex.matcher(trimmed).matches()) {
        return Result.Failure(DomainError.ValidationError("telefono", "Formato: +53 5 XXX XXXX"))
    }
    return Result.Success(trimmed)
}

/**
 * Valida el impuesto.
 * Reglas: numérico, rango 0-100 inclusive, máximo 2 decimales.
 */
fun validarImpuesto(impuesto: String): Result<Double> {
    val trimmed = impuesto.trim()
    if (trimmed.isBlank()) {
        return Result.Failure(DomainError.ValidationError("impuesto", "Debe ser entre 0 y 100"))
    }

    // Verificar máximo 2 decimales
    if (trimmed.contains(".")) {
        val decimals = trimmed.substringAfter(".").length
        if (decimals > 2) {
            return Result.Failure(DomainError.ValidationError("impuesto", "Máximo 2 decimales"))
        }
    }

    val valor = trimmed.toDoubleOrNull()
    if (valor == null) {
        return Result.Failure(DomainError.ValidationError("impuesto", "Debe ser un número válido"))
    }

    if (valor < 0 || valor > 100) {
        return Result.Failure(DomainError.ValidationError("impuesto", "Debe ser entre 0 y 100"))
    }

    return Result.Success(valor)
}