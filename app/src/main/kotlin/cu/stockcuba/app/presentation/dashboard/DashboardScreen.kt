package cu.stockcuba.app.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaTheme

/**
 * Pantalla Dashboard - Réplica fiel del diseño de Stitch.
 *
 * Jerarquía visual:
 * 1. Header con saludo y fecha
 * 2. Grid de 4 tarjetas de métricas (Total vendido, Ventas, Ticket promedio, Producto top)
 * 3. Sección "Alerta de Stock Bajo" con chips coral y lista de productos
 * 4. FAB flotante para "Nueva Venta" (Teal con glow)
 */
@Composable
fun DashboardScreen(
    onNavigateToNuevaVenta: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg),
            contentPadding = PaddingValues(
                top = StockCubaSpacing.Md,
                start = StockCubaSpacing.Md,
                end = StockCubaSpacing.Md,
                bottom = 100.dp
            )
        ) {
            // ===== 1. HEADER =====
            item {
                DashboardHeader()
            }

            // ===== 2. MÉTRICAS GRID =====
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    items(2) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = StockCubaSpacing.Md),
                            horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
                        ) {
                            repeat(2) {
                                Card(
                                    modifier = Modifier.weight(1f).height(120.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                    shape = Shape.Grande
                                ) { Box(modifier = Modifier.fillMaxSize()) }
                            }
                        }
                    }
                }
                is DashboardUiState.Error -> {
                    item {
                        DashboardErrorState(message = state.message)
                    }
                }
                is DashboardUiState.Success -> {
                    val metrics = listOf(
                        MetricData(
                            title = "Total Vendido",
                            value = state.totalVendidoHoy.formatoCUP(),
                            icon = "💰",
                            color = StockCubaColors.VerdeExito,
                            trend = state.tendenciaTotalVendido,
                            trendPositive = !state.tendenciaTotalVendido.startsWith("-") && state.tendenciaTotalVendido != "—"
                        ),
                        MetricData(
                            title = "Ventas Hoy",
                            value = state.cantidadVentasHoy.formatoCantidad(),
                            icon = "🧾",
                            color = StockCubaColors.IndigoMarca,
                            trend = state.tendenciaCantidadVentas,
                            trendPositive = !state.tendenciaCantidadVentas.startsWith("-") && state.tendenciaCantidadVentas != "—"
                        ),
                        MetricData(
                            title = "Ticket Promedio",
                            value = if (state.cantidadVentasHoy > 0)
                                (state.totalVendidoHoy / state.cantidadVentasHoy).formatoCUP()
                            else "0 CUP",
                            icon = "📊",
                            color = Color(0xFF8B5CF6),
                            trend = "Estable",
                            trendPositive = true
                        ),
                        MetricData(
                            title = "Producto Top",
                            value = state.productoMasVendido?.nombreProducto ?: "—",
                            icon = "🏆",
                            color = Color(0xFFF59E0B),
                            trend = state.productoMasVendido?.let { "${it.cantidadTotal} vendidos" } ?: "—",
                            trendPositive = true
                        )
                    )

                    items(metrics.chunked(2)) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
                        ) {
                            row.forEach { metric ->
                                MetricCard(metric = metric, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // ===== 3. ALERTA STOCK BAJO =====
                    item {
                        if (state.listaProductosBajoStock.isNotEmpty()) {
                            StockBajoSection(productos = state.listaProductosBajoStock)
                        } else {
                            StockOkState()
                        }
                    }
                }
            }
        }

        // ===== FAB NUEVA VENTA =====
        FloatingActionButtonNuevaVenta(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(StockCubaSpacing.Lg),
            onClick = onNavigateToNuevaVenta
        )
    }
}

/**
 * Header superior: Saludo + Fecha actual
 */
@Composable
fun DashboardHeader() {
    val hora = java.time.LocalTime.now()
    val saludo = when {
        hora.hour < 12 -> "Buenos días"
        hora.hour < 18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    val fecha = java.time.LocalDate.now().format(
        java.time.format.DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", java.util.Locale.getDefault())
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "$saludo, 👋",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = fecha.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Badge de estado de sincronización (placeholder)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = StockCubaColors.VerdeExito.copy(alpha = 0.15f)
            ),
            shape = Shape.Pequeno
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp)
                            .clip(CircleShape)
                            .background(StockCubaColors.VerdeExito)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sincronizado",
                        style = MaterialTheme.typography.labelSmall,
                        color = StockCubaColors.VerdeExito
                    )
                }
            }
        }
    }
}


