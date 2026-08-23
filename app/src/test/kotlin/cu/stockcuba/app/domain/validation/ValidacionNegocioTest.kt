package cu.stockcuba.app.domain.validation

import cu.stockcuba.app.domain.model.Result
import org.junit.Assert.*
import org.junit.Test

class ValidacionNegocioTest {

    @Test
    fun `validarNombre - nombre valido (1-100 chars) retorna Success`() {
        val result = validarNombre("Mi Negocio")
        assertTrue(result is Result.Success)
        assertEquals("Mi Negocio", (result as Result.Success).value)
    }

    @Test
    fun `validarNombre - nombre vacio retorna Failure`() {
        val result = validarNombre("")
        assertTrue(result is Result.Failure)
        assertEquals("El nombre es obligatorio", (result as Result.Failure).error.message)
    }

    @Test
    fun `validarNombre - nombre solo espacios retorna Failure`() {
        val result = validarNombre("   ")
        assertTrue(result is Result.Failure)
        assertEquals("El nombre es obligatorio", (result as Result.Failure).error.message)
    }

    @Test
    fun `validarNombre - nombre 101 caracteres retorna Failure`() {
        val nombreLargo = "a".repeat(101)
        val result = validarNombre(nombreLargo)
        assertTrue(result is Result.Failure)
        assertEquals("Máximo 100 caracteres", (result as Result.Failure).error.message)
    }

    @Test
    fun `validarNombre - nombre 100 caracteres es valido`() {
        val nombreExacto = "a".repeat(100)
        val result = validarNombre(nombreExacto)
        assertTrue(result is Result.Success)
    }

    @Test
    fun `validarTelefono - formato +53 5XXXXXXX valido`() {
        val result = validarTelefono("+5351234567")
        assertTrue(result is Result.Success)
        assertEquals("+5351234567", (result as Result.Success).value)
    }

    @Test
    fun `validarTelefono - formato 53 5XXXXXXX valido`() {
        val result = validarTelefono("5351234567")
        assertTrue(result is Result.Success)
        assertEquals("5351234567", (result as Result.Success).value)
    }

    @Test
    fun `validarTelefono - formato 5XXXXXXX valido (8 digitos)`() {
        val result = validarTelefono("51234567")
        assertTrue(result is Result.Success)
        assertEquals("51234567", (result as Result.Success).value)
    }

    @Test
    fun `validarTelefono - formato invalido retorna Failure`() {
        val result = validarTelefono("12345")
        assertTrue(result is Result.Failure)
        assertEquals("Formato: +53 5 XXX XXXX", (result as Result.Failure).error.message)
    }

    @Test
    fun `validarTelefono - vacio retorna Failure`() {
        val result = validarTelefono("")
        assertTrue(result is Result.Failure)
        assertEquals("Formato: +53 5 XXX XXXX", (result as Result.Failure).error.message)
    }

    @Test
    fun `validarImpuesto - valor valido 0-100 con 2 decimales`() {
        val result = validarImpuesto("15.50")
        assertTrue(result is Result.Success)
        assertEquals(15.50, (result as Result.Success).value, 0.001)
    }

    @Test
    fun `validarImpuesto - valor entero valido`() {
        val result = validarImpuesto("15")
        assertTrue(result is Result.Success)
        assertEquals(15.0, (result as Result.Success).value, 0.001)
    }

    @Test
    fun `validarImpuesto - valor 0 valido`() {
        val result = validarImpuesto("0")
        assertTrue(result is Result.Success)
        assertEquals(0.0, (result as Result.Success).value, 0.001)
    }

    @Test
    fun `validarImpuesto - valor 100 valido`() {
        val result = validarImpuesto("100")
        assertTrue(result is Result.Success)
        assertEquals(100.0, (result as Result.Success).value, 0.001)
    }

    @Test
    fun `validarImpuesto - mayor a 100 retorna Failure`() {
        val result = validarImpuesto("150")
        assertTrue(result is Result.Failure)
        assertEquals("Debe ser entre 0 y 100", (result as Result.Failure).error.message)
    }

    @Test
    fun `validarImpuesto - negativo retorna Failure`() {
        val result = validarImpuesto("-5")
        assertTrue(result is Result.Failure)
        assertEquals("Debe ser entre 0 y 100", (result as Result.Failure).error.message)
    }

    @Test
    fun `validarImpuesto - 3 decimales retorna Failure`() {
        val result = validarImpuesto("15.123")
        assertTrue(result is Result.Failure)
        assertEquals("Máximo 2 decimales", (result as Result.Failure).error.message)
    }

    @Test
    fun `validarImpuesto - no numerico retorna Failure`() {
        val result = validarImpuesto("abc")
        assertTrue(result is Result.Failure)
    }
}