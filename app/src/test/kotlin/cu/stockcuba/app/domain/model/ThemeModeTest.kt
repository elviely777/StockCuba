package cu.stockcuba.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `ThemeMode tiene valores SYSTEM LIGHT DARK`() {
        assertEquals(3, ThemeMode.entries.size)
        assertTrue(ThemeMode.entries.contains(ThemeMode.SYSTEM))
        assertTrue(ThemeMode.entries.contains(ThemeMode.LIGHT))
        assertTrue(ThemeMode.entries.contains(ThemeMode.DARK))
    }

    @Test
    fun `ThemeMode fromString mapea correctamente`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"))
        // Case insensitive
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("system"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("dark"))
        // Default fallback
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("INVALID"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(""))
    }

    @Test
    fun `ThemeMode toString devuelve nombre en mayusculas`() {
        assertEquals("SYSTEM", ThemeMode.SYSTEM.toString())
        assertEquals("LIGHT", ThemeMode.LIGHT.toString())
        assertEquals("DARK", ThemeMode.DARK.toString())
    }

    @Test
    fun `ThemeMode toDarkThemeBoolean mapea segun sistema`() {
        // SYSTEM -> depends on system
        assertEquals(false, ThemeMode.SYSTEM.toDarkThemeBoolean(false)) // system light
        assertEquals(true, ThemeMode.SYSTEM.toDarkThemeBoolean(true))   // system dark
        // LIGHT -> always false
        assertEquals(false, ThemeMode.LIGHT.toDarkThemeBoolean(false))
        assertEquals(false, ThemeMode.LIGHT.toDarkThemeBoolean(true))
        // DARK -> always true
        assertEquals(true, ThemeMode.DARK.toDarkThemeBoolean(false))
        assertEquals(true, ThemeMode.DARK.toDarkThemeBoolean(true))
    }
}