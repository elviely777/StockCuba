package cu.stockcuba.app.presentation.theme

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import cu.stockcuba.app.domain.model.ThemeMode
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn

class TemaProviderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    lateinit var ajustesDataStore: AjustesDataStore

    private lateinit var testViewModel: ThemeViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        val temaFlow = MutableStateFlow("SYSTEM")
        doReturn(temaFlow.asStateFlow()).when(ajustesDataStore).tema
        testViewModel = ThemeViewModel(ajustesDataStore)
    }

    @Test
    fun `TemaProvider provee ThemeMode desde ThemeViewModel`() {
        // Change the test ViewModel's flow
        val temaFlow = MutableStateFlow("LIGHT")
        doReturn(temaFlow.asStateFlow()).when(ajustesDataStore).tema
        testViewModel = ThemeViewModel(ajustesDataStore)

        composeRule.setContent {
            TemaProvider(themeViewModel = testViewModel) {
                TemaObserver()
            }
        }

        composeRule.onNodeWithText("Current: LIGHT").assertExists()

        // Change the flow value
        temaFlow.value = "DARK"

        composeRule.onNodeWithText("Current: DARK").assertExists()
    }

    @Test
    fun `TemaProvider usa SYSTEM como default cuando ViewModel emite valor invalido`() {
        val temaFlow = MutableStateFlow("INVALID")
        doReturn(temaFlow.asStateFlow()).when(ajustesDataStore).tema
        testViewModel = ThemeViewModel(ajustesDataStore)

        composeRule.setContent {
            TemaProvider(themeViewModel = testViewModel) {
                TemaObserver()
            }
        }

        composeRule.onNodeWithText("Current: SYSTEM").assertExists()
    }

    @Composable
    fun TemaObserver() {
        val tema = LocalTemaPreference.current
        androidx.compose.material3.Text(text = "Current: ${tema.name}")
    }
}