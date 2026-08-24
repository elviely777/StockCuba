package cu.stockcuba.app.data.repository

import android.content.ContentValues
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

import cu.stockcuba.app.domain.repository.VentaRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import cu.stockcuba.app.domain.repository.ClienteRepository
import cu.stockcuba.app.domain.repository.CategoriaRepository
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.presentation.dashboard.formatoCUP

import cu.stockcuba.app.domain.repository.ReportRepository
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore

@Singleton
class ReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ventaRepository: VentaRepository,
    private val productoRepository: ProductoRepository,
    private val clienteRepository: ClienteRepository,
    private val categoriaRepository: CategoriaRepository,
    private val ajustesDataStore: AjustesDataStore
) : ReportRepository {

    private val contentResolver = context.contentResolver

    private fun getReportsDir(): String = "${Environment.DIRECTORY_DOWNLOADS}/StockCuba/Reportes/"

    // ----------  Reporte diario en XLSX ----------
    override suspend fun generarReporteDiarioXlsx(): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay   = startOfDay + 24 * 60 * 60 * 1000 - 1

            val ventas = ventaRepository.getVentasPorRango(startOfDay, endOfDay).first()
            val clientes = clienteRepository.getAll().first().associateBy { it.id }
            val nombreNegocio = ajustesDataStore.nombreNegocio.first()

            val workbook = XSSFWorkbook()
            val reportsDir = getReportsDir()

            val categoriasMap = categoriaRepository.getAll().first().associateBy { it.id }
            val productosActivos = productoRepository.getAll().first().filter { it.activo }
            val categoriaIds = productosActivos.map { it.categoriaId }.distinct()

            for (categoriaId in categoriaIds) {
                val nombreCategoria = categoriasMap[categoriaId]?.nombre ?: "Sin categoría"
                val sheet = workbook.createSheet(nombreCategoria)

                // Encabezado
                val r0 = sheet.createRow(0)
                r0.createCell(0).setCellValue("STOCKCUBA – GESTIÓN INTELIGENTE")
                r0.getCell(0).setCellStyle(crearEstiloTitulo(workbook))

                val r1 = sheet.createRow(1)
                r1.createCell(0).setCellValue("REPORTE DIARIO DE OPERACIONES")
                r1.getCell(0).setCellStyle(crearEstiloTitulo(workbook))

                val r2 = sheet.createRow(2)
                r2.createCell(0).setCellValue("Negocio: ${nombreNegocio}")
                r2.getCell(0).setCellStyle(crearEstiloTitulo(workbook))

                val r3 = sheet.createRow(3)
                r3.createCell(0).setCellValue("Fecha: ${LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy"))}")
                r3.getCell(0).setCellStyle(crearEstiloTitulo(workbook))

                val r4 = sheet.createRow(4)
                r4.createCell(0).setCellValue("Generado: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"))}")
                r4.getCell(0).setCellStyle(crearEstiloTitulo(workbook))

                val r5 = sheet.createRow(5)
                r5.createCell(0).setCellValue("")
                r5.getCell(0).setCellStyle(crearEstiloTitulo(workbook))

                // Resumen financiero
                val r6 = sheet.createRow(6)
                r6.createCell(0).setCellValue("=== RESUMEN DEL DÍA ===")
                r6.getCell(0).setCellStyle(crearEstiloSubtitulo(workbook))

                val r7 = sheet.createRow(7)
                r7.createCell(0).setCellValue("Métrica")
                r7.createCell(1).setCellValue("Valor")
                r7.getCell(0).setCellStyle(crearEstiloTitulo(workbook))
                r7.getCell(1).setCellStyle(crearEstiloTitulo(workbook))

                val totalRecaudado = ventas.sumOf { it.total }
                val r8 = sheet.createRow(8)
                r8.createCell(0).setCellValue("Total Recaudado")
                r8.createCell(1).setCellValue(totalRecaudado.formatoCUP())
                r8.getCell(0).setCellStyle(crearEstiloTitulo(workbook))
                r8.getCell(1).setCellStyle(crearEstiloDatos(workbook))

                val r9 = sheet.createRow(9)
                r9.createCell(0).setCellValue("Ventas Totales")
                r9.createCell(1).setCellValue(ventas.size.toDouble())
                r9.getCell(0).setCellStyle(crearEstiloTitulo(workbook))
                r9.getCell(1).setCellStyle(crearEstiloDatos(workbook))

                val r10 = sheet.createRow(10)
                r10.createCell(0).setCellValue("Ticket Promedio")
                r10.createCell(1).setCellValue(if (ventas.isNotEmpty()) (totalRecaudado / ventas.size).formatoCUP() else 0.0.formatoCUP())
                r10.getCell(0).setCellStyle(crearEstiloTitulo(workbook))
                r10.getCell(1).setCellStyle(crearEstiloDatos(workbook))

                val r11 = sheet.createRow(11)
                r11.createCell(0).setCellValue("Cobrado en Efectivo")
                r11.createCell(1).setCellValue(ventas.sumOf { it.montoEfectivo }.formatoCUP())
                r11.getCell(0).setCellStyle(crearEstiloTitulo(workbook))
                r11.getCell(1).setCellStyle(crearEstiloDatos(workbook))

                val r12 = sheet.createRow(12)
                r12.createCell(0).setCellValue("Cobrado por Transferencia")
                r12.createCell(1).setCellValue(ventas.sumOf { it.montoTransferencia }.formatoCUP())
                r12.getCell(0).setCellStyle(crearEstiloTitulo(workbook))
                r12.getCell(1).setCellStyle(crearEstiloDatos(workbook))

                // Detalle de ventas
                val r13 = sheet.createRow(13)
                r13.createCell(0).setCellValue("=== DETALLE DE VENTAS ===")
                r13.getCell(0).setCellStyle(crearEstiloSubtitulo(workbook))

                val r14 = sheet.createRow(14)
                r14.createCell(0).setCellValue("HORA")
                r14.createCell(1).setCellValue("CLIENTE")
                r14.createCell(2).setCellValue("PRODUCTOS VENDIDOS")
                r14.createCell(3).setCellValue("MÉTODO")
                r14.createCell(4).setCellValue("EFECTIVO")
                r14.createCell(5).setCellValue("TRANSF.")
                r14.createCell(6).setCellValue("TOTAL VENTA")
                (0..6).forEach { r14.getCell(it).setCellStyle(crearEstiloTitulo(workbook)) }

                ventas.sortedBy { it.fecha }.forEachIndexed { idx, venta ->
                    val row = sheet.createRow(15 + idx)
                    val hora = venta.fecha.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("hh:mm a"))
                    val clienteNombre = venta.clienteId?.let { clientes[it]?.nombre } ?: "Consumidor Final"
                    val resumenProductos = venta.items.joinToString(" | ") {
                        "${it.nombreProducto} (x${it.cantidad})"
                    }

                    row.createCell(0).setCellValue(hora)
                    row.createCell(1).setCellValue(clienteNombre)
                    row.createCell(2).setCellValue(resumenProductos)
                    row.createCell(3).setCellValue(venta.metodoPago.name)
                    row.createCell(4).setCellValue(venta.montoEfectivo.formatoCUP())
                    row.createCell(5).setCellValue(venta.montoTransferencia.formatoCUP())
                    row.createCell(6).setCellValue(venta.total.formatoCUP())
                    if (idx % 2 == 0) {
                        (0..6).forEach { row.getCell(it).setCellStyle(crearEstiloFilaPar(workbook)) }
                    }
                }

                val finIdx = 15 + ventas.size
                val rowFin = sheet.createRow(finIdx)
                rowFin.createCell(0).setCellValue("FIN DEL REPORTE")
                rowFin.getCell(0).setCellStyle(crearEstiloTitulo(workbook))
            }

            // Persistir
            val fileName = "ReporteDiario_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}.xlsx"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
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
                ?: throw IOException("Error al crear archivo de reporte XLSX")
            contentResolver.openOutputStream(uri)?.use { output ->
                workbook.write(output)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publishValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                contentResolver.update(uri, publishValues, null, null)
            }
            workbook.close()
            Result.Success(uri)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    // ----------  Reporte de inventario en CSV (sin dependencias externas) ----------
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
                // BOM UTF-8 para que Excel abra bien los acentos
                output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

                BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8)).use { writer ->
                    fun escapeCsv(value: String): String {
                        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
                        val escaped = value.replace("\"", "\"\"")
                        return if (needsQuotes) "\"$escaped\"" else escaped
                    }

                    fun writeRow(values: List<Any>) {
                        writer.write(values.joinToString(",") { escapeCsv(it.toString()) })
                        writer.newLine()
                    }

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

    // ----------  Estilos auxiliares (XLSX) ----------
    private fun crearEstiloTitulo(wb: Workbook): CellStyle {
        val style = wb.createCellStyle()
        val font = wb.createFont()
        font.bold = true
        font.color = IndexedColors.WHITE.getIndex()
        style.setFont(font)
        style.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex())
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
        return style
    }

    private fun crearEstiloSubtitulo(wb: Workbook): CellStyle {
        val style = wb.createCellStyle()
        val font = wb.createFont()
        font.bold = true
        style.setFont(font)
        return style
    }

    private fun crearEstiloDatos(wb: Workbook): CellStyle {
        val style = wb.createCellStyle()
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"))
        return style
    }

    private fun crearEstiloFilaPar(wb: Workbook): CellStyle {
        val style = wb.createCellStyle()
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex())
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
        return style
    }
}