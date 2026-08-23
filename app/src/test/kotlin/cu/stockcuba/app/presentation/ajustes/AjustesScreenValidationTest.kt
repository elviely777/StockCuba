package cu.stockcuba.app.presentation.ajustes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import cu.stockcuba.app.data.backup.BackupRepository
import cu.stockcuba.app.data.local.database.StockCubaDatabase
import cu.stockcuba.app.domain.feedback.FeedbackRepository
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.security.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class AjustesScreenValidationTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun `AjustesScreen muestra error de nombre cuando ViewModel tiene error`() {
        val ajustesDataStore = mock<AjustesDataStore>()
        val backupRepository = mock<BackupRepository>()
        val database = mock<StockCubaDatabase>()
        val securityRepository = mock<SecurityRepository>()
        val feedbackRepository = mock<FeedbackRepository>()
        
        val nombreFlow = MutableStateFlow("Mi Negocio")
        val direccionFlow = MutableStateFlow("")
        val telefonoFlow = MutableStateFlow("")
        val monedaFlow = MutableStateFlow(Moneda.CUP)
        val impuestoFlow = MutableStateFlow(0.0)
        val temaFlow = MutableStateFlow("SYSTEM")
        val seguridadFlow = MutableStateFlow(false)

        doReturn(nombreFlow).when(ajustesDataStore).nombreNegocio
        doReturn(direccionFlow).when(ajustesDataStore).direccion
        doReturn(telefonoFlow).when(ajustesDataStore).telefono
        doReturn(monedaFlow).when(ajustesDataStore).moneda
        doReturn(impuestoFlow).when(ajustesDataStore).impuesto
        doReturn(temaFlow).when(ajustesDataStore).tema
        doReturn(seguridadFlow).when(ajustesDataStore).seguridadBiometrica
        doReturn(Result.Success(Unit)).when(ajustesDataStore).guardarNombreNegocio(any())
        doReturn(flowOf(Result.Success(false))).when(securityRepository).hasPin()

        val viewModel = AjustesViewModel(ajustesDataStore, backupRepository, database, securityRepository, feedbackRepository, {})
        
        // Trigger validation error
        viewModel.guardarNombreNegocio("")

        composeRule.setContent {
            cu.stockcuba.app.presentation.theme.StockCubaTheme {
                AjustesScreen(onBack = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("El nombre es obligatorio").assertExists()
    }

    @Test
    fun `AjustesScreen limpia error cuando usuario corrige input`() {
        val ajustesDataStore = mock<AjustesDataStore>()
        val backupRepository = mock<BackupRepository>()
        val database = mock<StockCubaDatabase>()
        val securityRepository = mock<SecurityRepository>()
        val feedbackRepository = mock<FeedbackRepository>()
        
        val nombreFlow = MutableStateFlow("Mi Negocio")
        val direccionFlow = MutableStateFlow("")
        val telefonoFlow = MutableStateFlow("")
        val monedaFlow = MutableStateFlow(Moneda.CUP)
        val impuestoFlow = MutableStateFlow(0.0)
        val temaFlow = MutableStateFlow("SYSTEM")
        val seguridadFlow = MutableStateFlow(false)

        doReturn(nombreFlow).when(ajustesDataStore).nombreNegocio
        doReturn(direccionFlow).when(ajustesDataStore).direccion
        doReturn(telefonoFlow).when(ajustesDataStore).telefono
        doReturn(monedaFlow).when(ajustesDataStore).moneda
        doReturn(impuestoFlow).when(ajustesDataStore).impuesto
        doReturn(temaFlow).when(ajustesDataStore).tema
        doReturn(seguridadFlow).when(ajustesDataStore).seguridadBiometrica
        doReturn(Result.Success(Unit)).when(ajustesDataStore).guardarNombreNegocio(any())
        doReturn(flowOf(Result.Success(false))).when(securityRepository).hasPin()

        val viewModel = AjustesViewModel(ajustesDataStore, backupRepository, database, securityRepository, feedbackRepository, {})
        
        // Trigger validation error
        viewModel.guardarNombreNegocio("")

        composeRule.setContent {
            cu.stockcuba.app.presentation.theme.StockCubaTheme {
                AjustesScreen(onBack = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("El nombre es obligatorio").assertExists()

        // Correct the input
        viewModel.guardarNombreNegocio("Nombre Valido")

        composeRule.onNodeWithText("El nombre es obligatorio").assertDoesNotExist()
    }
}