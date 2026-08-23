package cu.stockcuba.app.domain.model

/**
 * Modo de tema de la aplicación.
 * - SYSTEM: Sigue la configuración del sistema operativo
 * - LIGHT: Fuerza modo claro
 * - DARK: Fuerza modo oscuro
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    /**
     * Convierte un string a ThemeMode.
     * Es case-insensitive y devuelve SYSTEM como fallback.
     */
    companion object {
        fun fromString(value: String): ThemeMode {
            return try {
                ThemeMode.valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                SYSTEM
            }
        }
    }

    /**
     * Convierte el ThemeMode a boolean para darkTheme.
     * @param isSystemInDarkTheme Si el sistema está en modo oscuro (usado cuando ThemeMode.SYSTEM)
     * @return true si debe usar tema oscuro
     */
    fun toDarkThemeBoolean(isSystemInDarkTheme: Boolean): Boolean {
        return when (this) {
            SYSTEM -> isSystemInDarkTheme
            LIGHT -> false
            DARK -> true
        }
    }
}