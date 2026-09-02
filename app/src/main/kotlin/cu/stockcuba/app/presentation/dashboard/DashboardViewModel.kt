package cu.stockcuba.app.presentation.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.CierreDiario
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.repository.*
import cu.stockcuba.app.domain.usecase.ObtenerProductosBajoStockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val productoRepository: ProductoRepository,
    private val reportRepository: ReportRepository,
    private val cierreRepository: CierreRepository,
    private val businessRepository: BusinessRepository,
    private val obtenerProductosBajoStockUseCase: ObtenerProductosBajoStockUseCase
) : ViewModel() {

    private val _timeRange = MutableStateFlow(DashboardTimeRange.HOY)
    val timeRange = _timeRange.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        _timeRange,
        obtenerProductosBajoStockUseCase(),
        ventaRepository.getAll(),
        productoRepository.getAll(),
        cierreRepository.getHistoricoCierres(),
        businessRepository.getFacturacionEstimada(LocalDate.now().monthValue - 1, LocalDate.now().year)
    ) { array ->
        val range = array[0] as DashboardTimeRange
        val productosBajoStock = array[1] as List<Producto>
        val allVentas = array[2] as List<Venta>
        val allProductos = array[3] as List<Producto>
        val cierres = array[4] as List<CierreDiario>
        val facturacion = array[5] as Double

        val now = LocalDate.now()
        val startAndEnd = range.getTimestamps(now)
        val periodStart = startAndEnd.first
        val periodEnd = startAndEnd.second
        
        val prevStartAndEnd = range.getPreviousTimestamps(now)
        
        val periodVentas = allVentas.filter { it.fecha.toEpochMilli() in periodStart..periodEnd }
        val prevVentas = allVentas.filter { it.fecha.toEpochMilli() in prevStartAndEnd.first..prevStartAndEnd.second }

        // Buscar si hay cierre hoy
        val inicioHoy = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val cierreHoy = cierres.find { it.fecha.toEpochMilli() == inicioHoy }

        val totalVendido = periodVentas.sumOf { it.total }
        val totalPrevio = prevVentas.sumOf { it.total }
        
        val ticketPromedio = if (periodVentas.isNotEmpty()) totalVendido / periodVentas.size else 0.0
        
        val efectivo = periodVentas.sumOf { it.montoEfectivo }
        val transferencia = periodVentas.sumOf { it.montoTransferencia }

        // IPB e IPC (T66)
        val activeProductos = allProductos.filter { it.activo }
        val ipb = activeProductos.sumOf { it.stockActual * it.precioVenta }
        val ipc = activeProductos.sumOf { it.stockActual * it.costoUnitario }
        val gananciaProyectada = ipb - ipc

        val topProducto = periodVentas.flatMap { it.items }
            .groupBy { it.productoId }
            .maxByOrNull { it.value.sumOf { item -> item.cantidad } }
            ?.let { (id, items) ->
                VentaRepository.ProductoMasVendido(
                    productoId = id,
                    nombreProducto = items.first().nombreProducto,
                    cantidadTotal = items.sumOf { it.cantidad },
                    totalVendido = items.sumOf { it.subtotal }
                )
            }

        val tendenciaTotal = calcularTendencia(totalVendido, totalPrevio)
        val tendenciaVentas = calcularTendencia(periodVentas.size.toDouble(), prevVentas.size.toDouble())
        
        val progreso = if (totalPrevio > 0) (totalVendido / totalPrevio).toFloat() else 1.0f

        DashboardUiState.Success(
            timeRange = range,
            totalVendido = totalVendido,
            cantidadVentas = periodVentas.size,
            ticketPromedio = ticketPromedio,
            productoMasVendido = topProducto,
            montoEfectivo = efectivo,
            montoTransferencia = transferencia,
            metaVenta = totalPrevio,
            progresoMeta = progreso,
            valorInventarioVenta = ipb,
            valorInventarioCosto = ipc,
            gananciaProyectada = gananciaProyectada,
            listaProductosBajoStock = productosBajoStock,
            ventasRecientes = allVentas.take(5),
            tendenciaTotal = tendenciaTotal,
            tendenciaVentas = tendenciaVentas,
            ultimoCierre = cierreHoy,
            facturacionEstimada = facturacion,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    fun setTimeRange(range: DashboardTimeRange) {
        _timeRange.value = range
    }

    suspend fun exportarReporteDiario(): Result<Uri> {
        return reportRepository.generarReporteDiarioXlsx()
    }

    /**
     * Realiza el cierre formal del día actual y genera el reporte Excel.
     */
    suspend fun realizarCierreDelDia(notas: String = ""): Result<Uri> {
        val hoy = Instant.now()
        // 1. Registrar el cierre en la base de datos
        val cierreResult = cierreRepository.realizarCierre(hoy, notas)
        if (cierreResult is Result.Failure) return Result.Failure(cierreResult.error)

        // 2. Generar el reporte Excel (que ahora representa el estado final del día)
        return reportRepository.generarReporteDiarioXlsx()
    }

    private fun calcularTendencia(actual: Double, anterior: Double): String {
        return when {
            anterior == 0.0 && actual == 0.0 -> "—"
            anterior == 0.0 -> "+100%"
            else -> {
                val cambio = ((actual - anterior) / anterior) * 100
                if (cambio > 0) "+${"%.1f".format(cambio)}%"
                else if (cambio < 0) "%.1f".format(cambio) + "%"
                else "0%"
            }
        }
    }
}

private fun DashboardTimeRange.getTimestamps(now: LocalDate): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val start = when (this) {
        DashboardTimeRange.HOY -> now.atStartOfDay(zone)
        DashboardTimeRange.SEMANA -> now.minusDays(now.dayOfWeek.value.toLong() - 1).atStartOfDay(zone)
        DashboardTimeRange.MES -> now.withDayOfMonth(1).atStartOfDay(zone)
    }.toInstant().toEpochMilli()
    
    val end = now.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
    return Pair(start, end)
}

private fun DashboardTimeRange.getPreviousTimestamps(now: LocalDate): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    return when (this) {
        DashboardTimeRange.HOY -> {
            val yesterday = now.minusDays(1)
            Pair(yesterday.atStartOfDay(zone).toInstant().toEpochMilli(), yesterday.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli())
        }
        DashboardTimeRange.SEMANA -> {
            val lastWeek = now.minusWeeks(1)
            val start = lastWeek.minusDays(lastWeek.dayOfWeek.value.toLong() - 1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = lastWeek.plusDays(7 - lastWeek.dayOfWeek.value.toLong()).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
            Pair(start, end)
        }
        DashboardTimeRange.MES -> {
            val lastMonth = now.minusMonths(1)
            val start = lastMonth.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
            Pair(start, end)
        }
    }
}
