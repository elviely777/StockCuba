package cu.stockcuba.app.presentation.ventas

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialVentasScreen(
    onBack: () -> Unit,
    onDetalle: (String) -> Unit,
    viewModel: HistorialVentasViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Historial de Ventas", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refrescar() }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is HistorialVentasUiState.Loading -> HistorialCargando()
                is HistorialVentasUiState.Error -> HistorialError(message = state.message, onRetry = { viewModel.refrescar() })
                is HistorialVentasUiState.Success -> HistorialContenidoModerno(
                    ventasPorDia = state.ventasPorDia, 
                    onDetalle = onDetalle
                )
            }
        }
    }
}

@Composable
fun HistorialContenidoModerno(
    ventasPorDia: List<VentasPorDia>, 
    onDetalle: (String) -> Unit
) {
    if (ventasPorDia.isEmpty()) {
        HistorialVacio()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(StockCubaSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Xl)
        ) {
            items(ventasPorDia) { dia ->
                DiaVentasSectionModerno(dia = dia, onDetalle = onDetalle)
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun DiaVentasSectionModerno(dia: VentasPorDia, onDetalle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        // Cabecera del día
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = dia.fechaFormateada, 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Total: ${dia.totalDia.formatoCUP()}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Lista de ventas del día
        dia.ventas.forEach { ventaUi ->
            VentaRowModerno(ventaUi = ventaUi, onClick = { onDetalle(ventaUi.venta.id) })
        }
    }
}

@Composable
fun VentaRowModerno(ventaUi: VentaUi, onClick: () -> Unit) {
    val venta = ventaUi.venta
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault()) }
    val hora = remember(venta.fecha) { timeFormatter.format(venta.fecha) }
    
    // Crear un resumen de los productos vendidos
    val resumenProductos = remember(venta.items) {
        venta.items.take(3).joinToString(", ") { "${it.nombreProducto} x${it.cantidad}" } + 
        if (venta.items.size > 3) "..." else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(StockCubaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del método de pago
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when(venta.metodoPago) {
                            MetodoPago.EFECTIVO -> Color(0xFF2DD4BF).copy(alpha = 0.1f)
                            MetodoPago.TRANSFERENCIA -> Color(0xFF6366F1).copy(alpha = 0.1f)
                            MetodoPago.MIXTO -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(venta.metodoPago) {
                        MetodoPago.EFECTIVO -> Icons.Default.Payments
                        MetodoPago.TRANSFERENCIA -> Icons.Default.AccountBalance
                        MetodoPago.MIXTO -> Icons.Default.SwapHoriz
                    },
                    contentDescription = null,
                    tint = when(venta.metodoPago) {
                        MetodoPago.EFECTIVO -> Color(0xFF2DD4BF)
                        MetodoPago.TRANSFERENCIA -> Color(0xFF6366F1)
                        MetodoPago.MIXTO -> Color(0xFFF59E0B)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(StockCubaSpacing.Md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ventaUi.clienteNombre ?: "Consumidor Final",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "• $hora",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = if (resumenProductos.isBlank()) "Sin productos" else resumenProductos,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(StockCubaSpacing.Sm))

            Text(
                text = venta.total.formatoCUP(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HistorialVacio() {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = Shape.Full,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Receipt, 
                    contentDescription = null, 
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Sin movimientos", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text(
            "Tus ventas aparecerán aquí una vez que comiences a operar.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HistorialCargando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = StockCubaColors.VerdeExito)
    }
}

@Composable
fun HistorialError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = StockCubaColors.CoralAlerta)
        Spacer(Modifier.height(16.dp))
        Text("No se pudo cargar el historial", style = MaterialTheme.typography.titleMedium)
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, shape = Shape.Grande) { Text("Reintentar") }
    }
}
