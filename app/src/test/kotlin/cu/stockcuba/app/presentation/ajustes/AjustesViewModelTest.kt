package cu.stockcuba.app.presentation.ajustes

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
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

@OptIn(ExperimentalCoroutinesApi::class)
class AjustesViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    lateinit var ajustesDataStore: AjustesDataStore

    @Mock
    lateinit var backupRepository: BackupRepository

    @Mock
    lateinit var feedbackRepository: FeedbackRepository

    @Mock
    lateinit var securityRepository: cu.stockcuba.app.domain.security.SecurityRepository

    @Mock
    lateinit var database: StockCubaDatabase

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
    fun `guardarNombreNegocio - nombre valido guarda y no tiene error`() = runBlockingTest {
        viewModel.guardarNombreNegocio("Nuevo Negocio")
        
        verify(ajustesDataStore).guardarNombreNegocio("Nuevo Negocio")
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertNull(state.nombreError)
        assertFalse(state.hasValidationErrors)
    }

    @Test
    fun `guardarNombreNegocio - nombre vacio no guarda y muestra error`() = runBlockingTest {
        viewModel.guardarNombreNegocio("")
        
        Mockito.verify(ajustesDataStore, Mockito.never()).guardarNombreNegocio(any())
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertEquals("El nombre es obligatorio", state.nombreError)
        assertTrue(state.hasValidationErrors)
    }

    @Test
    fun `guardarNombreNegocio - nombre muy largo no guarda y muestra error`() = runBlockingTest {
        val nombreLargo = "a".repeat(101)
        viewModel.guardarNombreNegocio(nombreLargo)
        
        Mockito.verify(ajustesDataStore, Mockito.never()).guardarNombreNegocio(any())
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertEquals("Máximo 100 caracteres", state.nombreError)
        assertTrue(state.hasValidationErrors)
    }

    @Test
    fun `guardarTelefono - telefono valido guarda y no tiene error`() = runBlockingTest {
        viewModel.guardarTelefono("+5351234567")
        
        verify(ajustesDataStore).guardarTelefono("+5351234567")
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertNull(state.telefonoError)
        assertFalse(state.hasValidationErrors)
    }

    @Test
    fun `guardarTelefono - telefono invalido no guarda y muestra error`() = runBlockingTest {
        viewModel.guardarTelefono("12345")
        
        Mockito.verify(ajustesDataStore, Mockito.never()).guardarTelefono(any())
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertEquals("Formato: +53 5 XXX XXXX", state.telefonoError)
        assertTrue(state.hasValidationErrors)
    }

    @Test
    fun `guardarImpuesto - impuesto valido guarda y no tiene error`() = runBlockingTest {
        viewModel.guardarImpuesto("15.50")
        
        verify(ajustesDataStore).guardarImpuesto(15.50)
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertNull(state.impuestoError)
        assertFalse(state.hasValidationErrors)
    }

    @Test
    fun `guardarImpuesto - impuesto mayor a 100 no guarda y muestra error`() = runBlockingTest {
        viewModel.guardarImpuesto("150")
        
        Mockito.verify(ajustesDataStore, Mockito.never()).guardarImpuesto(any())
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertEquals("Debe ser entre 0 y 100", state.impuestoError)
        assertTrue(state.hasValidationErrors)
    }

    @Test
    fun `guardarImpuesto - impuesto con 3 decimales no guarda y muestra error`() = runBlockingTest {
        viewModel.guardarImpuesto("15.123")
        
        Mockito.verify(ajustesDataStore, Mockito.never()).guardarImpuesto(any())
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertEquals("Máximo 2 decimales", state.impuestoError)
        assertTrue(state.hasValidationErrors)
    }

    @Test
    fun `guardarImpuesto - impuesto negativo no guarda y muestra error`() = runBlockingTest {
        viewModel.guardarImpuesto("-5")
        
        Mockito.verify(ajustesDataStore, Mockito.never()).guardarImpuesto(any())
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertEquals("Debe ser entre 0 y 100", state.impuestoError)
        assertTrue(state.hasValidationErrors)
    }

    @Test
    fun `guardarImpuesto - texto no numerico no guarda y muestra error`() = runBlockingTest {
        viewModel.guardarImpuesto("abc")
        
        Mockito.verify(ajustesDataStore, Mockito.never()).guardarImpuesto(any())
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertNotNull(state.impuestoError)
        assertTrue(state.hasValidationErrors)
    }

    @Test
    fun `validationErrors se limpia cuando se corrige el error`() = runBlockingTest {
        // Primero causar error
        viewModel.guardarNombreNegocio("")
        var state = viewModel.uiState.value as AjustesUiState.Success
        assertNotNull(state.nombreError)
        
        // Luego corregir
        viewModel.guardarNombreNegocio("Nombre Valido")
        state = viewModel.uiState.value as AjustesUiState.Success
        assertNull(state.nombreError)
        assertFalse(state.hasValidationErrors)
    }

    @Test
    fun `multiples errores coexisten`() = runBlockingTest {
        viewModel.guardarNombreNegocio("")
        viewModel.guardarTelefono("123")
        viewModel.guardarImpuesto("150")
        
        val state = viewModel.uiState.value as AjustesUiState.Success
        assertNotNull(state.nombreError)
        assertNotNull(state.telefonoError)
        assertNotNull(state.impuestoError)
        assertEquals(3, state.validationErrors.size)
    }

    // ===== BACKUP EXPORT/IMPORT TESTS =====

    @Test
    fun `exportarBaseDatos - delega a BackupRepository y retorna Uri`() = runBlockingTest {
        // Given
        val expectedUri = mock<Uri>()
        doReturn(Result.Success(expectedUri)).when(backupRepository).exportDatabase()

        // When
        val result = viewModel.exportarBaseDatos()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedUri, (result as Result.Success<Uri>).value)
        verify(backupRepository).exportDatabase()
    }

    @Test
    fun `exportarBaseDatos - propaga error si BackupRepository falla`() = runBlockingTest {
        // Given
        val error = cu.stockcuba.app.domain.model.DomainError.DatabaseError(java.io.IOException("Export failed"))
        doReturn(Result.Failure(error)).when(backupRepository).exportDatabase()

        // When
        val result = viewModel.exportarBaseDatos()

        // Then
        assertTrue(result is Result.Failure)
        assertEquals(error, (result as Result.Failure).error)
        verify(backupRepository).exportDatabase()
    }

    @Test
    fun `importarBaseDatos - delega a BackupRepository con Uri`() = runBlockingTest {
        // Given
        val inputUri = mock<Uri>()
        doReturn(Result.Success(Unit)).when(backupRepository).importDatabase(inputUri)

        // When
        val result = viewModel.importarBaseDatos(inputUri)

        // Then
        assertTrue(result is Result.Success)
        verify(backupRepository).importDatabase(inputUri)
    }

    @Test
    fun `importarBaseDatos - propaga error si BackupRepository falla`() = runBlockingTest {
        // Given
        val inputUri = mock<Uri>()
        val error = cu.stockcuba.app.domain.model.DomainError.DatabaseError(java.io.IOException("Import failed"))
        doReturn(Result.Failure(error)).when(backupRepository).importDatabase(inputUri)

        // When
        val result = viewModel.importarBaseDatos(inputUri)

        // Then
        assertTrue(result is Result.Failure)
        assertEquals(error, (result as Result.Failure).error)
        verify(backupRepository).importDatabase(inputUri)
    }

    // ===== FEEDBACK TESTS (T46) =====

    @Test
    fun `sendFeedback - delega a FeedbackRepository y retorna Success`() = runBlockingTest {
        // Given
        doReturn(Result.Success(Unit)).when(feedbackRepository).sendFeedback()

        // When
        val result = viewModel.sendFeedback()

        // Then
        assertTrue(result is Result.Success)
        verify(feedbackRepository).sendFeedback()
    }

    @Test
    fun `sendFeedback - propaga error si FeedbackRepository falla`() = runBlockingTest {
        // Given
        val error = cu.stockcuba.app.domain.model.DomainError.NotFound("email app", "No email app")
        doReturn(Result.Failure(error)).when(feedbackRepository).sendFeedback()

        // When
        val result = viewModel.sendFeedback()

        // Then
        assertTrue(result is Result.Failure)
        assertEquals(error, (result as Result.Failure).error)
        verify(feedbackRepository).sendFeedback()
    }

    // ===== RESET TESTS =====

    @Test
    fun `reiniciarDatos - with exact "REINICIAR" confirmation clears DataStore and DB and triggers event`() = runBlockingTest {
        // When
        viewModel.reiniciarDatos("REINICIAR")

        // Then: clearAll called with preserved keys
        verify(ajustesDataStore).clearAll(any())
        
        // And: clearAllTables called on database
        verify(database).clearAllTables()
        
        // And: onResetComplete called
        verify(onResetComplete).invoke()
    }

    @Test
    fun `reiniciarDatos - with wrong confirmation does nothing and does not trigger event`() = runBlockingTest {
        // When
        viewModel.reiniciarDatos("REINICIAR ") // extra space
        viewModel.reiniciarDatos("reiniciar") // lowercase
        viewModel.reiniciarDatos("") // empty

        // Then: clearAll NOT called
        Mockito.verify(ajustesDataStore, Mockito.never()).clearAll(any())
        Mockito.verify(database, Mockito.never()).clearAllTables()
        
        // And: onResetComplete NOT called
        Mockito.verify(onResetComplete, Mockito.never()).invoke()
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
        viewModel.reiniciarDatos("REINICIAR")
        verify(onResetComplete).invoke()
    }
}