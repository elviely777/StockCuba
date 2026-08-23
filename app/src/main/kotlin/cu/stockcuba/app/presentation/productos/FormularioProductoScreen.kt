package cu.stockcuba.app.presentation.productos

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.UnidadMedida
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

@Composable
fun FormularioProductoScreen(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    initialModo: String = "crear",
    initialProductoId: String? = null,
    viewModel: FormularioProductoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val state = uiState) {
                        is FormularioProductoUiState.Editing -> if (state.isEditing) "Editar Producto" else "Nuevo Producto"
                        else -> "Producto"
                    }
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    if (uiState is FormularioProductoUiState.Editing) {
                        Button(onClick = { viewModel.guardar() }) { Text("Guardar") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    ) { padding ->
        LaunchedEffect(initialModo, initialProductoId) {
            if (initialModo == "editar" && initialProductoId != null) viewModel.cargarProductoParaEditar(initialProductoId)
            else if (initialModo == "crear") viewModel.resetForm()
        }

        when (val state = uiState) {
            is FormularioProductoUiState.Saving -> CargandoGuardado()
            is FormularioProductoUiState.Saved -> ExitoGuardado(onSave = onSave)
            is FormularioProductoUiState.Error -> ErrorGuardado(message = state.message, onRetry = { viewModel.resetForm() })
            is FormularioProductoUiState.Editing -> FormularioContenido(state = state, viewModel = viewModel, padding = padding)
        }
    }
}

@Composable
fun FormularioContenido(state: FormularioProductoUiState.Editing, viewModel: FormularioProductoViewModel, padding: PaddingValues) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(StockCubaSpacing.Md), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        item { TextField(value = state.nombre, onValueChange = { viewModel.updateField("nombre", it) }, label = { Text("Nombre *") }, modifier = Modifier.fillMaxWidth(), isError = state.errors["nombre"] != null, supportingText = { state.errors["nombre"]?.let { Text(it, color = MaterialTheme.colorScheme.error) } }) }
        item { TextField(value = state.descripcion, onValueChange = { viewModel.updateField("descripcion", it) }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth()) }
        
        // Unidad de Medida
        item {
            Text("Unidad de Medida", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SelectorUnidadMedida(unidadActual = state.unidadMedida, onChange = { viewModel.updateUnidadMedida(it) })
        }
        
        // Categoría
        item {
            Text("Categoría", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SelectorCategoria(categorias = state.categorias, categoriaSeleccionada = state.categoriaId, onChange = { viewModel.updateCategoria(it) }, error = state.errors["categoria"])
        }
        
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                TextField(value = state.precioVenta, onValueChange = { viewModel.updateField("precioVenta", it) }, label = { Text("Precio *") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state.errors["precioVenta"] != null, supportingText = { state.errors["precioVenta"]?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
                TextField(value = state.costoUnitario, onValueChange = { viewModel.updateField("costoUnitario", it) }, label = { Text("Costo *") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state.errors["costoUnitario"] != null, supportingText = { state.errors["costoUnitario"]?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                TextField(value = state.stockInicial, onValueChange = { viewModel.updateField("stockInicial", it) }, label = { Text("Stock Inicial *") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state.errors["stockInicial"] != null, supportingText = { state.errors["stockInicial"]?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
                TextField(value = state.stockMinimo, onValueChange = { viewModel.updateField("stockMinimo", it) }, label = { Text("Stock Mínimo *") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state.errors["stockMinimo"] != null, supportingText = { state.errors["stockMinimo"]?.let { Text(it, color = MaterialTheme.colorScheme.error) } })
            }
        }
    }
}

@Composable
fun SelectorUnidadMedida(unidadActual: UnidadMedida, onChange: (UnidadMedida) -> Unit) {
    val expanded = remember { mutableStateOf(false) }
    val unidades = UnidadMedida.values()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Xs)) {
        OutlinedButton(
            onClick = { expanded.value = !expanded.value },
            modifier = Modifier.fillMaxWidth(),
            shape = Shape.Grande,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = unidadActual.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Icon(imageVector = if (expanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        if (expanded.value) {
            DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                unidades.forEach { unidad ->
                    DropdownMenuItem(text = { Text(unidad.name) }, onClick = { onChange(unidad); expanded.value = false })
                }
            }
        }
    }
}

@Composable
fun SelectorCategoria(categorias: List<Categoria>, categoriaSeleccionada: String?, onChange: (String?) -> Unit, error: String?) {
    val expanded = remember { mutableStateOf(false) }
    val nombreActual = categorias.find { it.id == categoriaSeleccionada }?.nombre ?: "Seleccionar..."
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Xs)) {
        OutlinedButton(
            onClick = { expanded.value = !expanded.value },
            modifier = Modifier.fillMaxWidth(),
            shape = Shape.Grande,
            border = BorderStroke(1.dp, if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (error != null) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = nombreActual, style = MaterialTheme.typography.bodyLarge, color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Icon(imageVector = if (expanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
        if (expanded.value) {
            DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                DropdownMenuItem(text = { Text("Sin categoría") }, onClick = { onChange(null); expanded.value = false })
                categorias.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat.nombre) }, onClick = { onChange(cat.id); expanded.value = false })
                }
            }
        }
    }
}

@Composable
fun CargandoGuardado() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ExitoGuardado(onSave: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("¡Guardado!", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onSave) { Text("Continuar") }
    }
}

@Composable
fun ErrorGuardado(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Error: $message", color = Color.Red)
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
