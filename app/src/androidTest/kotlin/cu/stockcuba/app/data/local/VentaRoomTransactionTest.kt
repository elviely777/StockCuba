package cu.stockcuba.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cu.stockcuba.app.data.local.dao.ProductoDao
import cu.stockcuba.app.data.local.dao.VentaDao
import cu.stockcuba.app.data.local.entity.ProductoEntity
import cu.stockcuba.app.data.local.entity.VentaEntity
import cu.stockcuba.app.data.local.entity.VentaItemEntity
import cu.stockcuba.app.data.repository.VentaRepositoryImpl
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VentaRoomTransactionTest {

    private lateinit var db: StockCubaDatabase
    private lateinit var ventaDao: VentaDao
    private lateinit var productoDao: ProductoDao
    private lateinit var ventaRepository: VentaRepositoryImpl

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, StockCubaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        ventaDao = db.ventaDao()
        productoDao = db.productoDao()

        ventaRepository = VentaRepositoryImpl(
            ventaDao = ventaDao,
            database = db,
            inventarioRepository = mockInventarioRepository()
        )

        // Insertar producto de prueba con stock
        runBlockingTest {
            productoDao.insert(ProductoEntity(
                id = "prod-test-1",
                nombre = "Producto Test",
                descripcion = "Descripción",
                precioVenta = 100.0,
                costoUnitario = 50.0,
                stockActual = 10,
                stockMinimo = 2,
                unidadMedida = "UNIDAD",
                categoriaId = "cat-1",
                fechaCreacion = System.currentTimeMillis(),
                activo = true
            ))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun mockInventarioRepository(): cu.stockcuba.app.domain.repository.InventarioRepository {
        return object : cu.stockcuba.app.domain.repository.InventarioRepository {
            override fun getHistorialPorProducto(productoId: String) = kotlinx.coroutines.flow.flowOf(emptyList())
            override fun getHistorialPorProductoYRango(productoId: String, desde: Long, hasta: Long) = kotlinx.coroutines.flow.flowOf(emptyList())
            override suspend fun registrarMovimiento(movimiento: cu.stockcuba.app.domain.model.MovimientoInventario) = cu.stockcuba.app.domain.model.Result.Success(Unit)
            override suspend fun registrarMovimientos(movimientos: List<cu.stockcuba.app.domain.model.MovimientoInventario>) = cu.stockcuba.app.domain.model.Result.Success(Unit)
            override suspend fun getTotalEntradas(productoId: String) = cu.stockcuba.app.domain.model.Result.Success(0)
            override suspend fun getTotalSalidas(productoId: String) = cu.stockcuba.app.domain.model.Result.Success(0)
            override suspend fun getTotalAjustes(productoId: String) = cu.stockcuba.app.domain.model.Result.Success(0)
        }
    }

    @Test
    fun `registrarVenta descuenta stock del producto correctamente`() = runBlockingTest {
        // Given
        val venta = cu.stockcuba.app.domain.model.Venta(
            id = "venta-test-1",
            fecha = java.time.Instant.now(),
            total = 200.0,
            metodoPago = cu.stockcuba.app.domain.model.MetodoPago.EFECTIVO,
            items = listOf(
                cu.stockcuba.app.domain.model.VentaItem(
                    id = "item-1",
                    ventaId = "venta-test-1",
                    productoId = "prod-test-1",
                    nombreProducto = "Producto Test",
                    cantidad = 3,
                    precioUnitario = 100.0,
                    subtotal = 300.0
                )
            ),
            clienteId = null
        )

        // When
        val result = ventaRepository.registrarVenta(venta)

        // Then
        assertTrue(result is cu.stockcuba.app.domain.model.Result.Success)

        // Verificar que el stock se descontó (10 - 3 = 7)
        val producto = productoDao.getByIdSync("prod-test-1")
        assertNotNull(producto)
        assertEquals(7, producto!!.stockActual)

        // Verificar que la venta se guardó
        val ventaGuardada = ventaDao.getByIdSync("venta-test-1")
        assertNotNull(ventaGuardada)
        assertEquals(200.0, ventaGuardada!!.total, 0.001)

        // Verificar que los items se guardaron
        val items = ventaDao.getItemsByVentaIdSync("venta-test-1")
        assertEquals(1, items.size)
        assertEquals(3, items.first().cantidad)
    }

    @Test
    fun `registrarVenta falla si stock insuficiente`() = runBlockingTest {
        // Given - producto con stock 2
        runBlockingTest {
            productoDao.insert(ProductoEntity(
                id = "prod-test-2",
                nombre = "Producto Stock Bajo",
                descripcion = null,
                precioVenta = 50.0,
                costoUnitario = 25.0,
                stockActual = 2,
                stockMinimo = 1,
                unidadMedida = "UNIDAD",
                categoriaId = "cat-1",
                fechaCreacion = System.currentTimeMillis(),
                activo = true
            ))
        }

        val venta = cu.stockcuba.app.domain.model.Venta(
            id = "venta-test-2",
            fecha = java.time.Instant.now(),
            total = 300.0,
            metodoPago = cu.stockcuba.app.domain.model.MetodoPago.EFECTIVO,
            items = listOf(
                cu.stockcuba.app.domain.model.VentaItem(
                    id = "item-2",
                    ventaId = "venta-test-2",
                    productoId = "prod-test-2",
                    nombreProducto = "Producto Stock Bajo",
                    cantidad = 5, // Intentar vender 5, pero solo hay 2
                    precioUnitario = 50.0,
                    subtotal = 250.0
                )
            ),
            clienteId = null
        )

        // When
        val result = ventaRepository.registrarVenta(venta)

        // Then - debería fallar por stock insuficiente
        assertTrue(result is cu.stockcuba.app.domain.model.Result.Failure)

        // Verificar que NO se guardó la venta (rollback transacción)
        val ventaGuardada = ventaDao.getByIdSync("venta-test-2")
        assertNull(ventaGuardada)

        // Verificar que el stock NO cambió (sigue siendo 2)
        val producto = productoDao.getByIdSync("prod-test-2")
        assertNotNull(producto)
        assertEquals(2, producto!!.stockActual)
    }

    @Test
    fun `registrarVenta multiples items descuenta stock de cada uno`() = runBlockingTest {
        // Given - insertar segundo producto
        runBlockingTest {
            productoDao.insert(ProductoEntity(
                id = "prod-test-3",
                nombre = "Producto 3",
                descripcion = null,
                precioVenta = 30.0,
                costoUnitario = 15.0,
                stockActual = 20,
                stockMinimo = 5,
                unidadMedida = "UNIDAD",
                categoriaId = "cat-1",
                fechaCreacion = System.currentTimeMillis(),
                activo = true
            ))
        }

        val venta = cu.stockcuba.app.domain.model.Venta(
            id = "venta-test-3",
            fecha = java.time.Instant.now(),
            total = 410.0,
            metodoPago = cu.stockcuba.app.domain.model.MetodoPago.MIXTO,
            items = listOf(
                cu.stockcuba.app.domain.model.VentaItem(
                    id = "item-3a",
                    ventaId = "venta-test-3",
                    productoId = "prod-test-1",
                    nombreProducto = "Producto Test",
                    cantidad = 2,
                    precioUnitario = 100.0,
                    subtotal = 200.0
                ),
                cu.stockcuba.app.domain.model.VentaItem(
                    id = "item-3b",
                    ventaId = "venta-test-3",
                    productoId = "prod-test-3",
                    nombreProducto = "Producto 3",
                    cantidad = 7,
                    precioUnitario = 30.0,
                    subtotal = 210.0
                )
            ),
            clienteId = "cliente-1"
        )

        // When
        val result = ventaRepository.registrarVenta(venta)

        // Then
        assertTrue(result is cu.stockcuba.app.domain.model.Result.Success)

        // Verificar stock descuento producto 1: 10 - 2 = 8
        val prod1 = productoDao.getByIdSync("prod-test-1")
        assertEquals(8, prod1!!.stockActual)

        // Verificar stock descuento producto 3: 20 - 7 = 13
        val prod3 = productoDao.getByIdSync("prod-test-3")
        assertEquals(13, prod3!!.stockActual)

        // Verificar items guardados
        val items = ventaDao.getItemsByVentaIdSync("venta-test-3")
        assertEquals(2, items.size)
    }

    @Test
    fun `registrarVenta con clienteId lo guarda correctamente`() = runBlockingTest {
        val venta = cu.stockcuba.app.domain.model.Venta(
            id = "venta-test-4",
            fecha = java.time.Instant.now(),
            total = 100.0,
            metodoPago = cu.stockcuba.app.domain.model.MetodoPago.TRANSFERENCIA,
            items = listOf(
                cu.stockcuba.app.domain.model.VentaItem(
                    id = "item-4",
                    ventaId = "venta-test-4",
                    productoId = "prod-test-1",
                    nombreProducto = "Producto Test",
                    cantidad = 1,
                    precioUnitario = 100.0,
                    subtotal = 100.0
                )
            ),
            clienteId = "cliente-123"
        )

        val result = ventaRepository.registrarVenta(venta)

        assertTrue(result is cu.stockcuba.app.domain.model.Result.Success)

        val ventaGuardada = ventaDao.getByIdSync("venta-test-4")
        assertNotNull(ventaGuardada)
        assertEquals("cliente-123", ventaGuardada!!.clienteId)
    }
}