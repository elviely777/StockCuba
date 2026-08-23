package cu.stockcuba.app.presentation.ajustes

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import cu.stockcuba.app.data.backup.BackupRepository
import cu.stockcuba.app.data.local.database.StockCubaDatabase
import cu.stockcuba.app.domain.feedback.FeedbackRepository
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.security.SecurityRepository
import cu.stockcuba.app.domain.validation.validarImpuesto
import cu.stockcuba.app.domain.validation.validarNombre
import cu.stockcuba.app.domain.validation.validarTelefono
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn

/**
 * Compose UI tests for AjustesScreen reset confirmation dialog (T29)
 * Tests TextField requiring typed "REINICIAR", positive button enabled only on match
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AjustesScreenResetDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Mock
    lateinit var ajustesDataStore: AjustesDataStore

    @Mock
    lateinit var backupRepository: BackupRepository

    @Mock
    lateinit var database: StockCubaDatabase

    @Mock
    lateinit var securityRepository: SecurityRepository

    @Mock
    lateinit var feedbackRepository: FeedbackRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

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
        doReturn(Result.Success(Unit)).when(ajustesDataStore).guardarTelefono(any())
        doReturn(Result.Success(Unit)).when(ajustesDataStore).guardarImpuesto(any())
        doReturn(Result.Success(Unit)).when(ajustesDataStore).guardarTema(any())
        doReturn(Result.Success(Unit)).when(ajustesDataStore).clearAll(any())
        doReturn(Result.Success(Unit)).when(database).clearAllTables()
        doReturn(kotlinx.coroutines.flow.flowOf(Result.Success(false))).when(securityRepository).hasPin()
    }

    @Test
    fun `reset dialog - shows TextField for typing REINICIAR`() = runBlockingTest {
        composeRule.setContent {
            AjustesScreen(
                onBack = {},
                viewModel = AjustesViewModel(ajustesDataStore, backupRepository, database, securityRepository, feedbackRepository, {})
            )
        }

        // Click reset button
        composeRule.onNodeWithText("Reiniciar todos los datos").performClick()

        // Dialog should show with TextField
        composeRule.onNodeWithText("REINICIAR").assertExists()
    }

    @Test
    fun `reset dialog - confirm button disabled until exact "REINICIAR" typed`() = runBlockingTest {
        composeRule.setContent {
            AjustesScreen(
                onBack = {},
                viewModel = AjustesViewModel(ajustesDataStore, backupRepository, database, securityRepository, feedbackRepository, {})
            )
        }

        composeRule.onNodeWithText("Reiniciar todos los datos").performClick()

        // Initially confirm button should be disabled
        composeRule.onNodeWithText("Confirmar").assertIsDisabled()

        // Type partial - still disabled
        composeRule.onNodeWithText("REINICIAR").performTextInput("REINIC")
        composeRule.onNodeWithText("Confirmar").assertIsDisabled()

        // Type exact match - enabled
        composeRule.onNodeWithText("REINICIAR").performTextInput("AR")
        composeRule.onNodeWithText("Confirmar").assertIsEnabled()
    }

    @Test
    fun `reset dialog - case sensitive, lowercase "reiniciar" not accepted`() = runBlockingTest {
        composeRule.setContent {
            AjustesScreen(
                onBack = {},
                viewModel = AjustesViewModel(ajustesDataStore, backupRepository, database, securityRepository, feedbackRepository, {})
            )
        }

        composeRule.onNodeWithText("Reiniciar todos los datos").performClick()
        composeRule.onNodeWithText("REINICIAR").performTextInput("reiniciar")
        composeRule.onNodeWithText("Confirmar").assertIsDisabled()
    }

    @Test
    fun `reset dialog - extra spaces not accepted`() = runBlockingTest {
        composeRule.setContent {
            AjustesScreen(
                onBack = {},
                viewModel = AjustesViewModel(ajustesDataStore, backupRepository, database, securityRepository, feedbackRepository, {})
            )
        }

        composeRule.onNodeWithText("Reiniciar todos los datos").performClick()
        composeRule.onNodeWithText("REINICIAR").performTextInput("REINICIAR ")
        composeRule.onNodeWithText("Confirmar").assertIsDisabled()
    }
}