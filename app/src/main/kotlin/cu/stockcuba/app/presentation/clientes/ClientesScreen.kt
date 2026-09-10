package cu.stockcuba.app.presentation.clientes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import cu.stockcuba.app.domain.model.Cliente
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val showAbonoDialog = remember { mutableStateOf(false) }
    val editingCliente = remember { mutableStateOf<Cliente?>(null) }
    val targetAbonoCliente = remember { mutableStateOf<Cliente?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Directorio de Clientes", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingCliente.value = null; showDialog.value = true },
                containerColor = StockCubaColors.VerdeExito,
                contentColor = Color(0xFF001E1C),
                shape = Shape.Grande
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Agregar", modifier = Modifier.size(28.dp))
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
                onAbono = { cliente -> targetAbonoCliente.value = cliente; showAbonoDialog.value = true },
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
            cliente = editingCliente.value,
            onDismiss = { showDialog.value = false; editingCliente.value = null },
            onSave = { nombre, ci, telefono, notas ->
                scope.launch {
                    val current = editingCliente.value
                    val result = if (current != null) {
                        viewModel.actualizarCliente(current.copy(nombre = nombre, ci = ci, telefono = telefono, notas = notas))
                    } else {
                        viewModel.crearCliente(nombre, ci, telefono, notas)
                    }
                    result.onSuccess {
                        showDialog.value = false
                        launch { snackbarHostState.showSnackbar("Cliente guardado correctamente") }
                    }.onFailure {
                        launch { snackbarHostState.showSnackbar("Error al guardar") }
                    }
                }
            }
        )
    }

    if (showAbonoDialog.value && targetAbonoCliente.value != null) {
        val cliente = targetAbonoCliente.value!!
        AbonoDeudaDialog(
            clienteNombre = cliente.nombre,
            deudaActual = cliente.saldoDeuda,
            onDismiss = { showAbonoDialog.value = false; targetAbonoCliente.value = null },
            onConfirm = { monto, metodo, notas ->
                scope.launch {
                    viewModel.registrarAbono(cliente.id, monto, metodo, notas).onSuccess {
                        showAbonoDialog.value = false
                        launch { snackbarHostState.showSnackbar("Abono registrado con éxito") }
                    }.onFailure {
                        launch { snackbarHostState.showSnackbar("Error al registrar abono") }
                    }
                }
            }
        )
    }
}

