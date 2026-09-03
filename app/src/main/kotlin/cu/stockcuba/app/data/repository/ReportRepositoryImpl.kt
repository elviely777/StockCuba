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
import java.time.Instant
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
import cu.stockcuba.app.domain.repository.InventarioRepository
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.TipoMovimientoInventario
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
    private val inventarioRepository: InventarioRepository,
    private val ajustesDataStore: AjustesDataStore
) : ReportRepository {

    private val contentResolver = context.contentResolver

    private fun getReportsDir(): String = "${Environment.DIRECTORY_DOWNLOADS}/StockCuba/Reportes/"

    // ==========================================================
    //  REPORTE DE CIERRE DIARIO — 6 hojas
    // ==========================================================
    override suspend fun generarReporteDiarioXlsx(fecha: Long): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val zoneId = ZoneId.systemDefault()
            val localDate = Instant.ofEpochMilli(fecha).atZone(zoneId).toLocalDate()
            val startOfDay = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfDay   = startOfDay + 24 * 60 * 60 * 1000 - 1

            val ventas = ventaRepository.getVentasPorRango(startOfDay, endOfDay).first()
            val movimientos = inventarioRepository.getHistorialPorRango(startOfDay, endOfDay).first()
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
            data class VentaProducto(
                var unidades: Int = 0,
                var totalFacturado: Double = 0.0
            )
            val ventasPorProducto = mutableMapOf<String, VentaProducto>()
            ventas.forEach { venta ->
                venta.items.forEach { item ->
                    val acumulado = ventasPorProducto.getOrPut(item.productoId) { VentaProducto() }
                    acumulado.unidades += item.cantidad
                    acumulado.totalFacturado += item.subtotal
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

            fila = escribirTitulo(workbook, sheetResumen, "STOCKCUBA – GESTIÓN INTELIGENTE", fila)
            fila = escribirTitulo(workbook, sheetResumen, "RESUMEN EJECUTIVO – CIERRE DEL DÍA", fila)
            fila = escribirTitulo(workbook, sheetResumen, "Negocio: $nombreNegocio", fila)
            fila = escribirTitulo(workbook, sheetResumen, "Fecha: ${localDate.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy"))}", fila)
            fila = escribirTitulo(workbook, sheetResumen, "Generado: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a"))}", fila)
            fila++

            fila = escribirSubtitulo(workbook, sheetResumen, "=== RESUMEN DE CAJA ===", fila)
            fila = escribirDato(workbook, sheetResumen, "Total Recaudado", totalRecaudado.formatoCUP(), fila)
            fila = escribirDato(workbook, sheetResumen, "Cobrado en Efectivo", totalEfectivo.formatoCUP(), fila)
            fila = escribirDato(workbook, sheetResumen, "Cobrado por Transferencia", totalTransferencia.formatoCUP(), fila)
            fila = escribirDato(workbook, sheetResumen, "Cantidad de Ventas", cantidadVentas.toString(), fila)
            fila = escribirDato(workbook, sheetResumen, "Ticket Promedio", ticketPromedio.formatoCUP(), fila)
            fila++

            val gananciaDia = ventasPorCategoria.values.sumOf { it.ganancia }
            val margenPorcentaje = if (totalRecaudado > 0) (gananciaDia / totalRecaudado) * 100 else 0.0
            fila = escribirSubtitulo(workbook, sheetResumen, "=== INDICADORES DEL DÍA ===", fila)
            fila = escribirDato(workbook, sheetResumen, "Ganancia Bruta del Día", gananciaDia.formatoCUP(), fila)
            fila = escribirDato(workbook, sheetResumen, "Margen sobre Ventas", "%.2f%%".format(margenPorcentaje), fila)
            fila = escribirDato(workbook, sheetResumen, "Unidades Totales Vendidas", ventasPorProducto.values.sumOf { it.unidades }.toString(), fila)
            fila++

            fila = escribirSubtitulo(workbook, sheetResumen, "=== VALOR DE INVENTARIO RESTANTE ===", fila)
            fila = escribirDato(workbook, sheetResumen, "IPB (a Precio de Venta)", ipb.formatoCUP(), fila)
            fila = escribirDato(workbook, sheetResumen, "IPC (a Precio de Costo)", ipc.formatoCUP(), fila)
            fila = escribirDato(workbook, sheetResumen, "Utilidad Proyectada", utilidadProyectada.formatoCUP(), fila)
            fila++

            fila = escribirSubtitulo(workbook, sheetResumen, "=== ALERTAS DE STOCK BAJO ===", fila)
            if (productosStockBajo.isEmpty()) {
                fila = escribirDato(workbook, sheetResumen, "Estado", "Sin alertas de stock bajo", fila)
            } else {
                productosStockBajo.forEach { p ->
                    fila = escribirDato(workbook, sheetResumen, p.nombre, "Stock: ${p.stockActual} (mínimo: ${p.stockMinimo})", fila)
                }
            }

            sheetResumen.setColumnWidth(0, 8000)
            sheetResumen.setColumnWidth(1, 6000)

            // ==========================================================
            //  HOJA 2 — RANKING DE PRODUCTOS
            // ==========================================================
            val sheetRanking = workbook.createSheet("Ranking de Productos")
            val encabezadoRanking = sheetRanking.createRow(0)
            listOf("PRODUCTO", "CATEGORÍA", "UNIDADES VENDIDAS", "TOTAL FACTURADO", "COSTO UNITARIO", "PRECIO VENTA", "MARGEN UNITARIO", "GANANCIA TOTAL", "STOCK RESTANTE")
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

            sheetRanking.setColumnWidth(0, 7500)
            sheetRanking.setColumnWidth(1, 5500)
            sheetRanking.setColumnWidth(3, 4500)

            // ==========================================================
            //  HOJA 3 — DETALLE DE VENTAS
            // ==========================================================
            val sheetDetalle = workbook.createSheet("Detalle de Ventas")
            val encabezadoDetalle = sheetDetalle.createRow(0)
            listOf("HORA", "CLIENTE", "PRODUCTOS VENDIDOS", "MÉTODO", "EFECTIVO", "TRANSF.", "ID TRANSF.", "TOTAL VENTA")
                .forEachIndexed { idx, titulo ->
                    encabezadoDetalle.createCell(idx).setCellValue(titulo)
                    encabezadoDetalle.getCell(idx).setCellStyle(crearEstiloTitulo(workbook))
                }

            ventas.sortedBy { it.fecha }.forEachIndexed { idx, venta ->
                val row = sheetDetalle.createRow(idx + 1)
                val hora = venta.fecha.atZone(zoneId).format(DateTimeFormatter.ofPattern("hh:mm a"))
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
                row.createCell(6).setCellValue(venta.idTransferencia ?: "—")
                row.createCell(7).setCellValue(venta.total.formatoCUP())
                if (idx % 2 == 0) {
                    (0..7).forEach { row.getCell(it).setCellStyle(crearEstiloFilaPar(workbook)) }
                }
            }

            sheetDetalle.setColumnWidth(2, 12000)
            sheetDetalle.setColumnWidth(6, 4500)

            // ==========================================================
            //  HOJA 4 — ESTADO DEL INVENTARIO (DETALLADO)
            // ==========================================================
            val sheetInv = workbook.createSheet("Estado del Inventario")
            val encabezadoInv = sheetInv.createRow(0)
            listOf("PRODUCTO", "CATEGORÍA", "ESTADO", "STOCK INICIAL", "ENTRADAS", "VENTAS", "AJUSTES", "STOCK FINAL", "U.M.", "VALOR COSTO", "VALOR VENTA")
                .forEachIndexed { idx, titulo ->
                    encabezadoInv.createCell(idx).setCellValue(titulo)
                    encabezadoInv.getCell(idx).setCellStyle(crearEstiloTitulo(workbook))
                }

            val movsPorProducto = movimientos.groupBy { it.productoId }

            productosActivos.sortedBy { it.nombre }.forEachIndexed { idx, p ->
                val row = sheetInv.createRow(idx + 1)
                val cat = categoriasMap[p.categoriaId]?.nombre ?: "Sin categoría"
                val estado = when {
                    p.stockActual <= 0 -> "CRÍTICO (SIN STOCK)"
                    p.stockActual <= p.stockMinimo -> "BAJO"
                    else -> "OK"
                }

                val entradas = movsPorProducto[p.id]?.filter { it.tipo == TipoMovimientoInventario.ENTRADA }?.sumOf { it.cantidad } ?: 0
                val salidas = movsPorProducto[p.id]?.filter { it.tipo == TipoMovimientoInventario.SALIDA || it.tipo == TipoMovimientoInventario.VENTA }?.sumOf { it.cantidad } ?: 0
                val ajustes = movsPorProducto[p.id]?.filter { it.tipo == TipoMovimientoInventario.AJUSTE }?.sumOf { it.cantidadConSigno } ?: 0
                val stockInicial = p.stockActual - (entradas - salidas + ajustes)
                
                row.createCell(0).setCellValue(p.nombre)
                row.createCell(1).setCellValue(cat)
                row.createCell(2).setCellValue(estado)
                row.createCell(3).setCellValue(stockInicial.toDouble())
                row.createCell(4).setCellValue(entradas.toDouble())
                row.createCell(5).setCellValue(salidas.toDouble())
                row.createCell(6).setCellValue(ajustes.toDouble())
                row.createCell(7).setCellValue(p.stockActual.toDouble())
                row.createCell(8).setCellValue(p.unidadMedida.name)
                row.createCell(9).setCellValue((p.stockActual * p.costoUnitario).formatoCUP())
                row.createCell(10).setCellValue((p.stockActual * p.precioVenta).formatoCUP())
                
                if (idx % 2 == 0) {
                    (0..10).forEach { row.getCell(it).setCellStyle(crearEstiloFilaPar(workbook)) }
                }
            }
            sheetInv.setColumnWidth(0, 7500)
            sheetInv.setColumnWidth(1, 5500)

            // ==========================================================
            //  HOJA 5 — ANÁLISIS POR CATEGORÍA
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

            // ---- Persistir el archivo ----
            val fileName = "ReporteDiario_${localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}.xlsx"
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

    override suspend fun generarReporteMensualXlsx(mes: Int, anio: Int): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val zoneId = ZoneId.systemDefault()
            val yearMonth = java.time.YearMonth.of(anio, mes)
            val startOfMonth = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfMonth   = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999999999).atZone(zoneId).toInstant().toEpochMilli()

            val ventas = ventaRepository.getVentasPorRango(startOfMonth, endOfMonth).first()
            val productos = productoRepository.getAll().first()
            val productosMap = productos.associateBy { it.id }
            val nombreNegocio = ajustesDataStore.nombreNegocio.first()

            val workbook = XSSFWorkbook()

            // 1. Resumen Ejecutivo Mensual
            val sheetResumen = workbook.createSheet("Resumen Mensual")
            var fila = 0
            fila = escribirTitulo(workbook, sheetResumen, "STOCKCUBA – REPORTE MENSUAL", fila)
            fila = escribirTitulo(workbook, sheetResumen, "Mes: ${yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "CU")))}", fila)
            fila = escribirTitulo(workbook, sheetResumen, "Negocio: $nombreNegocio", fila)
            fila++

            val totalRecaudado = ventas.sumOf { it.total }
            val totalEfectivo = ventas.sumOf { it.montoEfectivo }
            val totalTransferencia = ventas.sumOf { it.montoTransferencia }
            
            val totalCostos = ventas.flatMap { it.items }.sumOf { item ->
                val p = productosMap[item.productoId]
                (p?.costoUnitario ?: 0.0) * item.cantidad
            }
            val gananciaReal = totalRecaudado - totalCostos
            
            val diasVenta = ventas.groupBy { it.fecha.atZone(zoneId).toLocalDate() }.size
            val promedioDiario = if (diasVenta > 0) totalRecaudado / diasVenta else 0.0

            fila = escribirDato(workbook, sheetResumen, "Total Facturado Mes", totalRecaudado.formatoCUP(), fila)
            fila = escribirDato(workbook, sheetResumen, "Costo de Mercancía (Gastos)", totalCostos.formatoCUP(), fila)
            fila = escribirDato(workbook, sheetResumen, "Ganancia Real del Mes", gananciaReal.formatoCUP(), fila)
            fila++
            fila = escribirDato(workbook, sheetResumen, "Total Efectivo", totalEfectivo.formatoCUP(), fila)
            fila = escribirDato(workbook, sheetResumen, "Total Transferencia", totalTransferencia.formatoCUP(), fila)
            fila = escribirDato(workbook, sheetResumen, "Días con Actividad", diasVenta.toString(), fila)
            fila = escribirDato(workbook, sheetResumen, "Promedio Venta Diaria", promedioDiario.formatoCUP(), fila)
            fila++

            // 2. Tendencia Diaria
            val sheetTendencia = workbook.createSheet("Tendencia Diaria")
            val headTendencia = sheetTendencia.createRow(0)
            listOf("DÍA", "VENTAS", "EFECTIVO", "TRANSF.", "TOTAL").forEachIndexed { i, t -> 
                headTendencia.createCell(i).setCellValue(t)
                headTendencia.getCell(i).setCellStyle(crearEstiloTitulo(workbook))
            }
            
            val ventasPorDia = ventas.groupBy { it.fecha.atZone(zoneId).toLocalDate() }
                .toSortedMap()
            
            var rIdx = 1
            ventasPorDia.forEach { (fecha, vList) ->
                val row = sheetTendencia.createRow(rIdx++)
                row.createCell(0).setCellValue(fecha.format(DateTimeFormatter.ISO_LOCAL_DATE))
                row.createCell(1).setCellValue(vList.size.toDouble())
                row.createCell(2).setCellValue(vList.sumOf { it.montoEfectivo }.formatoCUP())
                row.createCell(3).setCellValue(vList.sumOf { it.montoTransferencia }.formatoCUP())
                row.createCell(4).setCellValue(vList.sumOf { it.total }.formatoCUP())
            }

            // 3. Ranking de Productos (Mes)
            val sheetRank = workbook.createSheet("Ranking Mensual")
            val headRank = sheetRank.createRow(0)
            listOf("PRODUCTO", "UNIDADES", "FACTURADO", "GANANCIA EST.").forEachIndexed { i, t ->
                headRank.createCell(i).setCellValue(t)
                headRank.getCell(i).setCellStyle(crearEstiloTitulo(workbook))
            }

            val rankData = ventas.flatMap { it.items }
                .groupBy { it.productoId }
                .map { (pid, items) ->
                    val p = productosMap[pid]
                    val unidades = items.sumOf { it.cantidad }
                    val facturado = items.sumOf { it.subtotal }
                    val ganancia = if (p != null) unidades * (p.precioVenta - p.costoUnitario) else 0.0
                    Triple(p?.nombre ?: "Desconocido", unidades, facturado) to ganancia
                }.sortedByDescending { it.first.third }

            rIdx = 1
            rankData.forEach { (info, ganancia) ->
                val row = sheetRank.createRow(rIdx++)
                row.createCell(0).setCellValue(info.first)
                row.createCell(1).setCellValue(info.second.toDouble())
                row.createCell(2).setCellValue(info.third.formatoCUP())
                row.createCell(3).setCellValue(ganancia.formatoCUP())
            }

            // Guardar
            val fileName = "ReporteMensual_${anio}_${mes.toString().padStart(2, '0')}.xlsx"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, getReportsDir())
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Files.getContentUri("external")
            val uri = contentResolver.insert(collection, contentValues) ?: throw IOException("Error")
            contentResolver.openOutputStream(uri)?.use { workbook.write(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
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

    private fun escribirTitulo(wb: Workbook, sheet: Sheet, texto: String, filaIdx: Int): Int {
        val row = sheet.createRow(filaIdx)
        row.createCell(0).setCellValue(texto)
        row.getCell(0).setCellStyle(crearEstiloTitulo(wb))
        return filaIdx + 1
    }

    private fun escribirSubtitulo(wb: Workbook, sheet: Sheet, texto: String, filaIdx: Int): Int {
        val row = sheet.createRow(filaIdx)
        row.createCell(0).setCellValue(texto)
        row.getCell(0).setCellStyle(crearEstiloSubtitulo(wb))
        return filaIdx + 1
    }

    private fun escribirDato(wb: Workbook, sheet: Sheet, etiqueta: String, valor: String, filaIdx: Int): Int {
        val row = sheet.createRow(filaIdx)
        row.createCell(0).setCellValue(etiqueta)
        row.createCell(1).setCellValue(valor)
        row.getCell(0).setCellStyle(crearEstiloTitulo(wb))
        row.getCell(1).setCellStyle(crearEstiloDatos(wb))
        return filaIdx + 1
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