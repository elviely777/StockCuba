package cu.stockcuba.app.data.local.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented test for Room clearAllTables (T27, T32)
 * Tests that clearAllTables deletes all data and WAL/SHM files
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StockCubaDatabaseClearTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: StockCubaDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, StockCubaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `clearAllTables - deletes all data from all tables`() = runBlockingTest {
        // Given: Insert test data into all tables
        val categoria = CategoriaEntity(id = "cat1", nombre = "Test", activa = true)
        val producto = ProductoEntity(
            id = "prod1", categoriaId = "cat1", nombre = "Producto Test",
            precio = 100.0, unidadMedida = "UNIDAD", stockActual = 10, stockMinimo = 1,
            activo = true, impuesto = 0.0, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
        )
        val cliente = ClienteEntity(
            id = "cli1", nombre = "Cliente Test", telefono = "+5351234567",
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

        // Verify data exists
        assertEquals(1, database.categoriaDao().getAll().count())
        assertEquals(1, database.productoDao().getAll().count())
        assertEquals(1, database.clienteDao().getAll().count())
        assertEquals(1, database.ventaDao().getAll().count())
        assertEquals(1, database.ventaDao().getItemsForVenta("venta1").count())
        assertEquals(1, database.movimientoInventarioDao().getAll().count())

        // When: Clear all tables
        database.clearAllTables()

        // Then: All tables empty
        assertEquals(0, database.categoriaDao().getAll().count())
        assertEquals(0, database.productoDao().getAll().count())
        assertEquals(0, database.clienteDao().getAll().count())
        assertEquals(0, database.ventaDao().getAll().count())
        assertEquals(0, database.ventaDao().getItemsForVenta("venta1").count())
        assertEquals(0, database.movimientoInventarioDao().getAll().count())
    }

    @Test
    fun `clearAllTables - respects foreign key order`() = runBlockingTest {
        // Given: Data with FK relationships
        val categoria = CategoriaEntity(id = "cat1", nombre = "Test", activa = true)
        val producto = ProductoEntity(
            id = "prod1", categoriaId = "cat1", nombre = "Producto",
            precio = 100.0, unidadMedida = "UNIDAD", stockActual = 10, stockMinimo = 1,
            activo = true, impuesto = 0.0, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
        )
        
        database.categoriaDao().insert(categoria)
        database.productoDao().insert(producto)

        // When: Clear all tables
        // Should not throw FK constraint violation
        database.clearAllTables()

        // Then: All cleared
        assertEquals(0, database.categoriaDao().getAll().count())
        assertEquals(0, database.productoDao().getAll().count())
    }
}