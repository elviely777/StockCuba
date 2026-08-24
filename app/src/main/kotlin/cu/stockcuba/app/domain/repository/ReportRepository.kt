package cu.stockcuba.app.domain.repository

import android.net.Uri
import cu.stockcuba.app.domain.model.Result
import java.time.LocalDate

/**
 * Interfaz para la generación de reportes y exportaciones (T67).
 */
interface ReportRepository {
    
    /**
     * Genera un reporte detallado del día seleccionado y lo exporta a CSV (compatible con Excel).
     */
    suspend fun generarReporteDiarioExcel(fecha: LocalDate): Result<Uri>
    
    /**
     * Genera un reporte del estado actual del inventario (IPB/IPC) a CSV.
     */
    suspend fun generarReporteInventarioExcel(): Result<Uri>
}
