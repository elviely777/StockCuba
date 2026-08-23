package cu.stockcuba.app.presentation.theme

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import cu.stockcuba.app.domain.model.ThemeMode
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class ThemeViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    lateinit var ajustesDataStore: AjustesDataStore

    lateinit var viewModel: ThemeViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        val temaFlow = MutableStateFlow("SYSTEM")
        doReturn(temaFlow.asStateFlow()).when(ajustesDataStore).tema

        viewModel = ThemeViewModel(ajustesDataStore)
    }

    @Test
    fun `ThemeViewModel expone themeMode flow que mapea string a ThemeMode enum`() = runBlockingTest {
        // Collect the themeMode flow
        val collected = mutableListOf<ThemeMode>()
        val job = kotlinx.coroutines.launch {
            viewModel.themeMode.collect { collected.add(it) }
        }

        // Wait for initial emission
        kotlinx.coroutines.delay(10)
        
        // Should emit SYSTEM initially
        assertEquals(ThemeMode.SYSTEM, collected.first())

        // Change to DARK via the mocked flow
        val newTemaFlow = MutableStateFlow("DARK")
        doReturn(newTemaFlow.asStateFlow()).when(ajustesDataStore).tema
        
        // Note: In real implementation, ThemeViewModel would observe the DataStore flow directly
        // This test verifies the initial mapping logic
        job.cancel()
    }

    @Test
    fun `ThemeViewModel mapea LIGHT string a ThemeMode.LIGHT`() = runBlockingTest {
        val temaFlow = MutableStateFlow("LIGHT")
        doReturn(temaFlow.asStateFlow()).when(ajustesDataStore).tema

        val vm = ThemeViewModel(ajustesDataStore)
        val collected = mutableListOf<ThemeMode>()
        val job = kotlinx.coroutines.launch {
            vm.themeMode.collect { collected.add(it) }
        }

        kotlinx.coroutines.delay(10)
        
        assertEquals(ThemeMode.LIGHT, collected.first())
        job.cancel()
    }

    @Test
    fun `ThemeViewModel mapea DARK string a ThemeMode.DARK`() = runBlockingTest {
        val temaFlow = MutableStateFlow("DARK")
        doReturn(temaFlow.asStateFlow()).when(ajustesDataStore).tema

        val vm = ThemeViewModel(ajustesDataStore)
        val collected = mutableListOf<ThemeMode>()
        val job = kotlinx.coroutines.launch {
            vm.themeMode.collect { collected.add(it) }
        }

        kotlinx.coroutines.delay(10)
        
        assertEquals(ThemeMode.DARK, collected.first())
        job.cancel()
    }

    @Test
    fun `ThemeViewModel usa SYSTEM como default cuando DataStore emite valor invalido`() = runBlockingTest {
        val temaFlow = MutableStateFlow("INVALID")
        doReturn(temaFlow.asStateFlow()).when(ajustesDataStore).tema

        val vm = ThemeViewModel(ajustesDataStore)
        val collected = mutableListOf<ThemeMode>()
        val job = kotlinx.coroutines.launch {
            vm.themeMode.collect { collected.add(it) }
        }

        kotlinx.coroutines.delay(10)
        
        // Should use SYSTEM as fallback
        assertEquals(ThemeMode.SYSTEM, collected.first())
        job.cancel()
    }

    @Test
    fun `ThemeViewModel reacciona a cambios en DataStore`() = runBlockingTest {
        val temaFlow = MutableStateFlow("LIGHT")
        doReturn(temaFlow.asStateFlow()).when(ajustesDataStore).tema

        val vm = ThemeViewModel(ajustesDataStore)
        val collected = mutableListOf<ThemeMode>()
        val job = kotlinx.coroutines.launch {
            vm.themeMode.collect { collected.add(it) }
        }

        kotlinx.coroutines.delay(10)
        assertEquals(ThemeMode.LIGHT, collected.last())

        // Change DataStore flow
        temaFlow.value = "DARK"
        kotlinx.coroutines.delay(10)
        
        assertEquals(ThemeMode.DARK, collected.last())
        job.cancel()
    }
}