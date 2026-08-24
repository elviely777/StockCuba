package cu.stockcuba.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.ReportRepository
import cu.stockcuba.app.domain.repository.VentaRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import cu.stockcuba.app.domain.repository.ClienteRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ventaRepository: VentaRepository,
    private val productoRepository: ProductoRepository,
    private val clienteRepository: ClienteRepository
) : ReportRepository {

    private val contentResolver = context.contentResolver

    override suspend fun generarReporteDiarioExcel(fecha: LocalDate): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startOfDay = fecha.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
            
            val ventas = ventaRepository.getVentasPorRango(startOfDay, endOfDay).first()
            val clientes = clienteRepository.getAll().first().associateBy { it.id }

            val fileName = "Reporte_Diario_${fecha.format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.csv"
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/StockCuba/Reportes/")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val uri = contentResolver.insert(collection, contentValues)
                ?: throw IOException("Error al crear archivo de reporte")

            contentResolver.openOutputStream(uri)?.use { output ->
                csvWriter().open(output) {
                    writeRow(listOf("HORA", "CLIENTE", "PRODUCTOS", "METODO PAGO", "EFECTIVO", "TRANSFERENCIA", "TOTAL"))
                    ventas.forEach { venta ->
                        val resumenProductos = venta.items.joinToString(" | ") { "${it.nombreProducto} (x${it.cantidad})" }
                        val clienteNombre = venta.clienteId?.let { clientes[it]?.nombre } ?: "Consumidor Final"
                        writeRow(listOf(
                            venta.fecha.atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("hh:mm a")),
                            clienteNombre,
                            resumenProductos,
                            venta.metodoPago.name,
                            venta.montoEfectivo,
                            venta.montoTransferencia,
                            venta.total
                        ))
                    }
                    writeRow(listOf(""))
                    writeRow(listOf("TOTAL DIA", "", "", "", ventas.sumOf { it.montoEfectivo }, ventas.sumOf { it.montoTransferencia }, ventas.sumOf { it.total }))
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publishValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                contentResolver.update(uri, publishValues, null, null)
            }

            Result.Success(uri)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun generarReporteInventarioExcel(): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val productos = productoRepository.getAll().first().filter { it.activo }
            val fileName = "Estado_Inventario_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.csv"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/StockCuba/Reportes/")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val uri = contentResolver.insert(collection, contentValues)
                ?: throw IOException("Error al crear archivo de inventario")

            contentResolver.openOutputStream(uri)?.use { output ->
                csvWriter().open(output) {
                    writeRow(listOf("PRODUCTO", "STOCK ACTUAL", "UM", "COSTO UNIT.", "PRECIO VENTA", "IPB (VALOR VENTA)", "IPC (INVERSION)"))
                    productos.forEach { p ->
                        val ipb = p.stockActual * p.precioVenta
                        val ipc = p.stockActual * p.costoUnitario
                        writeRow(listOf(p.nombre, p.stockActual, p.unidadMedida.name, p.costoUnitario, p.precioVenta, ipb, ipc))
                    }
                    writeRow(listOf(""))
                    writeRow(listOf("TOTALES", "", "", "", "", productos.sumOf { it.stockActual * it.precioVenta }, productos.sumOf { it.stockActual * it.costoUnitario }))
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publishValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                contentResolver.update(uri, publishValues, null, null)
            }

            Result.Success(uri)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }
}