@Composable
fun ClientesContenido(
    state: ClientesUiState.Success, 
    queryText: String, 
    onQueryChange: (String) -> Unit, 
    onEditar: (Cliente) -> Unit, 
    onAbono: (Cliente) -> Unit,
    onEliminar: (String) -> Unit, 
    padding: PaddingValues
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        OutlinedTextField(
            value = queryText,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar por nombre o CI...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(StockCubaSpacing.Md),
            shape = Shape.Grande,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent
            )
        )
        
        if (state.clientes.isEmpty()) {
            ClientesVacio()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(StockCubaSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
            ) {
                items(state.clientes) { cliente ->
                    ClienteCard(
                        cliente = cliente, 
                        onEditar = onEditar, 
                        onAbono = onAbono,
                        onEliminar = { onEliminar(cliente.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ClienteCard(
    cliente: Cliente, 
    onEditar: (Cliente) -> Unit, 
    onAbono: (Cliente) -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Md)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(cliente.nombre, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("CI: ${cliente.ci}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                if (cliente.saldoDeuda > 0) {
                    Surface(
                        color = StockCubaColors.CoralAlerta.copy(alpha = 0.1f),
                        shape = Shape.Pequeno,
                        border = BorderStroke(1.dp, StockCubaColors.CoralAlerta.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Debe: ${cliente.saldoDeuda.formatoCUP()}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StockCubaColors.CoralAlerta
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!cliente.telefono.isNullOrBlank()) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(cliente.telefono, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cliente.saldoDeuda > 0) {
                        FilledTonalButton(
                            onClick = { onAbono(cliente) },
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            shape = Shape.Mediano,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = StockCubaColors.VerdeExito.copy(alpha = 0.1f), contentColor = StockCubaColors.VerdeExito)
                        ) {
                            Icon(Icons.Default.Payments, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cobrar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    
                    IconButton(onClick = { onEditar(cliente) }, modifier = Modifier.size(36.dp)) { 
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) 
                    }
                    IconButton(onClick = onEliminar, modifier = Modifier.size(36.dp)) { 
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Borrar", tint = StockCubaColors.CoralAlerta) 
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbonoDeudaDialog(
    clienteNombre: String,
    deudaActual: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, cu.stockcuba.app.domain.model.MetodoPago, String) -> Unit
) {
    var monto by remember { mutableStateOf(deudaActual.toString()) }
    var metodo by remember { mutableStateOf(cu.stockcuba.app.domain.model.MetodoPago.EFECTIVO) }
    var notas by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Pago de $clienteNombre", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                Text("Deuda pendiente: ${deudaActual.formatoCUP()}", style = MaterialTheme.typography.bodyMedium)
                
                OutlinedTextField(
                    value = monto,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) monto = it },
                    label = { Text("Monto del Pago") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("CUP") }
                )

                Text("Método de Pago", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetodoItemChip("Efectivo", metodo == cu.stockcuba.app.domain.model.MetodoPago.EFECTIVO) { 
                        metodo = cu.stockcuba.app.domain.model.MetodoPago.EFECTIVO 
                    }
                    MetodoItemChip("Transf.", metodo == cu.stockcuba.app.domain.model.MetodoPago.TRANSFERENCIA) { 
                        metodo = cu.stockcuba.app.domain.model.MetodoPago.TRANSFERENCIA 
                    }
                }
                
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val m = monto.toDoubleOrNull() ?: 0.0
                    if (m > 0) onConfirm(m, metodo, notas) 
                },
                enabled = monto.toDoubleOrNull() != null && (monto.toDoubleOrNull() ?: 0.0) > 0,
                shape = Shape.Grande
            ) {
                Text("Registrar Cobro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun RowScope.MetodoItemChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.weight(1f),
        shape = Shape.Grande
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioClienteDialog(
    cliente: Cliente?,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?) -> Unit
) {
    val nombre = remember { mutableStateOf(cliente?.nombre ?: "") }
    val ci = remember { mutableStateOf(cliente?.ci ?: "") }
    val telefono = remember { mutableStateOf(cliente?.telefono ?: "") }
    val notas = remember { mutableStateOf(cliente?.notas ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (cliente == null) "Nuevo Cliente" else "Editar Cliente", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                OutlinedTextField(
                    value = nombre.value, 
                    onValueChange = { nombre.value = it },
                    label = { Text("Nombre Completo *") },
                    shape = Shape.Grande,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ci.value, 
                    onValueChange = { if (it.length <= 11 && it.all { char -> char.isDigit() }) ci.value = it },
                    label = { Text("Carnet de Identidad *") },
                    shape = Shape.Grande,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("${ci.value.length}/11 dígitos") }
                )
                OutlinedTextField(
                    value = telefono.value, 
                    onValueChange = { telefono.value = it },
                    label = { Text("Teléfono") },
                    shape = Shape.Grande,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nombre.value, ci.value, telefono.value, notas.value) },
                enabled = nombre.value.isNotBlank() && ci.value.length == 11,
                shape = Shape.Grande
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ClientesVacio() {
    Box(modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PeopleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Text("No hay clientes registrados", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ClientesCargando() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = StockCubaColors.VerdeExito)
    }
}

@Composable
fun ClientesError(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl), contentAlignment = Alignment.Center) {
        Text(message, color = StockCubaColors.CoralAlerta, textAlign = TextAlign.Center)
    }
}
