package cu.stockcuba.app.domain.repository

import android.net.Uri
import cu.stockcuba.app.domain.model.Result
import java.time.LocalDate

/**
 * Interfaz para la generación de reportes y exportaciones (T67).
 */
interface ReportRepository {

    /**
     * Genera un reporte detallado del día seleccionado y lo exporta a .xlsx
     * con una hoja por categoría de producto (movimientos + datos del producto).
     */
    suspend fun generarReporteDiarioXlsx(fecha: Long = System.currentTimeMillis()): Result<Uri>

    /**
     * Genera un reporte del estado actual del inventario (IPB/IPC) a CSV.
     * (se mantiene para compatibilidad; no toca la UI)
     */
    suspend fun generarReporteInventarioExcel(): Result<Uri>
}