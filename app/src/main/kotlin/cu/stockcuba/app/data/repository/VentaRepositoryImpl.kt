package cu.stockcuba.app.data.repository

import cu.stockcuba.app.data.local.database.StockCubaDatabase
import cu.stockcuba.app.data.local.dao.VentaDao
import cu.stockcuba.app.data.mapper.*
import cu.stockcuba.app.domain.model.*
import cu.stockcuba.app.domain.repository.InventarioRepository
import cu.stockcuba.app.domain.repository.VentaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VentaRepositoryImpl @Inject constructor(
    private val ventaDao: VentaDao,
    private val database: StockCubaDatabase,
    private val inventarioRepository: InventarioRepository
) : VentaRepository {

    override fun getAll(): Flow<List<Venta>> = ventaDao.getAll().map { it.map { it.toDomain() } }

    override fun getById(id: String): Flow<Venta?> = ventaDao.getById(id).map { it?.toDomain() }

    override fun getVentasPorRango(desde: Long, hasta: Long): Flow<List<Venta>> =
        ventaDao.getByDateRange(desde, hasta).map { it.map { it.toDomain() } }

    override fun getVentasDeHoy(): Flow<List<Venta>> {
        val inicioDia = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val finDia = inicioDia + 24 * 60 * 60 * 1000 - 1
        return ventaDao.getByDateRange(inicioDia, finDia).map { it.map { it.toDomain() } }
    }

    override fun getByCliente(clienteId: String): Flow<List<Venta>> =
        ventaDao.getByCliente(clienteId).map { it.map { it.toDomain() } }

    override suspend fun getByIdSync(id: String): Result<Venta> {
        return try {
            val entity = ventaDao.getByIdSync(id)
            entity?.let { Result.Success(it.toDomain()) } ?: Result.Failure(DomainError.NotFound("Venta", id))
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun getItemsByVentaId(ventaId: String): Result<List<VentaItem>> {
        return try {
            val items = ventaDao.getItemsByVentaIdSync(ventaId).map { it.toDomain() }
            Result.Success(items)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun registrarVenta(venta: Venta): Result<Unit> {
        return try {
            database.withTransaction {
                val (ventaEntity, itemsEntities) = venta.toEntityWithItems()
                ventaDao.insert(ventaEntity)
                ventaDao.insertItems(itemsEntities)

                // Registrar movimientos de inventario tipo VENTA
                val movimientoDao = database.movimientoInventarioDao()
                val movimientosEntities = venta.items.map { item ->
                    MovimientoInventario(
                        id = java.util.UUID.randomUUID().toString(),
                        productoId = item.productoId,
                        tipo = TipoMovimientoInventario.VENTA,
                        cantidad = item.cantidad,
                        fecha = venta.fecha,
                        motivo = "Venta #${venta.id.take(8)}"
                    ).toEntity()
                }
                movimientoDao.insertAll(movimientosEntities)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun getTotalVendidoPorRango(desde: Long, hasta: Long): Result<Double> {
        return try {
            val total = ventaDao.getTotalByDateRange(desde, hasta).first() ?: 0.0
            Result.Success(total)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun getResumenDelDia(fecha: Long): Result<VentaRepository.ResumenDia> {
        return try {
            val inicioDia = java.time.Instant.ofEpochMilli(fecha)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val finDia = inicioDia + 24 * 60 * 60 * 1000 - 1

            val totalVendido = ventaDao.getTotalByDateRange(inicioDia, finDia).first() ?: 0.0
            val ventas = ventaDao.getByDateRange(inicioDia, finDia).first()
            val cantidadVentas = ventas.size

            // Obtener producto más vendido del día
            val todosLosItems = mutableListOf<VentaDao.VentaItemWithProduct>()
            for (v in ventas) {
                todosLosItems.addAll(ventaDao.getItemsWithProductName(v.id))
            }

            val productoMasVendido = if (todosLosItems.isNotEmpty()) {
                todosLosItems
                    .groupBy { it.productoNombre }
                    .maxByOrNull { group -> group.value.sumOf { it.cantidad } }
                    ?.let { (nombre, items) ->
                        VentaRepository.ProductoMasVendido(
                            productoId = items.first().productoId,
                            nombreProducto = nombre,
                            cantidadTotal = items.sumOf { it.cantidad },
                            totalVendido = items.sumOf { it.subtotal }
                        )
                    }
            } else null

            Result.Success(VentaRepository.ResumenDia(
                fecha = fecha,
                totalVendido = totalVendido,
                cantidadVentas = cantidadVentas,
                productoMasVendido = productoMasVendido
            ))
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }
}