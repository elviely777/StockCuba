package cu.stockcuba.app.presentation.ajustes

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.rememberNavController
import cu.stockcuba.app.data.backup.BackupRepository
import cu.stockcuba.app.data.local.database.StockCubaDatabase
import cu.stockcuba.app.domain.feedback.FeedbackRepository
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.security.SecurityRepository
import cu.stockcuba.app.domain.validation.validarImpuesto
import cu.stockcuba.app.domain.validation.validarNombre
import cu.stockcuba.app.domain.validation.validarTelefono
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify

/**
 * Unit tests for AjustesViewModel.reiniciarDatos (T28, T31)
 * Tests reset clears DB + DataStore (except preserved keys), nav action triggered
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AjustesViewModelResetTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    lateinit var ajustesDataStore: AjustesDataStore

    @Mock
    lateinit var backupRepository: BackupRepository

    @Mock
    lateinit var database: StockCubaDatabase

    @Mock
    lateinit var navController: NavController

    @Mock
    lateinit var securityRepository: SecurityRepository

    @Mock
    lateinit var feedbackRepository: FeedbackRepository

    @Mock
    lateinit var onResetComplete: () -> Unit

    lateinit var viewModel: AjustesViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Setup default flows
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

        // Default success for save operations
        doReturn(Result.Success(Unit)).when(ajustesDataStore).guardarNombreNegocio(any())
        doReturn(Result.Success(Unit)).when(ajustesDataStore).guardarTelefono(any())
        doReturn(Result.Success(Unit)).when(ajustesDataStore).guardarImpuesto(any())
        doReturn(Result.Success(Unit)).when(ajustesDataStore).guardarTema(any())
        doReturn(Result.Success(Unit)).when(ajustesDataStore).clearAll(any())

        doReturn(Result.Success(Unit)).when(database).clearAllTables()

        // Default for securityRepository.hasPin()
        doReturn(flowOf(Result.Success(false))).when(securityRepository).hasPin()

        viewModel = AjustesViewModel(ajustesDataStore, backupRepository, database, securityRepository, feedbackRepository)
        viewModel.onResetComplete = onResetComplete
    }

    @Test
    fun `reiniciarDatos - with exact "REINICIAR" confirmation clears DataStore and DB`() = runBlockingTest {
        // When
        viewModel.reiniciarDatos("REINICIAR")

        // Then: clearAll called with preserved keys
        verify(ajustesDataStore).clearAll(any())
        
        // And: clearAllTables called on database
        verify(database).clearAllTables()
    }

    @Test
    fun `reiniciarDatos - with wrong confirmation does nothing`() = runBlockingTest {
        // When
        viewModel.reiniciarDatos("REINICIAR ") // extra space
        viewModel.reiniciarDatos("reiniciar") // lowercase
        viewModel.reiniciarDatos("") // empty

        // Then: clearAll NOT called
        Mockito.verify(ajustesDataStore, Mockito.never()).clearAll(any())
        Mockito.verify(database, Mockito.never()).clearAllTables()
    }

    @Test
    fun `reiniciarDatos - clears all DataStore keys except TEMA_KEY, PIN_HASH_KEY, PIN_SALT_KEY, BIOMETRIC_ENABLED_KEY`() = runBlockingTest {
        // When
        viewModel.reiniciarDatos("REINICIAR")

        // Then: Verify preserved keys passed to clearAll
        val preservedKeysCaptor = org.mockito.kotlin.argumentCaptor<Set<AjustesDataStore.Preferences.Key<*>>>()
        verify(ajustesDataStore).clearAll(preservedKeysCaptor.capture())
        
        val preservedKeys = preservedKeysCaptor.firstValue
        assertTrue(preservedKeys.contains(AjustesDataStore.TEMA_KEY))
        assertTrue(preservedKeys.contains(AjustesDataStore.PIN_HASH_KEY))
        assertTrue(preservedKeys.contains(AjustesDataStore.PIN_SALT_KEY))
        assertTrue(preservedKeys.contains(AjustesDataStore.BIOMETRIC_ENABLED_KEY))
    }

    @Test
    fun `reiniciarDatos - triggers navigation event to Dashboard`() = runBlockingTest {
        // When
        viewModel.reiniciarDatos("REINICIAR")

        // Then: onResetComplete callback called
        verify(onResetComplete).invoke()
    }

    @Test
    fun `reiniciarDatos - with wrong confirmation does not trigger event`() = runBlockingTest {
        // When
        viewModel.reiniciarDatos("REINICIAR ")
        viewModel.reiniciarDatos("reiniciar")
        viewModel.reiniciarDatos("")

        // Then: onResetComplete NOT called
        Mockito.verify(onResetComplete, Mockito.never()).invoke()
    }
}