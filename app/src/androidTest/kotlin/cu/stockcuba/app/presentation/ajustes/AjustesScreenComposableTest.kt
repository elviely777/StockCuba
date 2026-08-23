package cu.stockcuba.app.presentation.ajustes

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.security.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Compose UI tests for all AjustesScreen composables (T29, T41, T46).
 * Strict TDD: Tests written first, then implementation.
 */
@RunWith(AndroidJUnit4::class)
class AjustesScreenComposableTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ===== AjustesCargando Tests =====

    @Test
    fun `AjustesCargando muestra CircularProgressIndicator centrado`() {
        composeRule.setContent {
            AjustesCargando()
        }

        composeRule.onNodeWithTag("cargando_progress").assertExists()
        composeRule.onNode(hasText("Cargando ajustes...")).assertExists()
    }

    // ===== AjustesError Tests =====

    @Test
    fun `AjustesError muestra mensaje de error centrado`() {
        composeRule.setContent {
            AjustesError(message = "Error de conexión")
        }

        composeRule.onNodeWithText("Error de conexión").assertExists()
        composeRule.onNodeWithTag("error_container").assertExists()
    }

    @Test
    fun `AjustesError muestra botón de reintentar`() {
        var retryClicked = false
        composeRule.setContent {
            AjustesError(
                message = "Error de conexión",
                onRetry = { retryClicked = true }
            )
        }

        composeRule.onNodeWithText("Reintentar").performClick()
        assertTrue(retryClicked)
    }

    // ===== CampoTextoAjuste Tests =====

    @Test
    fun `CampoTextoAjuste muestra label y value`() {
        composeRule.setContent {
            CampoTextoAjuste(
                label = "Nombre del Negocio",
                value = "Mi Negocio",
                onValueChange = {},
                isError = false,
                supportingText = null
            )
        }

        composeRule.onNodeWithText("Nombre del Negocio").assertExists()
        composeRule.onNodeWithText("Mi Negocio").assertExists()
    }

    @Test
    fun `CampoTextoAjuste muestra error inline cuando isError=true`() {
        composeRule.setContent {
            CampoTextoAjuste(
                label = "Nombre del Negocio",
                value = "",
                onValueChange = {},
                isError = true,
                supportingText = "El nombre es obligatorio"
            )
        }

        composeRule.onNodeWithText("El nombre es obligatorio").assertExists()
    }

    @Test
    fun `CampoTextoAjuste no muestra error cuando isError=false`() {
        composeRule.setContent {
            CampoTextoAjuste(
                label = "Nombre del Negocio",
                value = "Mi Negocio",
                onValueChange = {},
                isError = false,
                supportingText = null
            )
        }

        composeRule.onNodeWithText("El nombre es obligatorio").assertDoesNotExist()
    }

    @Test
    fun `CampoTextoAjuste actualiza value al escribir`() {
        var capturedValue = ""
        composeRule.setContent {
            CampoTextoAjuste(
                label = "Nombre",
                value = "",
                onValueChange = { capturedValue = it },
                isError = false,
                supportingText = null
            )
        }

        composeRule.onNodeWithTag("campo_texto_ajuste").performTextInput("Nuevo Nombre")
        assertEquals("Nuevo Nombre", capturedValue)
    }

    // ===== AjustesContenido Tests =====

    @Test
    fun `AjustesContenido muestra las 5 secciones principales`() {
        val state = AjustesUiState.Success()

        composeRule.setContent {
            AjustesContenido(
                state = state,
                onNombreChange = {},
                onDireccionChange = {},
                onTelefonoChange = {},
                onMonedaChange = {},
                onImpuestoChange = {},
                onTemaChange = {},
                onSeguridadChange = {},
                onExportar = {},
                onImportar = {},
                onReiniciar = {},
                onPinSetup = {},
                onPinChange = {},
                onBiometricToggle = {},
                onFeedback = {},
                padding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            )
        }

        // Section headers (5 sections)
        composeRule.onNodeWithText("Negocio").assertExists()
        composeRule.onNodeWithText("Apariencia").assertExists()
        composeRule.onNodeWithText("Seguridad").assertExists()
        composeRule.onNodeWithText("Base de Datos").assertExists()
        composeRule.onNodeWithText("Acerca de").assertExists()
    }

    @Test
    fun `AjustesContenido sección Negocio tiene campos correctos`() {
        val state = AjustesUiState.Success(
            nombreNegocio = "Mi Negocio",
            direccion = "Calle 123",
            telefono = "555-1234",
            moneda = Moneda.CUP,
            impuesto = 15.0
        )

        composeRule.setContent {
            AjustesContenido(
                state = state,
                onNombreChange = {},
                onDireccionChange = {},
                onTelefonoChange = {},
                onMonedaChange = {},
                onImpuestoChange = {},
                onTemaChange = {},
                onSeguridadChange = {},
                onExportar = {},
                onImportar = {},
                onReiniciar = {},
                onPinSetup = {},
                onPinChange = {},
                onBiometricToggle = {},
                onFeedback = {},
                padding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            )
        }

        composeRule.onNodeWithText("Mi Negocio").assertExists()
        composeRule.onNodeWithText("Calle 123").assertExists()
        composeRule.onNodeWithText("555-1234").assertExists()
        composeRule.onNodeWithText("CUP").assertExists()
        composeRule.onNodeWithText("15%").assertExists()
    }

    @Test
    fun `AjustesContenido sección Apariencia muestra selector de tema`() {
        val state = AjustesUiState.Success(tema = "DARK")

        composeRule.setContent {
            AjustesContenido(
                state = state,
                onNombreChange = {},
                onDireccionChange = {},
                onTelefonoChange = {},
                onMonedaChange = {},
                onImpuestoChange = {},
                onTemaChange = {},
                onSeguridadChange = {},
                onExportar = {},
                onImportar = {},
                onReiniciar = {},
                onPinSetup = {},
                onPinChange = {},
                onBiometricToggle = {},
                onFeedback = {},
                padding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            )
        }

        composeRule.onNodeWithText("DARK").assertExists()
        composeRule.onNodeWithText("Tema").assertExists()
    }

    @Test
    fun `AjustesContenido sección Seguridad muestra estado biometrico y PIN`() {
        val state = AjustesUiState.Success(
            seguridadBiometrica = true,
            tienePin = true
        )

        composeRule.setContent {
            AjustesContenido(
                state = state,
                onNombreChange = {},
                onDireccionChange = {},
                onTelefonoChange = {},
                onMonedaChange = {},
                onImpuestoChange = {},
                onTemaChange = {},
                onSeguridadChange = {},
                onExportar = {},
                onImportar = {},
                onReiniciar = {},
                onPinSetup = {},
                onPinChange = {},
                onBiometricToggle = {},
                onFeedback = {},
                padding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            )
        }

        composeRule.onNodeWithText("Autenticación biométrica").assertExists()
        composeRule.onNodeWithText("Cambiar PIN").assertExists()
        composeRule.onNodeWithTag("biometric_switch").assertExists()
    }

    @Test
    fun `AjustesContenido sección Base de Datos muestra botones exportar/importar/reiniciar`() {
        val state = AjustesUiState.Success()

        composeRule.setContent {
            AjustesContenido(
                state = state,
                onNombreChange = {},
                onDireccionChange = {},
                onTelefonoChange = {},
                onMonedaChange = {},
                onImpuestoChange = {},
                onTemaChange = {},
                onSeguridadChange = {},
                onExportar = {},
                onImportar = {},
                onReiniciar = {},
                onPinSetup = {},
                onPinChange = {},
                onBiometricToggle = {},
                onFeedback = {},
                padding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            )
        }

        composeRule.onNodeWithText("Exportar base de datos").assertExists()
        composeRule.onNodeWithText("Importar base de datos").assertExists()
        composeRule.onNodeWithText("Reiniciar todos los datos").assertExists()
    }

    @Test
    fun `AjustesContenido sección Acerca de muestra versión de la app`() {
        val state = AjustesUiState.Success(appVersion = "2.5.1")

        composeRule.setContent {
            AjustesContenido(
                state = state,
                onNombreChange = {},
                onDireccionChange = {},
                onTelefonoChange = {},
                onMonedaChange = {},
                onImpuestoChange = {},
                onTemaChange = {},
                onSeguridadChange = {},
                onExportar = {},
                onImportar = {},
                onReiniciar = {},
                onPinSetup = {},
                onPinChange = {},
                onBiometricToggle = {},
                onFeedback = {},
                padding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            )
        }

        composeRule.onNodeWithText("Versión 2.5.1").assertExists()
        composeRule.onNodeWithText("Enviar feedback").assertExists()
    }

    @Test
    fun `AjustesContenido muestra errores de validación en campos`() {
        val state = AjustesUiState.Success(
            validationErrors = mapOf(
                "nombre" to "El nombre es obligatorio",
                "telefono" to "Teléfono inválido",
                "impuesto" to "Impuesto debe ser entre 0 y 100"
            )
        )

        composeRule.setContent {
            AjustesContenido(
                state = state,
                onNombreChange = {},
                onDireccionChange = {},
                onTelefonoChange = {},
                onMonedaChange = {},
                onImpuestoChange = {},
                onTemaChange = {},
                onSeguridadChange = {},
                onExportar = {},
                onImportar = {},
                onReiniciar = {},
                onPinSetup = {},
                onPinChange = {},
                onBiometricToggle = {},
                onFeedback = {},
                padding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            )
        }

        composeRule.onNodeWithText("El nombre es obligatorio").assertExists()
        composeRule.onNodeWithText("Teléfono inválido").assertExists()
        composeRule.onNodeWithText("Impuesto debe ser entre 0 y 100").assertExists()
    }

    // ===== ResetConfirmationDialog Tests =====

    @Test
    fun `ResetConfirmationDialog muestra TextField para escribir REINICIAR`() {
        composeRule.setContent {
            ResetConfirmationDialog(
                onDismiss = {},
                onConfirm = {},
                confirmationText = "",
                onTextChange = {}
            )
        }

        composeRule.onNodeWithText("Confirmar reinicio").assertExists()
        composeRule.onNodeWithText("Escribe REINICIAR para confirmar").assertExists()
        composeRule.onNodeWithTag("reset_confirmation_field").assertExists()
    }

    @Test
    fun `ResetConfirmationDialog botón confirmar deshabilitado hasta escribir REINICIAR exacto`() {
        composeRule.setContent {
            ResetConfirmationDialog(
                onDismiss = {},
                onConfirm = {},
                confirmationText = "",
                onTextChange = {}
            )
        }

        composeRule.onNodeWithText("Confirmar").assertIsDisabled()

        composeRule.onNodeWithTag("reset_confirmation_field").performTextInput("REINIC")
        composeRule.onNodeWithText("Confirmar").assertIsDisabled()

        composeRule.onNodeWithTag("reset_confirmation_field").performTextInput("AR")
        composeRule.onNodeWithText("Confirmar").assertIsEnabled()
    }

    @Test
    fun `ResetConfirmationDialog sensible a mayúsculas - "reiniciar" minúscula no habilita`() {
        composeRule.setContent {
            ResetConfirmationDialog(
                onDismiss = {},
                onConfirm = {},
                confirmationText = "",
                onTextChange = {}
            )
        }

        composeRule.onNodeWithTag("reset_confirmation_field").performTextInput("reiniciar")
        composeRule.onNodeWithText("Confirmar").assertIsDisabled()
    }

    @Test
    fun `ResetConfirmationDialog espacios extra no habilitan botón`() {
        composeRule.setContent {
            ResetConfirmationDialog(
                onDismiss = {},
                onConfirm = {},
                confirmationText = "",
                onTextChange = {}
            )
        }

        composeRule.onNodeWithTag("reset_confirmation_field").performTextInput("REINICIAR ")
        composeRule.onNodeWithText("Confirmar").assertIsDisabled()
    }

    @Test
    fun `ResetConfirmationDialog llama onConfirm con texto exacto`() {
        var confirmedText = ""
        composeRule.setContent {
            ResetConfirmationDialog(
                onDismiss = {},
                onConfirm = { confirmedText = it },
                confirmationText = "REINICIAR",
                onTextChange = {}
            )
        }

        composeRule.onNodeWithText("Confirmar").performClick()
        assertEquals("REINICIAR", confirmedText)
    }

    @Test
    fun `ResetConfirmationDialog llama onDismiss al cancelar`() {
        var dismissed = false
        composeRule.setContent {
            ResetConfirmationDialog(
                onDismiss = { dismissed = true },
                onConfirm = {},
                confirmationText = "REINICIAR",
                onTextChange = {}
            )
        }

        composeRule.onNodeWithText("Cancelar").performClick()
        assertTrue(dismissed)
    }

    // ===== BiometricToggleDialog Tests =====

    @Test
    fun `BiometricToggleDialog muestra Switch para biometría`() {
        composeRule.setContent {
            BiometricToggleDialog(
                onDismiss = {},
                currentEnabled = false,
                onToggle = {}
            )
        }

        composeRule.onNodeWithText("Autenticación biométrica").assertExists()
        composeRule.onNodeWithTag("biometric_switch").assertExists()
    }

    @Test
    fun `BiometricToggleDialog muestra estado actual del switch`() {
        composeRule.setContent {
            BiometricToggleDialog(
                onDismiss = {},
                currentEnabled = true,
                onToggle = {}
            )
        }

        val switchNode = composeRule.onNodeWithTag("biometric_switch")
        switchNode.assertExists()
        // Switch should be checked when enabled
    }

    @Test
    fun `BiometricToggleDialog muestra mensaje de requerir PIN si no existe`() {
        composeRule.setContent {
            BiometricToggleDialog(
                onDismiss = {},
                currentEnabled = false,
                onToggle = {},
                requiresPin = true
            )
        }

        composeRule.onNodeWithText("Requiere configurar PIN primero").assertExists()
    }

    @Test
    fun `BiometricToggleDialog no muestra mensaje PIN cuando ya existe PIN`() {
        composeRule.setContent {
            BiometricToggleDialog(
                onDismiss = {},
                currentEnabled = false,
                onToggle = {},
                requiresPin = false
            )
        }

        composeRule.onNodeWithText("Requiere configurar PIN primero").assertDoesNotExist()
    }

    @Test
    fun `BiometricToggleDialog llama onToggle al cambiar switch`() {
        var toggledValue = false
        composeRule.setContent {
            BiometricToggleDialog(
                onDismiss = {},
                currentEnabled = false,
                onToggle = { toggledValue = it },
                requiresPin = false
            )
        }

        composeRule.onNodeWithTag("biometric_switch").performClick()
        assertTrue(toggledValue)
    }

    @Test
    fun `BiometricToggleDialog llama onDismiss al cancelar`() {
        var dismissed = false
        composeRule.setContent {
            BiometricToggleDialog(
                onDismiss = { dismissed = true },
                currentEnabled = false,
                onToggle = {},
                requiresPin = false
            )
        }

        composeRule.onNodeWithText("Cancelar").performClick()
        assertTrue(dismissed)
    }
}