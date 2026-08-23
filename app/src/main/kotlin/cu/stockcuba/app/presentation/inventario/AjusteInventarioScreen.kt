package cu.stockcuba.app.presentation.inventario

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.TipoMovimientoInventario
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import kotlinx.coroutines.launch

@Composable
fun AjusteInventarioScreen(
    productoId: String,
    onComplete: () -> Unit,
    viewModel: InventarioViewModel = hiltViewModel()
) {
    val productoState = remember { mutableStateOf<Producto?>(null) }
    val tipo = remember { mutableStateOf(TipoMovimientoInventario.ENTRADA) }
    val cantidadText = remember { mutableStateOf("") }
    val motivoText = remember { mutableStateOf("") }
    val isSaving = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(productoId) {
        val state = viewModel.uiState.value
        if (state is InventarioUiState.Success) {
            productoState.value = state.productos.firstOrNull { it.producto.id == productoId }?.producto
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(productoState.value?.let { "Ajuste: ${it.nombre}" } ?: "Ajuste", overflow = TextOverflow.Ellipsis, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    ) { padding ->
        productoState.value?.let { producto ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(StockCubaSpacing.Md), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = Shape.Grande) {
                    Column(modifier = Modifier.padding(StockCubaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)) {
                        Text("Stock Actual", style = MaterialTheme.typography.labelMedium)
                        Text("${producto.stockActual} ${producto.unidadMedida.name}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)) {
                    listOf(TipoMovimientoInventario.ENTRADA, TipoMovimientoInventario.AJUSTE).forEach { t ->
                        FilterChip(
                            selected = tipo.value == t,
                            onClick = { tipo.value = t },
                            label = { Text(if (t == TipoMovimientoInventario.ENTRADA) "Entrada" else "Ajuste") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                TextField(
                    value = cantidadText.value,
                    onValueChange = { cantidadText.value = it },
                    label = { Text("Cantidad") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                TextField(
                    value = motivoText.value,
                    onValueChange = { motivoText.value = it },
                    label = { Text("Motivo (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val cant = cantidadText.value.toIntOrNull() ?: 0
                        if (cant > 0) {
                            isSaving.value = true
                            scope.launch {
                                viewModel.registrarMovimiento(producto, tipo.value, cant, motivoText.value.takeIf { it.isNotBlank() })
                                    .onSuccess { onComplete() }
                                    .onFailure { isSaving.value = false }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving.value
                ) {
                    if (isSaving.value) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Registrar")
                }
            }
        }
    }
}
