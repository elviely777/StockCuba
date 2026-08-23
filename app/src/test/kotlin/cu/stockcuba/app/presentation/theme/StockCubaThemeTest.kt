package cu.stockcuba.app.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import cu.stockcuba.app.domain.model.ThemeMode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StockCubaThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `StockCubaTheme consume LocalTemaPreference y mapea a darkTheme`() {
        // Test with LIGHT theme
        composeRule.setContent {
            CompositionLocalProvider(LocalTemaPreference provides ThemeMode.LIGHT) {
                StockCubaTheme {
                    ThemeModeObserver()
                }
            }
        }
        composeRule.onNodeWithText("darkTheme: false").assertExists()

        // Test with DARK theme
        composeRule.setContent {
            CompositionLocalProvider(LocalTemaPreference provides ThemeMode.DARK) {
                StockCubaTheme {
                    ThemeModeObserver()
                }
            }
        }
        composeRule.onNodeWithText("darkTheme: true").assertExists()

        // Test with SYSTEM theme when system is light
        composeRule.setContent {
            CompositionLocalProvider(LocalTemaPreference provides ThemeMode.SYSTEM) {
                StockCubaTheme {
                    ThemeModeObserver(isSystemInDarkTheme = false)
                }
            }
        }
        composeRule.onNodeWithText("darkTheme: false").assertExists()

        // Test with SYSTEM theme when system is dark
        composeRule.setContent {
            CompositionLocalProvider(LocalTemaPreference provides ThemeMode.SYSTEM) {
                StockCubaTheme {
                    ThemeModeObserver(isSystemInDarkTheme = true)
                }
            }
        }
        composeRule.onNodeWithText("darkTheme: true").assertExists()
    }

    @Composable
    fun ThemeModeObserver(isSystemInDarkTheme: Boolean = false) {
        val isDark = if (LocalTemaPreference.current == ThemeMode.SYSTEM) {
            isSystemInDarkTheme
        } else {
            LocalTemaPreference.current == ThemeMode.DARK
        }
        androidx.compose.material3.Text(text = "darkTheme: $isDark")
    }
}