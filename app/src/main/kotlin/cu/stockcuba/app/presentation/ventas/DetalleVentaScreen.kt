package cu.stockcuba.app.presentation.ventas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.VentaItem
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleVentaScreen(
    ventaId: String,
    onBack: () -> Unit,
    viewModel: DetalleVentaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(ventaId) {
        viewModel.cargarVenta(ventaId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumen de Venta", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is DetalleVentaUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is DetalleVentaUiState.Error -> PantallaErrorVenta(state.message, onRetry = { viewModel.cargarVenta(ventaId) })
                is DetalleVentaUiState.Success -> DetalleVentaContenido(state.venta, state.cliente)
            }
        }
    }
}

@Composable
fun DetalleVentaContenido(venta: Venta, cliente: cu.stockcuba.app.domain.model.Cliente?) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy").withZone(ZoneId.systemDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault()) }
    
    val fechaStr = remember(venta.fecha) { dateFormatter.format(venta.fecha) }
    val horaStr = remember(venta.fecha) { timeFormatter.format(venta.fecha) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(StockCubaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
    ) {
        // --- 1. CABECERA DE TICKET ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Venta #${venta.id.take(8).uppercase()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("$fechaStr • $horaStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // --- 2. INFORMACIÓN DEL CLIENTE ---
        item {
            SeccionDetalle(titulo = "Cliente", icono = Icons.Default.Person) {
                if (cliente != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text(cliente.nombre, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            Text("CI: ${cliente.ci}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!cliente.telefono.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(cliente.telefono, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    Text("Consumidor Final", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                }
            }
        }

        // --- 3. PRODUCTOS ---
        item {
            SeccionDetalle(titulo = "Productos", icono = Icons.Default.Inventory2) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    venta.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.nombreProducto, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                Text("${item.cantidad} x ${item.precioUnitario.formatoCUP()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(item.subtotal.formatoCUP(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                        Text(venta.total.formatoCUP(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = StockCubaColors.VerdeExito))
                    }
                }
            }
        }

        // --- 4. DETALLES DE PAGO ---
        item {
            SeccionDetalle(titulo = "Método de Pago", icono = Icons.Default.Payments) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape).background(
                                when(venta.metodoPago) {
                                    MetodoPago.EFECTIVO -> Color(0xFF2DD4BF)
                                    MetodoPago.TRANSFERENCIA -> Color(0xFF6366F1)
                                    MetodoPago.MIXTO -> Color(0xFFF59E0B)
                                }
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = when(venta.metodoPago) {
                                MetodoPago.EFECTIVO -> "Efectivo"
                                MetodoPago.TRANSFERENCIA -> "Transferencia"
                                MetodoPago.MIXTO -> "Pago Mixto"
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    
                    if (venta.metodoPago == MetodoPago.MIXTO) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = Shape.Mediano,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(StockCubaSpacing.Md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Efectivo:", style = MaterialTheme.typography.bodySmall)
                                    Text(venta.montoEfectivo.formatoCUP(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Transferencia:", style = MaterialTheme.typography.bodySmall)
                                    Text(venta.montoTransferencia.formatoCUP(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    } else if (venta.metodoPago == MetodoPago.EFECTIVO) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Monto recibido: ${venta.montoEfectivo.formatoCUP()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (venta.montoEfectivo > venta.total) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                    shape = Shape.Pequeno,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        "Cambio devuelto: ${(venta.montoEfectivo - venta.total).formatoCUP()}", 
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun SeccionDetalle(
    titulo: String,
    icono: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(titulo, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp))
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}
