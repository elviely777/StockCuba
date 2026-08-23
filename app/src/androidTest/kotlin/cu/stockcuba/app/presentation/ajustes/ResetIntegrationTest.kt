package cu.stockcuba.app.presentation.ajustes

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cu.stockcuba.app.data.local.database.StockCubaDatabase
import cu.stockcuba.app.data.local.dao.CategoriaDao
import cu.stockcuba.app.data.local.dao.ClienteDao
import cu.stockcuba.app.data.local.dao.MovimientoInventarioDao
import cu.stockcuba.app.data.local.dao.ProductoDao
import cu.stockcuba.app.data.local.dao.VentaDao
import cu.stockcuba.app.data.local.entity.CategoriaEntity
import cu.stockcuba.app.data.local.entity.ClienteEntity
import cu.stockcuba.app.data.local.entity.MovimientoInventarioEntity
import cu.stockcuba.app.data.local.entity.ProductoEntity
import cu.stockcuba.app.data.local.entity.VentaEntity
import cu.stockcuba.app.data.local.entity.VentaItemEntity
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Moneda
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented integration test for reset flow (T32)
 * Tests reset → fresh app state, tema/pin/biometric preserved
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ResetIntegrationTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var database: StockCubaDatabase
    private lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var ajustesDataStore: AjustesDataStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StockCubaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        dataStore = context.preferencesDataStore(name = "test_prefs")
        ajustesDataStore = AjustesDataStore(dataStore)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `reset - clears database but preserves tema, pin, biometric`() = runBlockingTest {
        // Given: Populate database with data
        val categoria = CategoriaEntity(id = "cat1", nombre = "Test", activa = true)
        val producto = ProductoEntity(
            id = "prod1", categoriaId = "cat1", nombre = "Producto",
            precio = 100.0, unidadMedida = "UNIDAD", stockActual = 10, stockMinimo = 1,
            activo = true, impuesto = 0.0, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
        )
        val cliente = ClienteEntity(
            id = "cli1", nombre = "Cliente", telefono = "+5351234567",
            email = "test@test.com", direccion = "Calle 123", createdAt = System.currentTimeMillis()
        )
        val venta = VentaEntity(
            id = "venta1", fecha = System.currentTimeMillis(), total = 100.0,
            metodoPago = "EFECTIVO", clienteId = "cli1", sincronizado = false
        )
        val ventaItem = VentaItemEntity(
            id = "item1", ventaId = "venta1", productoId = "prod1",
            cantidad = 1, precioUnitario = 100.0, subtotal = 100.0
        )
        val movimiento = MovimientoInventarioEntity(
            id = "mov1", productoId = "prod1", tipo = "ENTRADA",
            cantidad = 5, motivo = "Test", fecha = System.currentTimeMillis()
        )

        database.categoriaDao().insert(categoria)
        database.productoDao().insert(producto)
        database.clienteDao().insert(cliente)
        database.ventaDao().insert(venta)
        database.ventaDao().insertItem(ventaItem)
        database.movimientoInventarioDao().insert(movimiento)

        // And: Set preferences including preserved keys
        ajustesDataStore.guardarNombreNegocio("Mi Negocio")
        ajustesDataStore.guardarDireccion("Calle 123")
        ajustesDataStore.guardarTelefono("+5351234567")
        ajustesDataStore.guardarMoneda(Moneda.CUP)
        ajustesDataStore.guardarImpuesto(15.0)
        ajustesDataStore.guardarTema("DARK") // Should be preserved
        ajustesDataStore.guardarSeguridadBiometrica(true) // Should be preserved
        
        // Set PIN keys
        dataStore.edit { it[AjustesDataStore.PIN_HASH_KEY] = "hashed_pin" }.await()
        dataStore.edit { it[AjustesDataStore.PIN_SALT_KEY] = "salt" }.await()
        dataStore.edit { it[AjustesDataStore.BIOMETRIC_ENABLED_KEY] = true }.await()

        // Verify initial state
        assertEquals(1, database.categoriaDao().getAll().count())
        assertEquals("DARK", dataStore.data.first()[AjustesDataStore.TEMA_KEY] ?: "")
        assertEquals("hashed_pin", dataStore.data.first()[AjustesDataStore.PIN_HASH_KEY] ?: "")
        assertEquals("salt", dataStore.data.first()[AjustesDataStore.PIN_SALT_KEY] ?: "")
        assertTrue(dataStore.data.first()[AjustesDataStore.BIOMETRIC_ENABLED_KEY] ?: false)

        // When: Perform reset
        val preservedKeys = setOf(
            AjustesDataStore.TEMA_KEY,
            AjustesDataStore.PIN_HASH_KEY,
            AjustesDataStore.PIN_SALT_KEY,
            AjustesDataStore.BIOMETRIC_ENABLED_KEY
        )
        ajustesDataStore.clearAll(preservedKeys).onSuccess { 
            database.clearAllTables() 
        }

        // Then: Database cleared
        assertEquals(0, database.categoriaDao().getAll().count())
        assertEquals(0, database.productoDao().getAll().count())
        assertEquals(0, database.clienteDao().getAll().count())
        assertEquals(0, database.ventaDao().getAll().count())
        assertEquals(0, database.ventaDao().getItemsForVenta("venta1").count())
        assertEquals(0, database.movimientoInventarioDao().getAll().count())

        // And: Preserved keys still have values
        assertEquals("DARK", dataStore.data.first()[AjustesDataStore.TEMA_KEY] ?: "")
        assertEquals("hashed_pin", dataStore.data.first()[AjustesDataStore.PIN_HASH_KEY] ?: "")
        assertEquals("salt", dataStore.data.first()[AjustesDataStore.PIN_SALT_KEY] ?: "")
        assertTrue(dataStore.data.first()[AjustesDataStore.BIOMETRIC_ENABLED_KEY] ?: false)

        // And: Non-preserved keys reset to defaults
        assertEquals("Mi Negocio", ajustesDataStore.nombreNegocio.first())
        assertEquals("", ajustesDataStore.direccion.first())
        assertEquals("", ajustesDataStore.telefono.first())
        assertEquals(Moneda.CUP, ajustesDataStore.moneda.first())
        assertEquals(0.0, ajustesDataStore.impuesto.first(), 0.001)
    }

    @Test
    fun `reset - fresh app state after reset`() = runBlockingTest {
        // Given: App with data and PIN set
        val categoria = CategoriaEntity(id = "cat1", nombre = "Test", activa = true)
        database.categoriaDao().insert(categoria)
        
        ajustesDataStore.guardarTema("DARK")
        dataStore.edit { it[AjustesDataStore.PIN_HASH_KEY] = "hash" }.await()
        dataStore.edit { it[AjustesDataStore.PIN_SALT_KEY] = "salt" }.await()
        dataStore.edit { it[AjustesDataStore.BIOMETRIC_ENABLED_KEY] = true }.await()

        // When: Reset
        val preservedKeys = setOf(
            AjustesDataStore.TEMA_KEY,
            AjustesDataStore.PIN_HASH_KEY,
            AjustesDataStore.PIN_SALT_KEY,
            AjustesDataStore.BIOMETRIC_ENABLED_KEY
        )
        ajustesDataStore.clearAll(preservedKeys).onSuccess { database.clearAllTables() }

        // Then: Fresh state - no business data, but security preserved
        assertEquals(0, database.categoriaDao().getAll().count())
        
        // Security still works - PIN can be verified
        assertEquals("hash", dataStore.data.first()[AjustesDataStore.PIN_HASH_KEY])
        assertEquals("salt", dataStore.data.first()[AjustesDataStore.PIN_SALT_KEY])
        assertTrue(dataStore.data.first()[AjustesDataStore.BIOMETRIC_ENABLED_KEY]!!)
        
        // Tema preserved
        assertEquals("DARK", dataStore.data.first()[AjustesDataStore.TEMA_KEY])
    }
}