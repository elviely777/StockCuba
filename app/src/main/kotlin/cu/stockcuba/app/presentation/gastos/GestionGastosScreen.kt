package cu.stockcuba.app.presentation.gastos

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.domain.model.Gasto
import cu.stockcuba.app.domain.model.Moneda
import cu.stockcuba.app.domain.model.TipoGasto
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionGastosScreen(
    onBack: () -> Unit,
    viewModel: GestionGastosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gastos Operativos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = StockCubaColors.VerdeExito,
                contentColor = Color(0xFF001E1C),
                shape = Shape.Grande
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Gasto")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is GestionGastosUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = StockCubaColors.VerdeExito) }
                is GestionGastosUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message) }
                is GestionGastosUiState.Success -> {
                    if (state.gastos.isEmpty()) {
                        EmptyGastosState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(StockCubaSpacing.Lg),
                            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
                        ) {
                            item {
                                ResumenGastosHoy(total = state.totalHoy)
                            }
                            item {
                                Text(
                                    "Historial de Gastos",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(state.gastos) { gasto ->
                                GastoItem(
                                    gasto = gasto,
                                    onDelete = { viewModel.eliminarGasto(gasto) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddGastoDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { concepto, monto, tipo, moneda ->
                    viewModel.registrarGasto(concepto, monto, tipo, moneda)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ResumenGastosHoy(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(StockCubaSpacing.Lg), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Gastos de Hoy", style = MaterialTheme.typography.labelMedium)
            Text(
                total.formatoCUP(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun GastoItem(gasto: Gasto, onDelete: () -> Unit) {
    var showConfirmDelete by remember { mutableStateOf(false) }

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
            Surface(
                color = getGastoColor(gasto.tipo).copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        getGastoIcon(gasto.tipo),
                        contentDescription = null,
                        tint = getGastoColor(gasto.tipo),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gasto.concepto,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${gasto.tipo.name.lowercase().replaceFirstChar { it.uppercase() }} • ${formatDate(gasto.fecha)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${gasto.monto} ${gasto.moneda}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = StockCubaColors.CoralAlerta
                )
                IconButton(onClick = { showConfirmDelete = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Eliminar Gasto") },
            text = { Text("¿Estás seguro de que deseas eliminar este registro de gasto?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirmDelete = false }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun AddGastoDialog(onDismiss: () -> Unit, onConfirm: (String, Double, TipoGasto, Moneda) -> Unit) {
    var concepto by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoGasto.OTROS) }
    var moneda by remember { mutableStateOf(Moneda.CUP) }
    var expandedTipo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Gasto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto") },
                    placeholder = { Text("Ej. Electricidad local") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = monto,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) monto = it },
                        label = { Text("Monto") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = Shape.Grande
                    )
                    
                    Box(modifier = Modifier.weight(0.8f)) {
                        OutlinedCard(
                            onClick = { /* Moneda selection logic or simple CUP for now */ },
                            modifier = Modifier.height(56.dp).padding(top = 8.dp),
                            shape = Shape.Grande
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(moneda.name)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { expandedTipo = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = Shape.Grande
                    ) {
                        Row(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tipo.name.lowercase().replaceFirstChar { it.uppercase() })
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                    DropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                        TipoGasto.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { tipo = t; expandedTipo = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val m = monto.toDoubleOrNull() ?: 0.0
                    if (concepto.isNotBlank() && m > 0) {
                        onConfirm(concepto, m, tipo, moneda)
                    }
                },
                enabled = concepto.isNotBlank() && monto.toDoubleOrNull() != null,
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
fun EmptyGastosState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Receipt, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))
        Text("No hay gastos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Toca el botón + para añadir uno", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

fun getGastoIcon(tipo: TipoGasto): ImageVector = when (tipo) {
    TipoGasto.SALARIO -> Icons.Default.Badge
    TipoGasto.SERVICIOS -> Icons.Default.Bolt
    TipoGasto.ALQUILER -> Icons.Default.HomeWork
    TipoGasto.TRANSPORTE -> Icons.Default.LocalShipping
    TipoGasto.SUMINISTROS -> Icons.Default.ShoppingBag
    TipoGasto.IMPUESTOS -> Icons.Default.AccountBalance
    TipoGasto.OTROS -> Icons.Default.MoreHoriz
}

fun getGastoColor(tipo: TipoGasto): Color = when (tipo) {
    TipoGasto.SALARIO -> Color(0xFF6366F1)
    TipoGasto.SERVICIOS -> Color(0xFFF59E0B)
    TipoGasto.ALQUILER -> Color(0xFF8B5CF6)
    TipoGasto.TRANSPORTE -> Color(0xFF10B981)
    TipoGasto.SUMINISTROS -> Color(0xFFEC4899)
    TipoGasto.IMPUESTOS -> Color(0xFFEF4444)
    TipoGasto.OTROS -> Color(0xFF64748B)
}

fun formatDate(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}
