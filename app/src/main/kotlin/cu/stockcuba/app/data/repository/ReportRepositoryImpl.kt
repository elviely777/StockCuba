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
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ventaRepository: VentaRepository,
    private val productoRepository: ProductoRepository,
    private val clienteRepository: ClienteRepository,
    private val ajustesDataStore: AjustesDataStore
) : ReportRepository {

    private val contentResolver = context.contentResolver

    override suspend fun generarReporteDiarioExcel(fecha: LocalDate): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startOfDay = fecha.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
            
            val ventas = ventaRepository.getVentasPorRango(startOfDay, endOfDay).first()
            val clientes = clienteRepository.getAll().first().associateBy { it.id }
            val nombreNegocio = ajustesDataStore.nombreNegocio.first()

            val fileName = "Reporte_${fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}.csv"
            
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
                // Write UTF-8 BOM for Excel compatibility (T67)
                output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                csvWriter().open(output) {
                    // --- ENCABEZADO DE MARCA ---
                    writeRow(listOf("STOCKCUBA - GESTIÓN INTELIGENTE"))
                    writeRow(listOf("REPORTE DIARIO DE OPERACIONES"))
                    writeRow(listOf("Negocio:", nombreNegocio))
                    writeRow(listOf("Fecha:", fecha.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy"))))
                    writeRow(listOf("Generado:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"))))
                    writeRow(listOf("")) // Espacio

                    // --- SECCIÓN 1: RESUMEN FINANCIERO ---
                    writeRow(listOf("=== RESUMEN DEL DÍA ==="))
                    writeRow(listOf("Métrica", "Valor"))
                    writeRow(listOf("Total Recaudado", ventas.sumOf { it.total }))
                    writeRow(listOf("Ventas Totales", ventas.size))
                    writeRow(listOf("Ticket Promedio", if (ventas.isNotEmpty()) ventas.sumOf { it.total } / ventas.size else 0.0))
                    writeRow(listOf("Cobrado en Efectivo", ventas.sumOf { it.montoEfectivo }))
                    writeRow(listOf("Cobrado por Transferencia", ventas.sumOf { it.montoTransferencia }))
                    writeRow(listOf("")) // Espacio

                    // --- SECCIÓN 2: LOG DETALLADO ---
                    writeRow(listOf("=== DETALLE DE VENTAS ==="))
                    writeRow(listOf("HORA", "CLIENTE", "PRODUCTOS VENDIDOS", "MÉTODO", "EFECTIVO", "TRANSF.", "TOTAL VENTA"))
                    
                    ventas.sortedBy { it.fecha }.forEach { venta ->
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
                    
                    writeRow(listOf("")) // Espacio
                    writeRow(listOf("FIN DEL REPORTE"))
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
            val nombreNegocio = ajustesDataStore.nombreNegocio.first()
            val fileName = "Inventario_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}.csv"

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
                // Write UTF-8 BOM for Excel compatibility (T67)
                output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                csvWriter().open(output) {
                    // --- CABECERA ---
                    writeRow(listOf("STOCKCUBA - REPORTE DE INVENTARIO"))
                    writeRow(listOf("Negocio:", nombreNegocio))
                    writeRow(listOf("Fecha de Corte:", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                    writeRow(listOf(""))

                    // --- RESUMEN DE VALORACIÓN ---
                    val ipb = productos.sumOf { it.stockActual * it.precioVenta }
                    val ipc = productos.sumOf { it.stockActual * it.costoUnitario }
                    
                    writeRow(listOf("=== VALORACIÓN TOTAL ==="))
                    writeRow(listOf("IPB (Inventario a Precio de Venta)", ipb))
                    writeRow(listOf("IPC (Inversión a Precio de Costo)", ipc))
                    writeRow(listOf("Utilidad Proyectada", ipb - ipc))
                    writeRow(listOf(""))

                    // --- TABLA DETALLADA ---
                    writeRow(listOf("=== DETALLE POR PRODUCTO ==="))
                    writeRow(listOf("PRODUCTO", "EXISTENCIA", "UM", "COSTO UNIT.", "PRECIO VENTA", "VALOR VENTA (IPB)", "VALOR COSTO (IPC)"))
                    
                    productos.sortedBy { it.nombre }.forEach { p ->
                        writeRow(listOf(
                            p.nombre,
                            p.stockActual,
                            p.unidadMedida.name,
                            p.costoUnitario,
                            p.precioVenta,
                            p.stockActual * p.precioVenta,
                            p.stockActual * p.costoUnitario
                        ))
                    }
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
