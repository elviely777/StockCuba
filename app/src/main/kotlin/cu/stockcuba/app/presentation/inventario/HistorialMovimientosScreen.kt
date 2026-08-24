package cu.stockcuba.app.presentation.inventario

import androidx.compose.animation.*
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
import cu.stockcuba.app.domain.model.MovimientoInventario
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.TipoMovimientoInventario
import cu.stockcuba.app.presentation.dashboard.formatoCantidad
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialMovimientosScreen(
    productoId: String,
    onBack: () -> Unit,
    viewModel: HistorialMovimientosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productoId) {
        viewModel.cargarHistorial(productoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kardex de Producto", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        (uiState as? HistorialMovimientosUiState.Success)?.producto?.let {
                            Text(it.nombre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refrescar(productoId) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            when (val state = uiState) {
                is HistorialMovimientosUiState.Loading -> HistorialCargando()
                is HistorialMovimientosUiState.Error -> HistorialError(message = state.message, onRetry = { viewModel.refrescar(productoId) })
                is HistorialMovimientosUiState.Success -> HistorialMovimientosContenidoModerno(state = state)
            }
        }
    }
}

@Composable
fun HistorialMovimientosContenidoModerno(state: HistorialMovimientosUiState.Success) {
    val movimientosAgrupados = remember(state.movimientos) {
        state.movimientos.groupBy { 
            it.fecha.atZone(ZoneId.systemDefault()).toLocalDate()
        }.toList().sortedByDescending { it.first }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(StockCubaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
    ) {
        // --- RESUMEN DEL PRODUCTO ---
        state.producto?.let {
            item {
                ProductoStatusCard(producto = it)
            }
        }

        if (movimientosAgrupados.isEmpty()) {
            item { HistorialVacioModerno() }
        } else {
            movimientosAgrupados.forEach { (fecha, movimientos) ->
                item {
                    val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM")
                    Text(
                        text = fecha.format(formatter).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                items(movimientos) { mov ->
                    MovimientoCardModerno(
                        movimiento = mov, 
                        unidad = state.producto?.unidadMedida?.name?.lowercase() ?: ""
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun ProductoStatusCard(producto: Producto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(StockCubaSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Existencia en Almacén", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "${producto.stockActual.formatoCantidad()} ${producto.unidadMedida.name.lowercase()}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}

@Composable
fun MovimientoCardModerno(movimiento: MovimientoInventario, unidad: String) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault()) }
    val hora = remember(movimiento.fecha) { timeFormatter.format(movimiento.fecha) }
    
    val color = when(movimiento.tipo) {
        TipoMovimientoInventario.ENTRADA -> StockCubaColors.VerdeExito
        TipoMovimientoInventario.VENTA, TipoMovimientoInventario.SALIDA -> StockCubaColors.CoralAlerta
        TipoMovimientoInventario.AJUSTE -> Color(0xFF6366F1) // Indigo
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(StockCubaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de dirección
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(movimiento.tipo) {
                        TipoMovimientoInventario.ENTRADA -> Icons.Default.Add
                        TipoMovimientoInventario.VENTA -> Icons.Default.ShoppingBag
                        TipoMovimientoInventario.SALIDA -> Icons.Default.Remove
                        TipoMovimientoInventario.AJUSTE -> Icons.Default.Tune
                    },
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when(movimiento.tipo) {
                            TipoMovimientoInventario.ENTRADA -> "Entrada de Almacén"
                            TipoMovimientoInventario.VENTA -> "Venta Realizada"
                            TipoMovimientoInventario.SALIDA -> "Salida / Merma"
                            TipoMovimientoInventario.AJUSTE -> "Ajuste de Auditoría"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("• $hora", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                movimiento.motivo?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (movimiento.tipo == TipoMovimientoInventario.VENTA) FontWeight.Medium else FontWeight.Normal),
                        color = if (movimiento.tipo == TipoMovimientoInventario.VENTA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (movimiento.esEntrada) "+" else "-"}${movimiento.cantidad}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = color
                )
                Text(
                    text = unidad,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HistorialVacioModerno() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))
        Text("No hay movimientos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Column(modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = StockCubaColors.CoralAlerta)
        Spacer(Modifier.height(16.dp))
        Text("Error al cargar historial", style = MaterialTheme.typography.titleMedium)
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, shape = Shape.Grande) { Text("Reintentar") }
    }
}
