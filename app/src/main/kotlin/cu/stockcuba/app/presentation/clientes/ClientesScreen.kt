package cu.stockcuba.app.presentation.clientes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import cu.stockcuba.app.domain.model.Cliente
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(
    onBack: () -> Unit,
    viewModel: ClientesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val queryText = remember { mutableStateOf("") }
    val showDialog = remember { mutableStateOf(false) }
    val editingCliente = remember { mutableStateOf<Cliente?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clientes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingCliente.value = null; showDialog.value = true },
                containerColor = StockCubaColors.FabFondo,
                contentColor = StockCubaColors.FabTexto,
                shape = Shape.Full
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar", modifier = Modifier.size(28.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is ClientesUiState.Loading -> ClientesCargando()
            is ClientesUiState.Error -> ClientesError(message = state.message)
            is ClientesUiState.Success -> ClientesContenido(
                state = state,
                queryText = queryText.value,
                onQueryChange = { text -> queryText.value = text; viewModel.setQuery(text) },
                onEditar = { cliente -> editingCliente.value = cliente; showDialog.value = true },
                onEliminar = { clienteId ->
                    scope.launch {
                        viewModel.eliminarCliente(clienteId).onSuccess {
                            launch { snackbarHostState.showSnackbar("Cliente eliminado") }
                        }
                    }
                },
                padding = padding
            )
        }
    }

    if (showDialog.value) {
        FormularioClienteDialog(
            onDismiss = { showDialog.value = false; editingCliente.value = null },
            onSave = { nombre, telefono, notas ->
                scope.launch {
                    val cliente = editingCliente.value
                    val result = if (cliente != null) {
                        viewModel.actualizarCliente(cliente.copy(nombre = nombre, telefono = telefono, notas = notas))
                    } else {
                        viewModel.crearCliente(nombre, telefono, notas)
                    }
                    result.onSuccess {
                        showDialog.value = false
                        launch { snackbarHostState.showSnackbar("Guardado") }
                    }
                }
            },
            cliente = editingCliente.value
        )
    }
}

@Composable
fun ClientesContenido(state: ClientesUiState.Success, queryText: String, onQueryChange: (String) -> Unit, onEditar: (Cliente) -> Unit, onEliminar: (String) -> Unit, padding: PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        TextField(
            value = queryText,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar...") },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = TextFieldDefaults.colors()
        )
        if (state.clientes.isEmpty()) ClientesVacio() else {
            LazyColumn { items(state.clientes) { cliente -> ClienteRow(cliente, onEditar, { onEliminar(cliente.id) }) } }
        }
    }
}

@Composable
fun ClienteRow(cliente: Cliente, onEditar: (Cliente) -> Unit, onEliminar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(cliente.nombre, fontWeight = FontWeight.Bold); cliente.telefono?.let { Text(it) } }
            Row {
                IconButton(onClick = { onEditar(cliente) }) { Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar") }
                IconButton(onClick = onEliminar) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Borrar", tint = Color.Red) }
            }
        }
    }
}

@Composable
fun FormularioClienteDialog(onDismiss: () -> Unit, onSave: (String, String?, String?) -> Unit, cliente: Cliente?) {
    val nombre = remember { mutableStateOf(cliente?.nombre ?: "") }
    val telefono = remember { mutableStateOf(cliente?.telefono ?: "") }
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = { onSave(nombre.value, telefono.value, null) }) { Text("OK") } }, title = { Text("Cliente") }, text = { Column { TextField(nombre.value, { nombre.value = it }) } })
}

@Composable
fun ClientesVacio() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Vacío") } }
@Composable
fun ClientesCargando() { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
@Composable
fun ClientesError(message: String) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message, color = Color.Red) } }
