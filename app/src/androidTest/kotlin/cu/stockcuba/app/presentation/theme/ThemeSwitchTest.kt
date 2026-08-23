package cu.stockcuba.app.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.performClick
import cu.stockcuba.app.domain.model.ThemeMode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ThemeSwitchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<cu.stockcuba.app.MainActivity>()

    @Test
    fun `ThemeMode enum values existen`() {
        assertEquals(3, ThemeMode.entries.size)
        assertTrue(ThemeMode.entries.contains(ThemeMode.SYSTEM))
        assertTrue(ThemeMode.entries.contains(ThemeMode.LIGHT))
        assertTrue(ThemeMode.entries.contains(ThemeMode.DARK))
    }

    @Test
    fun `LocalTemaPreference default es SYSTEM`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalTemaPreference provides ThemeMode.SYSTEM) {
                ThemeModeDisplay()
            }
        }
        composeRule.onNodeWithText("SYSTEM").assertExists()
    }

    @Test
    fun `StockCubaTheme aplica tema LIGHT`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalTemaPreference provides ThemeMode.LIGHT) {
                StockCubaTheme {
                    ThemeModeDisplay()
                }
            }
        }
        composeRule.onNodeWithText("LIGHT").assertExists()
    }

    @Test
    fun `StockCubaTheme aplica tema DARK`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalTemaPreference provides ThemeMode.DARK) {
                StockCubaTheme {
                    ThemeModeDisplay()
                }
            }
        }
        composeRule.onNodeWithText("DARK").assertExists()
    }

    @Composable
    fun ThemeModeDisplay() {
        val tema = LocalTemaPreference.current
        androidx.compose.material3.Text(text = tema.name)
    }
}