package cu.stockcuba.app.presentation.inventario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import cu.stockcuba.app.domain.model.MovimientoInventario
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.TipoMovimientoInventario
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

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
                    val title = (uiState as? HistorialMovimientosUiState.Success)?.producto?.nombre ?: "Historial"
                    Text(title, overflow = TextOverflow.Ellipsis, maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refrescar(productoId) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    ) { padding ->
        when (uiState) {
            is HistorialMovimientosUiState.Loading -> HistorialCargando()
            is HistorialMovimientosUiState.Error -> HistorialError(message = (uiState as HistorialMovimientosUiState.Error).message, onRetry = { viewModel.refrescar(productoId) })
            is HistorialMovimientosUiState.Success -> HistorialContenido(state = uiState as HistorialMovimientosUiState.Success, padding = padding)
        }
    }
}

@Composable
fun HistorialContenido(state: HistorialMovimientosUiState.Success, padding: PaddingValues) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(StockCubaSpacing.Md), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        state.producto?.let { item { ProductoHistorialHeader(producto = it) } }
        items(state.movimientos) { MovimientoRow(movimiento = it) }
        if (state.movimientos.isEmpty()) item { HistorialVacio() }
    }
}

@Composable
fun ProductoHistorialHeader(producto: Producto) {
    Card(modifier = Modifier.fillMaxWidth(), shape = Shape.Grande, border = BorderStroke(1.dp, StockCubaColors.BordeSutil)) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)) {
            Text(producto.nombre, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text("Stock: ${producto.stockActual}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MovimientoRow(movimiento: MovimientoInventario) {
    Card(modifier = Modifier.fillMaxWidth(), shape = Shape.Grande, border = BorderStroke(1.dp, StockCubaColors.BordeSutil)) {
        Row(modifier = Modifier.padding(StockCubaSpacing.Md), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(movimiento.tipo.name, fontWeight = FontWeight.Bold)
                Text("Cantidad: ${movimiento.cantidad}", style = MaterialTheme.typography.bodySmall)
            }
            Text(movimiento.fecha.toString(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun HistorialVacio() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Sin movimientos", textAlign = TextAlign.Center)
    }
}

@Composable
fun HistorialCargando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun HistorialError(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Error: $message", color = Color.Red)
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
