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

    // ==========================================================
    //  REPORTE DE CIERRE DIARIO — 4 hojas
    // ==========================================================
    override suspend fun generarReporteDiarioXlsx(): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay   = startOfDay + 24 * 60 * 60 * 1000 - 1

            val ventas = ventaRepository.getVentasPorRango(startOfDay, endOfDay).first()
            val clientes = clienteRepository.getAll().first().associateBy { it.id }
            val nombreNegocio = ajustesDataStore.nombreNegocio.first()

            val productos = productoRepository.getAll().first()
            val productosMap = productos.associateBy { it.id }
            val productosActivos = productos.filter { it.activo }
            val categoriasMap = categoriaRepository.getAll().first().associateBy { it.id }

            // ---- Totales generales del día ----
            val totalRecaudado = ventas.sumOf { it.total }
            val totalEfectivo = ventas.sumOf { it.montoEfectivo }
            val totalTransferencia = ventas.sumOf { it.montoTransferencia }
            val cantidadVentas = ventas.size
            val ticketPromedio = if (cantidadVentas > 0) totalRecaudado / cantidadVentas else 0.0

            // ---- Agregación por producto vendido ----
            // ⚠️ VERIFICAR: asumo que VentaItem tiene el campo "productoId".
            // Si tu VentaItem usa otro nombre (ej. idProducto, sku), cambia
            // la referencia "item.productoId" en el bloque de abajo.
            data class VentaProducto(
                var unidades: Int = 0,
                var totalFacturado: Double = 0.0
            )
            val ventasPorProducto = mutableMapOf<String, VentaProducto>()
            ventas.forEach { venta ->
                venta.items.forEach { item ->
                    val acumulado = ventasPorProducto.getOrPut(item.productoId) { VentaProducto() }
                    acumulado.unidades += item.cantidad
                    acumulado.totalFacturado += item.cantidad * (productosMap[item.productoId]?.precioVenta ?: 0.0)
                }
            }

            // ---- Agregación por categoría ----
            data class VentaCategoria(
                var unidades: Int = 0,
                var totalFacturado: Double = 0.0,
                var ganancia: Double = 0.0
            )
            val ventasPorCategoria = mutableMapOf<String, VentaCategoria>()
            ventasPorProducto.forEach { (productoId, datos) ->
                val producto = productosMap[productoId] ?: return@forEach
                val margenUnitario = producto.precioVenta - producto.costoUnitario
                val acumulado = ventasPorCategoria.getOrPut(producto.categoriaId) { VentaCategoria() }
                acumulado.unidades += datos.unidades
                acumulado.totalFacturado += datos.totalFacturado
                acumulado.ganancia += datos.unidades * margenUnitario
            }

            // ---- Valor de inventario restante (IPB / IPC) ----
            val ipb = productosActivos.sumOf { it.stockActual * it.precioVenta }
            val ipc = productosActivos.sumOf { it.stockActual * it.costoUnitario }
            val utilidadProyectada = ipb - ipc
            val productosStockBajo = productosActivos.filter { it.stockBajo }

            val workbook = XSSFWorkbook()

            // ==========================================================
            //  HOJA 1 — RESUMEN EJECUTIVO
            // ==========================================================
            val sheetResumen = workbook.createSheet("Resumen Ejecutivo")
            var fila = 0

            fun escribirTitulo(sheet: Sheet, texto: String, filaIdx: Int): Int {
                val row = sheet.createRow(filaIdx)
                row.createCell(0).setCellValue(texto)
                row.getCell(0).setCellStyle(crearEstiloTitulo(workbook))
                return filaIdx + 1
            }

            fun escribirSubtitulo(sheet: Sheet, texto: String, filaIdx: Int): Int {
                val row = sheet.createRow(filaIdx)
                row.createCell(0).setCellValue(texto)
                row.getCell(0).setCellStyle(crearEstiloSubtitulo(workbook))
                return filaIdx + 1
            }

            fun escribirDato(sheet: Sheet, etiqueta: String, valor: String, filaIdx: Int): Int {
                val row = sheet.createRow(filaIdx)
                row.createCell(0).setCellValue(etiqueta)
                row.createCell(1).setCellValue(valor)
                row.getCell(0).setCellStyle(crearEstiloTitulo(workbook))
                row.getCell(1).setCellStyle(crearEstiloDatos(workbook))
                return filaIdx + 1
            }

            fila = escribirTitulo(sheetResumen, "STOCKCUBA – GESTIÓN INTELIGENTE", fila)
            fila = escribirTitulo(sheetResumen, "RESUMEN EJECUTIVO – CIERRE DEL DÍA", fila)
            fila = escribirTitulo(sheetResumen, "Negocio: $nombreNegocio", fila)
            fila = escribirTitulo(sheetResumen, "Fecha: ${LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy"))}", fila)
            fila = escribirTitulo(sheetResumen, "Generado: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"))}", fila)
            fila++

            fila = escribirSubtitulo(sheetResumen, "=== RESUMEN DE CAJA ===", fila)
            fila = escribirDato(sheetResumen, "Total Recaudado", totalRecaudado.formatoCUP(), fila)
            fila = escribirDato(sheetResumen, "Cobrado en Efectivo", totalEfectivo.formatoCUP(), fila)
            fila = escribirDato(sheetResumen, "Cobrado por Transferencia", totalTransferencia.formatoCUP(), fila)
            fila = escribirDato(sheetResumen, "Cantidad de Ventas", cantidadVentas.toString(), fila)
            fila = escribirDato(sheetResumen, "Ticket Promedio", ticketPromedio.formatoCUP(), fila)
            fila++

            val gananciaDia = ventasPorCategoria.values.sumOf { it.ganancia }
            val margenPorcentaje = if (totalRecaudado > 0) (gananciaDia / totalRecaudado) * 100 else 0.0
            fila = escribirSubtitulo(sheetResumen, "=== INDICADORES DEL DÍA ===", fila)
            fila = escribirDato(sheetResumen, "Ganancia Bruta del Día", gananciaDia.formatoCUP(), fila)
            fila = escribirDato(sheetResumen, "Margen sobre Ventas", "%.2f%%".format(margenPorcentaje), fila)
            fila = escribirDato(sheetResumen, "Unidades Totales Vendidas", ventasPorProducto.values.sumOf { it.unidades }.toString(), fila)
            fila++

            fila = escribirSubtitulo(sheetResumen, "=== VALOR DE INVENTARIO RESTANTE ===", fila)
            fila = escribirDato(sheetResumen, "IPB (a Precio de Venta)", ipb.formatoCUP(), fila)
            fila = escribirDato(sheetResumen, "IPC (a Precio de Costo)", ipc.formatoCUP(), fila)
            fila = escribirDato(sheetResumen, "Utilidad Proyectada", utilidadProyectada.formatoCUP(), fila)
            fila++

            fila = escribirSubtitulo(sheetResumen, "=== ALERTAS DE STOCK BAJO ===", fila)
            if (productosStockBajo.isEmpty()) {
                fila = escribirDato(sheetResumen, "Estado", "Sin alertas de stock bajo", fila)
            } else {
                productosStockBajo.forEach { p ->
                    fila = escribirDato(sheetResumen, p.nombre, "Stock: ${p.stockActual} (mínimo: ${p.stockMinimo})", fila)
                }
            }

            sheetResumen.setColumnWidth(0, 8000)
            sheetResumen.setColumnWidth(1, 6000)

            // ==========================================================
            //  HOJA 2 — RANKING DE PRODUCTOS
            // ==========================================================
            val sheetRanking = workbook.createSheet("Ranking de Productos")
            val encabezadoRanking = sheetRanking.createRow(0)
            listOf("PRODUCTO", "CATEGORÍA", "UNIDADES VENDIDAS", "TOTAL FACTURADO", "COSTO UNITARIO", "PRECIO VENTA", "MARGEN UNITARIO", "GANANCIA TOTAL", "STOCK ACTUAL")
                .forEachIndexed { idx, titulo ->
                    encabezadoRanking.createCell(idx).setCellValue(titulo)
                    encabezadoRanking.getCell(idx).setCellStyle(crearEstiloTitulo(workbook))
                }

            val rankingOrdenado = ventasPorProducto.entries
                .mapNotNull { (productoId, datos) ->
                    val producto = productosMap[productoId] ?: return@mapNotNull null
                    val margenUnitario = producto.precioVenta - producto.costoUnitario
                    Triple(producto, datos, margenUnitario)
                }
                .sortedByDescending { it.second.totalFacturado }

            rankingOrdenado.forEachIndexed { idx, (producto, datos, margenUnitario) ->
                val row = sheetRanking.createRow(idx + 1)
                val nombreCategoria = categoriasMap[producto.categoriaId]?.nombre ?: "Sin categoría"
                row.createCell(0).setCellValue(producto.nombre)
                row.createCell(1).setCellValue(nombreCategoria)
                row.createCell(2).setCellValue(datos.unidades.toDouble())
                row.createCell(3).setCellValue(datos.totalFacturado.formatoCUP())
                row.createCell(4).setCellValue(producto.costoUnitario.formatoCUP())
                row.createCell(5).setCellValue(producto.precioVenta.formatoCUP())
                row.createCell(6).setCellValue(margenUnitario.formatoCUP())
                row.createCell(7).setCellValue((datos.unidades * margenUnitario).formatoCUP())
                row.createCell(8).setCellValue(producto.stockActual.toDouble())
                if (idx % 2 == 0) {
                    (0..8).forEach { row.getCell(it).setCellStyle(crearEstiloFilaPar(workbook)) }
                }
            }

            sheetRanking.setColumnWidth(0, 7500)  // Producto
            sheetRanking.setColumnWidth(1, 5500)  // Categoría
            sheetRanking.setColumnWidth(2, 4500)  // Unidades
            sheetRanking.setColumnWidth(3, 4500)  // Total facturado
            sheetRanking.setColumnWidth(4, 4000)  // Costo unitario
            sheetRanking.setColumnWidth(5, 4000)  // Precio venta
            sheetRanking.setColumnWidth(6, 4000)  // Margen unitario
            sheetRanking.setColumnWidth(7, 4500)  // Ganancia total
            sheetRanking.setColumnWidth(8, 3500)  // Stock actual

            // ==========================================================
            //  HOJA 3 — DETALLE DE VENTAS
            // ==========================================================
            val sheetDetalle = workbook.createSheet("Detalle de Ventas")
            val encabezadoDetalle = sheetDetalle.createRow(0)
            listOf("HORA", "CLIENTE", "PRODUCTOS VENDIDOS", "MÉTODO", "EFECTIVO", "TRANSF.", "TOTAL VENTA")
                .forEachIndexed { idx, titulo ->
                    encabezadoDetalle.createCell(idx).setCellValue(titulo)
                    encabezadoDetalle.getCell(idx).setCellStyle(crearEstiloTitulo(workbook))
                }

            ventas.sortedBy { it.fecha }.forEachIndexed { idx, venta ->
                val row = sheetDetalle.createRow(idx + 1)
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

            sheetDetalle.setColumnWidth(0, 3500)  // Hora
            sheetDetalle.setColumnWidth(1, 5000)  // Cliente
            sheetDetalle.setColumnWidth(2, 9000)  // Productos vendidos
            sheetDetalle.setColumnWidth(3, 3500)  // Método
            sheetDetalle.setColumnWidth(4, 4000)  // Efectivo
            sheetDetalle.setColumnWidth(5, 4000)  // Transferencia
            sheetDetalle.setColumnWidth(6, 4000)  // Total

            // ==========================================================
            //  HOJA 4 — ANÁLISIS POR CATEGORÍA
            // ==========================================================
            val sheetCategoria = workbook.createSheet("Análisis por Categoría")
            val encabezadoCategoria = sheetCategoria.createRow(0)
            listOf("CATEGORÍA", "UNIDADES VENDIDAS", "TOTAL FACTURADO", "% DEL TOTAL DEL DÍA", "GANANCIA GENERADA")
                .forEachIndexed { idx, titulo ->
                    encabezadoCategoria.createCell(idx).setCellValue(titulo)
                    encabezadoCategoria.getCell(idx).setCellStyle(crearEstiloTitulo(workbook))
                }

            val categoriaOrdenada = ventasPorCategoria.entries.sortedByDescending { it.value.totalFacturado }
            categoriaOrdenada.forEachIndexed { idx, (categoriaId, datos) ->
                val row = sheetCategoria.createRow(idx + 1)
                val nombreCategoria = categoriasMap[categoriaId]?.nombre ?: "Sin categoría"
                val porcentaje = if (totalRecaudado > 0) (datos.totalFacturado / totalRecaudado) * 100 else 0.0

                row.createCell(0).setCellValue(nombreCategoria)
                row.createCell(1).setCellValue(datos.unidades.toDouble())
                row.createCell(2).setCellValue(datos.totalFacturado.formatoCUP())
                row.createCell(3).setCellValue("%.2f%%".format(porcentaje))
                row.createCell(4).setCellValue(datos.ganancia.formatoCUP())
                if (idx % 2 == 0) {
                    (0..4).forEach { row.getCell(it).setCellStyle(crearEstiloFilaPar(workbook)) }
                }
            }

            sheetCategoria.setColumnWidth(0, 6000)  // Categoría
            sheetCategoria.setColumnWidth(1, 4500)  // Unidades
            sheetCategoria.setColumnWidth(2, 4500)  // Total facturado
            sheetCategoria.setColumnWidth(3, 4500)  // % del total
            sheetCategoria.setColumnWidth(4, 4500)  // Ganancia

            // ---- Persistir el archivo ----
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

    // ==========================================================
    //  REPORTE DE INVENTARIO EN CSV (sin cambios, sin dependencias externas)
    // ==========================================================
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

                    writeRow(listOf("STOCKCUBA - REPORTE DE INVENTARIO"))
                    writeRow(listOf("Negocio:", nombreNegocio))
                    writeRow(listOf("Fecha de Corte:", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                    writeRow(listOf(""))

                    val ipb = productos.sumOf { it.stockActual * it.precioVenta }
                    val ipc = productos.sumOf { it.stockActual * it.costoUnitario }

                    writeRow(listOf("=== VALORACIÓN TOTAL ==="))
                    writeRow(listOf("IPB (Inventario a Precio de Venta)", ipb))
                    writeRow(listOf("IPC (Inversión a Precio de Costo)", ipc))
                    writeRow(listOf("Utilidad Proyectada", ipb - ipc))
                    writeRow(listOf(""))

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

    // ==========================================================
    //  ESTILOS AUXILIARES (XLSX)
    // ==========================================================
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