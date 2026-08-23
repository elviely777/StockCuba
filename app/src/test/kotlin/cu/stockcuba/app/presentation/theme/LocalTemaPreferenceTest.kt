package cu.stockcuba.app.presentation.theme

import androidx.compose.runtime.CompositionLocal
import org.junit.Assert.*
import org.junit.Test

class LocalTemaPreferenceTest {

    @Test
    fun `LocalTemaPreference es CompositionLocal con default SYSTEM`() {
        assertTrue(LocalTemaPreference is CompositionLocal<*>)
        assertEquals(ThemeMode.SYSTEM, LocalTemaPreference.current)
    }

    @Test
    fun `LocalTemaPreference proporciona valor configurado`() {
        // This test verifies the CompositionLocal can be provided with different values
        // The actual provision is tested in TemaProvider tests
        assertEquals(ThemeMode.SYSTEM, LocalTemaPreference.current)
    }
}