data class MetricData(
    val title: String,
    val value: String,
    val icon: String,
    val color: Color,
    val trend: String,
    val trendPositive: Boolean
)

@Composable
fun MetricCard(
    metric: MetricData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = Shape.Grande, // 16dp
        border = BorderStroke(
            width = 1.dp,
            color = StockCubaColors.BordeSutil
        )
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(StockCubaSpacing.Md)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Fila superior: Icono + Trend
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp)
                            .clip(CircleShape)
                            .background(metric.color.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = metric.icon,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center)
                        )
                    }
                    Text(
                        text = metric.trend,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (metric.trendPositive) StockCubaColors.VerdeExito else StockCubaColors.CoralAlerta
                    )
                }

                // Valor principal
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Título
                Text(
                    text = metric.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun DashboardErrorState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = StockCubaColors.CoralAlertaContainer.copy(alpha = 0.2f)
        ),
        shape = Shape.Grande,
        border = BorderStroke(1.dp, StockCubaColors.CoralAlerta)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(StockCubaSpacing.Md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = StockCubaColors.CoralAlerta,
                    modifier = Modifier.padding(end = 12.dp).size(24.dp)
                )
                Column {
                    Text("Error al cargar dashboard", style = MaterialTheme.typography.titleMedium, color = StockCubaColors.CoralAlerta)
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Sección "Alerta de Stock Bajo" - diseño fiel a Stitch
 * Fondo coral sutil, chips coral, lista de productos con badge de stock
 */
@Composable
fun StockBajoSection(productos: List<cu.stockcuba.app.domain.model.Producto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = StockCubaColors.CoralAlertaContainer.copy(alpha = 0.1f)
        ),
        shape = Shape.Grande,
        border = BorderStroke(1.dp, StockCubaColors.CoralAlerta.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(StockCubaSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
        ) {
            // Header sección
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StockCubaColors.CoralAlerta,
                        modifier = Modifier.padding(end = 8.dp).size(20.dp)
                    )
                    Text(
                        text = "⚠️ Stock Bajo (${productos.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StockCubaColors.CoralAlerta
                    )
                }
                Text(
                    text = "Ver todos",
                    style = MaterialTheme.typography.labelLarge,
                    color = StockCubaColors.CoralAlerta
                )
            }

            // Lista de productos con stock bajo
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
            ) {
                productos.take(5).forEach { producto ->
                    StockBajoItem(producto = producto)
                }
            }

            if (productos.size > 5) {
                Text(
                    text = "Y ${productos.size - 5} productos más...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = StockCubaSpacing.Xs)
                )
            }
        }
    }
}

@Composable
fun StockBajoItem(producto: cu.stockcuba.app.domain.model.Producto) {
    val esCritico = producto.stockActual <= 0
    val colorStock = if (esCritico) StockCubaColors.CoralAlerta else StockCubaColors.ChipStockBajoTexto
    val fondoStock = if (esCritico) StockCubaColors.ChipStockBajoFondo else StockCubaColors.ChipStockBajoFondo

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = StockCubaSpacing.Xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${producto.stockActual.formatoCantidad()} ${producto.unidadMedida.name.lowercase(java.util.Locale.getDefault())} (mín: ${producto.stockMinimo})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Badge de stock
        Card(
            colors = CardDefaults.cardColors(containerColor = fondoStock),
            shape = Shape.Pequeno // 8dp
        ) {
            Text(
                text = if (esCritico) "SIN STOCK" else "BAJO",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = colorStock,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Estado vacío - todo OK
 */
@Composable
fun StockOkState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = StockCubaColors.VerdeExito.copy(alpha = 0.1f)
        ),
        shape = Shape.Grande,
        border = BorderStroke(1.dp, StockCubaColors.VerdeExito.copy(alpha = 0.3f))
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StockCubaSpacing.Lg)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = StockCubaColors.VerdeExito,
                    modifier = Modifier.padding(end = 12.dp).size(24.dp)
                )
                Column {
                    Text("✅ Todo en orden", style = MaterialTheme.typography.titleMedium, color = StockCubaColors.VerdeExito)
                    Text("No hay productos con stock bajo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * FAB "Nueva Venta" - Teal con glow, posición bottom-end
 */
@Composable
fun FloatingActionButtonNuevaVenta(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = StockCubaColors.FabFondo,
        contentColor = StockCubaColors.FabTexto,
        shape = Shape.Full,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp,
            focusedElevation = 12.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Nueva venta",
            modifier = Modifier.size(28.dp)
        )
    }
}