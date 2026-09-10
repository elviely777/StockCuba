package cu.stockcuba.app.presentation.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.CierreDiario
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.ProductInsight
import cu.stockcuba.app.domain.model.InsightTipo
import cu.stockcuba.app.domain.model.RolUsuario
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
    private val clienteRepository: cu.stockcuba.app.domain.repository.ClienteRepository,
    private val gastoRepository: cu.stockcuba.app.domain.repository.GastoRepository,
    val securityRepository: cu.stockcuba.app.domain.security.SecurityRepository,
    private val ajustesDataStore: cu.stockcuba.app.presentation.ajustes.AjustesDataStore,
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
        cierreRepository.getHistoricoCierresMensuales(),
        ajustesDataStore.rolActual,
        ajustesDataStore.nombreVendedor,
        ajustesDataStore.moneda,
        ajustesDataStore.tasaUSD,
        ajustesDataStore.tasaMLC,
        ajustesDataStore.tasaEUR,
        gastoRepository.getAll(),
        clienteRepository.getActivos()
    ) { array ->
        val range = array[0] as DashboardTimeRange
        val productosBajoStock = array[1] as List<Producto>
        val allVentas = array[2] as List<Venta>
        val allProductos = array[3] as List<Producto>
        val cierres = array[4] as List<CierreDiario>
        val cierresMensuales = array[5] as List<cu.stockcuba.app.domain.model.CierreMensual>
        val rolActual = array[6] as cu.stockcuba.app.domain.model.RolUsuario
        val nombreVendedor = array[7] as String
        val monedaBase = array[8] as cu.stockcuba.app.domain.model.Moneda
        val tasaUSD = array[9] as Double
        val tasaMLC = array[10] as Double
        val tasaEUR = array[11] as Double
        val allGastos = array[12] as List<cu.stockcuba.app.domain.model.Gasto>
        val activeClientes = array[13] as List<cu.stockcuba.app.domain.model.Cliente>

        val tasas = mapOf(
            cu.stockcuba.app.domain.model.Moneda.USD to tasaUSD,
            cu.stockcuba.app.domain.model.Moneda.MLC to tasaMLC,
            cu.stockcuba.app.domain.model.Moneda.EUR to tasaEUR,
            cu.stockcuba.app.domain.model.Moneda.CUP to 1.0,
            cu.stockcuba.app.domain.model.Moneda.CLASICA to 1.0
        )

        fun toBase(valor: Double, moneda: cu.stockcuba.app.domain.model.Moneda): Double {
            if (moneda == monedaBase) return valor
            val tasaOrigen = tasas[moneda] ?: 1.0
            val tasaDestino = tasas[monedaBase] ?: 1.0
            // Convertir a CUP primero si es necesario, luego a monedaBase
            // Pero aquí asumimos que las tasas son respecto a CUP? 
            // Si MonedaBase es CUP, simplemente multiplicamos por tasaOrigen.
            // Si MonedaBase es USD, dividimos por tasaUSD.
            
            // Lógica simplificada: tasaOrigen es "CUP por 1 unidad de moneda"
            val valorEnCUP = valor * tasaOrigen
            return valorEnCUP / tasaDestino
        }

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

        // Buscar si hay cierre mensual este mes
        val cierreMensual = cierresMensuales.find { it.mes == now.monthValue && it.anio == now.year }

        val totalVendido = periodVentas.sumOf { it.total }
        val totalPrevio = prevVentas.sumOf { it.total }

        val totalGastos = periodVentas.flatMap { it.items }.sumOf { item ->
            val producto = allProductos.find { it.id == item.productoId }
            val costoBase = toBase(producto?.costoUnitario ?: 0.0, producto?.moneda ?: cu.stockcuba.app.domain.model.Moneda.CUP)
            costoBase * item.cantidad
        }
        
        val ticketPromedio = if (periodVentas.isNotEmpty()) totalVendido / periodVentas.size else 0.0
        
        val efectivo = periodVentas.sumOf { it.montoEfectivo }
        val transferencia = periodVentas.sumOf { it.montoTransferencia }

        // Gastos Operativos (T71)
        val periodGastos = allGastos.filter { it.fecha.toEpochMilli() in periodStart..periodEnd }
        val totalGastosOperativos = periodGastos.sumOf { toBase(it.monto, it.moneda) }

        val totalPorCobrar = activeClientes.sumOf { it.saldoDeuda }

        val gananciaReal = totalVendido - totalGastos - totalGastosOperativos

        // IPB e IPC (T66)
        val activeProductos = allProductos.filter { it.activo }
        val ipb = activeProductos.sumOf { it.stockActual * toBase(it.precioVenta, it.moneda) }
        val ipc = activeProductos.sumOf { it.stockActual * toBase(it.costoUnitario, it.moneda) }
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

        // --- CÁLCULO DE INSIGHTS (DUENO ONLY) ---
        val insights = if (rolActual == RolUsuario.DUENO) {
            calcularInsights(periodVentas, allProductos, tasas, monedaBase)
        } else emptyList()

        // --- EFICIENCIA DE VENDEDORES (DUENO ONLY) ---
        val eficiencia = if (rolActual == RolUsuario.DUENO) {
            periodVentas.groupBy { it.vendedorNombre }
                .map { (nombre, ventas) ->
                    VentaRepository.EficienciaVendedor(
                        nombre = nombre,
                        cantidadVentas = ventas.size,
                        totalRecaudado = ventas.sumOf { it.total }
                    )
                }
                .sortedByDescending { it.totalRecaudado }
        } else emptyList()

        DashboardUiState.Success(
            rolActual = rolActual,
            nombreVendedor = nombreVendedor,
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
            totalGastos = totalGastos,
            totalGastosOperativos = totalGastosOperativos,
            totalPorCobrar = totalPorCobrar,
            gananciaReal = gananciaReal,
            listaProductosBajoStock = productosBajoStock,
            ventasRecientes = allVentas.take(5),
            listaInsights = insights,
            eficienciaVendedores = eficiencia,
            tendenciaTotal = tendenciaTotal,
            tendenciaVentas = tendenciaVentas,
            ultimoCierre = cierreHoy,
            ultimoCierreMensual = cierreMensual,
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

    fun cambiarRol(rol: cu.stockcuba.app.domain.model.RolUsuario) {
        viewModelScope.launch {
            ajustesDataStore.guardarRolActual(rol)
            if (rol == cu.stockcuba.app.domain.model.RolUsuario.DUENO) {
                ajustesDataStore.guardarNombreVendedor("")
            }
        }
    }

    fun cambiarRolYVendedor(rol: cu.stockcuba.app.domain.model.RolUsuario, nombre: String) {
        viewModelScope.launch {
            ajustesDataStore.guardarRolActual(rol)
            ajustesDataStore.guardarNombreVendedor(nombre)
        }
    }

    suspend fun exportarReporteDiario(): Result<Uri> {
        return reportRepository.generarReporteDiarioXlsx()
    }

    suspend fun exportarReporteMensual(): Result<Uri> {
        val now = LocalDate.now()
        return reportRepository.generarReporteMensualXlsx(now.monthValue, now.year)
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

    /**
     * Realiza el cierre formal del mes actual y genera el reporte Excel consolidado.
     */
    suspend fun realizarCierreMensual(notas: String = ""): Result<Uri> {
        val now = LocalDate.now()
        // 1. Registrar cierre mensual
        val result = cierreRepository.realizarCierreMensual(now.monthValue, now.year, notas)
        if (result is Result.Failure) return Result.Failure(result.error)

        // 2. Generar reporte mensual
        return reportRepository.generarReporteMensualXlsx(now.monthValue, now.year)
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

    private fun calcularInsights(
        ventasPeriodo: List<Venta>,
        allProductos: List<Producto>,
        tasas: Map<cu.stockcuba.app.domain.model.Moneda, Double>,
        monedaBase: cu.stockcuba.app.domain.model.Moneda
    ): List<ProductInsight> {
        val insights = mutableListOf<ProductInsight>()
        val totalVentasPorProducto = ventasPeriodo.flatMap { it.items }.groupBy { it.productoId }

        fun toBase(valor: Double, moneda: cu.stockcuba.app.domain.model.Moneda): Double {
            val tasaOrigen = tasas[moneda] ?: 1.0
            val tasaDestino = tasas[monedaBase] ?: 1.0
            return (valor * tasaOrigen) / tasaDestino
        }

        // 1. Identificar Estrellas y Alertas de Margen
        totalVentasPorProducto.forEach { (id, items) ->
            val producto = allProductos.find { it.id == id } ?: return@forEach
            val precioVentaBase = toBase(producto.precioVenta, producto.moneda)
            val costoUnitarioBase = toBase(producto.costoUnitario, producto.moneda)
            
            val gananciaUnitaria = precioVentaBase - costoUnitarioBase
            val gananciaTotal = gananciaUnitaria * items.sumOf { it.cantidad }
            val margen = if (costoUnitarioBase > 0) (gananciaUnitaria / costoUnitarioBase) * 100 else 0.0

            if (margen < 15.0 && margen > 0) {
                insights.add(ProductInsight(
                    productoId = id,
                    nombre = producto.nombre,
                    tipo = InsightTipo.ALERTA_MARGEN,
                    valorPrimario = "${"%.1f".format(margen)}% margen",
                    mensaje = "Ganancia muy baja. Considera ajustar el precio."
                ))
            } else if (margen <= 0) {
                insights.add(ProductInsight(
                    productoId = id,
                    nombre = producto.nombre,
                    tipo = InsightTipo.ALERTA_MARGEN,
                    valorPrimario = "Pérdida",
                    mensaje = "Estás vendiendo por debajo del costo."
                ))
            }
        }

        // 2. Identificar Top Ganancia (Estrella)
        totalVentasPorProducto.mapNotNull { (id, items) ->
            val producto = allProductos.find { it.id == id } ?: return@mapNotNull null
            val precioVentaBase = toBase(producto.precioVenta, producto.moneda)
            val costoUnitarioBase = toBase(producto.costoUnitario, producto.moneda)
            val gananciaTotal = (precioVentaBase - costoUnitarioBase) * items.sumOf { it.cantidad }
            Triple(id, producto.nombre, gananciaTotal)
        }.maxByOrNull { it.third }?.let { (id, nombre, ganancia) ->
            if (ganancia > 0) {
                insights.add(ProductInsight(
                    productoId = id,
                    nombre = nombre,
                    tipo = InsightTipo.ESTRELLA,
                    valorPrimario = "${ganancia.toInt().formatoCantidad()} ${monedaBase.name}",
                    mensaje = "Es el producto que más dinero real te aporta."
                ))
            }
        }

        // 3. Estancados (Stock > 5 pero 0 ventas)
        allProductos.filter { it.activo && it.stockActual > 5 }
            .filter { it.id !in totalVentasPorProducto.keys }
            .take(2)
            .forEach { producto ->
                insights.add(ProductInsight(
                    productoId = producto.id,
                    nombre = producto.nombre,
                    tipo = InsightTipo.ESTANCADO,
                    valorPrimario = "${producto.stockActual} en stock",
                    mensaje = "Sin ventas en este periodo. ¿Hacemos rebaja?"
                ))
            }

        return insights.sortedBy { it.tipo.ordinal }
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
