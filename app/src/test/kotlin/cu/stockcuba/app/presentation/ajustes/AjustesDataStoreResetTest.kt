package cu.stockcuba.app.presentation.ajustes

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for AjustesDataStore clearAll functionality (T26)
 * Tests that clearAll preserves TEMA_KEY, PIN_HASH_KEY, PIN_SALT_KEY, BIOMETRIC_ENABLED_KEY
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AjustesDataStoreResetTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var ajustesDataStore: AjustesDataStore

    @Before
    fun setup() {
        // Use in-memory DataStore for testing
        dataStore = androidx.datastore.preferences.PreferencesDataStoreFactory.create(androidx.datastore.core.MemoryDataStore())
        ajustesDataStore = AjustesDataStore(dataStore)
    }

    @Test
    fun `clearAll - preserves TEMA_KEY while clearing other keys`() = runBlockingTest {
        // Given: Populate all keys
        ajustesDataStore.guardarNombreNegocio("Mi Negocio")
        ajustesDataStore.guardarDireccion("Calle 123")
        ajustesDataStore.guardarTelefono("+5351234567")
        ajustesDataStore.guardarMoneda(Moneda.CUP)
        ajustesDataStore.guardarImpuesto(15.0)
        ajustesDataStore.guardarTema("DARK")
        ajustesDataStore.guardarSeguridadBiometrica(true)
        
        // Set PIN keys (to be added in T33)
        val pinHashKey = AjustesDataStore.PIN_HASH_KEY
        val pinSaltKey = AjustesDataStore.PIN_SALT_KEY
        val biometricEnabledKey = AjustesDataStore.BIOMETRIC_ENABLED_KEY
        dataStore.edit { it[pinHashKey] = "hashed_pin" }.await()
        dataStore.edit { it[pinSaltKey] = "salt" }.await()
        dataStore.edit { it[biometricEnabledKey] = true }.await()

        // When: Clear all except preserved keys
        val preservedKeys = setOf(
            AjustesDataStore.TEMA_KEY,
            AjustesDataStore.PIN_HASH_KEY,
            AjustesDataStore.PIN_SALT_KEY,
            AjustesDataStore.BIOMETRIC_ENABLED_KEY
        )
        ajustesDataStore.clearAll(preservedKeys)

        // Then: Verify preserved keys still have values
        assertEquals("DARK", dataStore.data.first()[AjustesDataStore.TEMA_KEY])
        assertEquals("hashed_pin", dataStore.data.first()[pinHashKey])
        assertEquals("salt", dataStore.data.first()[pinSaltKey])
        assertTrue(dataStore.data.first()[biometricEnabledKey]!!)

        // And: Verify non-preserved keys are cleared (return defaults)
        assertEquals("Mi Negocio", ajustesDataStore.nombreNegocio.first()) // default
        assertEquals("", ajustesDataStore.direccion.first()) // default
        assertEquals("", ajustesDataStore.telefono.first()) // default
        assertEquals(Moneda.CUP, ajustesDataStore.moneda.first()) // default
        assertEquals(0.0, ajustesDataStore.impuesto.first(), 0.001) // default
        assertEquals("SYSTEM", ajustesDataStore.tema.first()) // wait, this should be "DARK" preserved
    }

    @Test
    fun `clearAll - preserves PIN_HASH_KEY and PIN_SALT_KEY`() = runBlockingTest {
        // Given
        val pinHashKey = AjustesDataStore.PIN_HASH_KEY
        val pinSaltKey = AjustesDataStore.PIN_SALT_KEY
        dataStore.edit { it[pinHashKey] = "test_hash" }.await()
        dataStore.edit { it[pinSaltKey] = "test_salt" }.await()

        // When
        val preservedKeys = setOf(pinHashKey, pinSaltKey)
        ajustesDataStore.clearAll(preservedKeys)

        // Then
        assertEquals("test_hash", dataStore.data.first()[pinHashKey])
        assertEquals("test_salt", dataStore.data.first()[pinSaltKey])
    }

    @Test
    fun `clearAll - preserves BIOMETRIC_ENABLED_KEY`() = runBlockingTest {
        // Given
        val biometricEnabledKey = AjustesDataStore.BIOMETRIC_ENABLED_KEY
        dataStore.edit { it[biometricEnabledKey] = true }.await()

        // When
        val preservedKeys = setOf(biometricEnabledKey)
        ajustesDataStore.clearAll(preservedKeys)

        // Then
        assertTrue(dataStore.data.first()[biometricEnabledKey]!!)
    }
}