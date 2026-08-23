package cu.stockcuba.app.presentation.ventas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

@Composable
fun NuevaVentaScreen(
    onComplete: () -> Unit,
    viewModel: NuevaVentaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val queryText = remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Venta") },
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    if (uiState is NuevaVentaUiState.Editing && (uiState as NuevaVentaUiState.Editing).carrito.isNotEmpty()) {
                        OutlinedButton(onClick = { viewModel.limpiarCarrito() }) { Text("Limpiar") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is NuevaVentaUiState.Error -> ErrorEstado(message = state.message, onRetry = { viewModel.cargarDatosIniciales() })
            is NuevaVentaUiState.Saved -> ExitoVenta(ventaId = state.ventaId, onContinue = { viewModel.resetVenta() })
            is NuevaVentaUiState.Editing -> {
                if (state.isLoading) CargandoInicial()
                else NuevaVentaContenido(
                    state = state,
                    queryText = queryText.value,
                    onQueryChange = { text -> queryText.value = text; viewModel.setQuery(text) },
                    viewModel = viewModel,
                    padding = padding
                )
            }
            else -> CargandoInicial()
        }
    }
}

@Composable
fun NuevaVentaContenido(state: NuevaVentaUiState.Editing, queryText: String, onQueryChange: (String) -> Unit, viewModel: NuevaVentaViewModel, padding: PaddingValues) {
    val productosFiltrados = state.productosDisponibles.filter { it.nombre.lowercase().contains(queryText.lowercase()) }
    val totales = CarritoTotales.calcular(state.carrito)

    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(StockCubaSpacing.Md), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        TextField(value = queryText, onValueChange = onQueryChange, label = { Text("Buscar...") }, modifier = Modifier.fillMaxWidth())

        LazyRow(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)) {
            items(productosFiltrados) { producto ->
                Card(modifier = Modifier.width(150.dp).clickable { viewModel.agregarAlCarrito(producto) }) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(producto.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        Text(producto.precioVenta.toString())
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(state.carrito) { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.producto.nombre, modifier = Modifier.weight(1f))
                    Text("${item.cantidad} x ${item.precioUnitario}")
                    IconButton(onClick = { viewModel.eliminarDelCarrito(item.producto.id) }) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red) }
                }
            }
        }

        HorizontalDivider()
        Text("Total: ${totales.total}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onClick = { viewModel.confirmarVenta() }, modifier = Modifier.fillMaxWidth(), enabled = state.carrito.isNotEmpty()) { Text("Confirmar Venta") }
    }
}

@Composable
fun CargandoInicial() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }

@Composable
fun ErrorEstado(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Error: $message", color = Color.Red)
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
fun ExitoVenta(ventaId: String, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("¡Venta registrada!", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onContinue) { Text("Nueva Venta") }
    }
}
