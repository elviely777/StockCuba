package cu.stockcuba.app.presentation.ajustes

import org.junit.Assert.*
import org.junit.Test

class AjustesUiStateTest {

    @Test
    fun `AjustesUiState.Success tiene validationErrors map vacio por defecto`() {
        val state = AjustesUiState.Success()
        assertTrue(state.validationErrors.isEmpty())
    }

    @Test
    fun `AjustesUiState.Success helper getters devuelven null cuando no hay error`() {
        val state = AjustesUiState.Success()
        assertNull(state.nombreError)
        assertNull(state.telefonoError)
        assertNull(state.impuestoError)
    }

    @Test
    fun `AjustesUiState.Success helper getters devuelven error cuando existe`() {
        val state = AjustesUiState.Success(
            validationErrors = mapOf(
                "nombre" to "El nombre es obligatorio",
                "telefono" to "Formato: +53 5 XXX XXXX",
                "impuesto" to "Debe ser entre 0 y 100"
            )
        )
        assertEquals("El nombre es obligatorio", state.nombreError)
        assertEquals("Formato: +53 5 XXX XXXX", state.telefonoError)
        assertEquals("Debe ser entre 0 y 100", state.impuestoError)
    }

    @Test
    fun `AjustesUiState.Success copy mantiene validationErrors`() {
        val original = AjustesUiState.Success(
            validationErrors = mapOf("nombre" to "Error")
        )
        val copied = original.copy(nombreNegocio = "Nuevo")
        assertEquals("Error", copied.nombreError)
    }
}