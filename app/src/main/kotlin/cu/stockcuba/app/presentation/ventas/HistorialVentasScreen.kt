package cu.stockcuba.app.presentation.ventas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

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
                title = { Text("Ventas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refrescar() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is HistorialVentasUiState.Loading -> HistorialCargando()
            is HistorialVentasUiState.Error -> HistorialError(message = state.message, onRetry = { viewModel.refrescar() })
            is HistorialVentasUiState.Success -> HistorialContenido(ventasPorDia = state.ventasPorDia, onDetalle = onDetalle, padding = padding)
        }
    }
}

@Composable
fun HistorialContenido(ventasPorDia: List<VentasPorDia>, onDetalle: (String) -> Unit, padding: PaddingValues) {
    if (ventasPorDia.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { HistorialVacio() }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(StockCubaSpacing.Md), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)) {
            items(ventasPorDia) { dia -> DiaVentasSection(dia = dia, onDetalle = onDetalle) }
        }
    }
}

@Composable
fun DiaVentasSection(dia: VentasPorDia, onDetalle: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = Shape.Grande, border = BorderStroke(1.dp, StockCubaColors.BordeSutil)) {
        Column {
            Text(dia.fechaFormateada, modifier = Modifier.padding(StockCubaSpacing.Md), fontWeight = FontWeight.Bold)
            HorizontalDivider()
            dia.ventas.forEach { venta -> VentaRow(venta = venta, onClick = { onDetalle(venta.id) }) }
        }
    }
}

@Composable
fun VentaRow(venta: Venta, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(StockCubaSpacing.Sm)) {
        Row(modifier = Modifier.padding(StockCubaSpacing.Md), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("#${venta.id.take(8)}")
            Text(venta.total.toString(), fontWeight = FontWeight.Bold, color = Color.Green)
        }
    }
}

@Composable
fun HistorialVacio() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No hay ventas", textAlign = TextAlign.Center)
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
        androidx.compose.material3.Button(onClick = onRetry) { Text("Reintentar") }
    }
}
