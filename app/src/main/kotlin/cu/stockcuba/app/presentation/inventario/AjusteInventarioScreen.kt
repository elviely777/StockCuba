package cu.stockcuba.app.presentation.inventario

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Note
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
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.TipoMovimientoInventario
import cu.stockcuba.app.presentation.dashboard.formatoCantidad
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjusteInventarioScreen(
    productoId: String,
    onComplete: () -> Unit,
    viewModel: InventarioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val producto = (uiState as? InventarioUiState.Success)?.productos?.find { it.producto.id == productoId }?.producto
    
    var selectedOption by remember { mutableStateOf(AjusteUIOption.ENTRADA) }
    var cantidadText by remember { mutableStateOf("") }
    var motivoText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorCantidad by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Movimiento Manual", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (producto != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val cant = cantidadText.toIntOrNull() ?: 0
                        if (cant <= 0) {
                            errorCantidad = "Ingrese una cantidad"
                            return@ExtendedFloatingActionButton
                        }
                        
                        isSaving = true
                        scope.launch {
                            val tipoReal = when(selectedOption) {
                                AjusteUIOption.ENTRADA -> TipoMovimientoInventario.ENTRADA
                                AjusteUIOption.SALIDA -> TipoMovimientoInventario.SALIDA
                                AjusteUIOption.CONTEO -> TipoMovimientoInventario.AJUSTE
                            }
                            
                            viewModel.registrarMovimiento(
                                producto = producto, 
                                tipo = tipoReal, 
                                cantidad = cant, 
                                motivo = motivoText.takeIf { it.isNotBlank() } ?: selectedOption.label
                            ).onSuccess { 
                                onComplete() 
                            }.onFailure { 
                                isSaving = false 
                            }
                        }
                    },
                    containerColor = selectedOption.color,
                    contentColor = if (selectedOption.color == StockCubaColors.VerdeExito) Color(0xFF001E1C) else Color.White,
                    icon = { 
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
                        } else {
                            Icon(selectedOption.icon, null) 
                        }
                    },
                    text = { Text("Registrar ${selectedOption.label}", fontWeight = FontWeight.Bold) },
                    expanded = !isSaving,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        if (producto == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StockCubaColors.VerdeExito)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(StockCubaSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
            ) {
                // --- 1. PRODUCTO SELECCIONADO ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shape.Grande,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(StockCubaSpacing.Lg), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape) {
                                Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(producto.nombre, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    "Existencia actual: ${producto.stockActual.formatoCantidad()} ${producto.unidadMedida.name.lowercase()}", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // --- 2. SELECTOR DE TIPO ---
                item {
                    Text("¿Qué desea registrar?", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)) {
                        AjusteUIOption.entries.forEach { option ->
                            OptionSelectorCard(
                                option = option,
                                isSelected = selectedOption == option,
                                onClick = { selectedOption = option }
                            )
                        }
                    }
                }

                // --- 3. DATOS ---
                item {
                    Text("Detalles", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cantidadText,
                        onValueChange = { 
                            if (it.all { c -> c.isDigit() }) {
                                cantidadText = it
                                errorCantidad = null
                            }
                        },
                        label = { Text("Cantidad de artículos") },
                        placeholder = { Text("Ej. 10") },
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                        suffix = { Text(producto.unidadMedida.name.lowercase()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shape.Grande,
                        isError = errorCantidad != null,
                        supportingText = { errorCantidad?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = selectedOption.color,
                            focusedLabelColor = selectedOption.color
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = motivoText,
                        onValueChange = { motivoText = it },
                        label = { Text("Nota (Opcional)") },
                        placeholder = { Text(when(selectedOption) {
                            AjusteUIOption.ENTRADA -> "Ej. Compra de suministros..."
                            AjusteUIOption.SALIDA -> "Ej. Se rompió una unidad..."
                            AjusteUIOption.CONTEO -> "Ej. Corrección por inventario..."
                        }) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shape.Grande,
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = selectedOption.color,
                            focusedLabelColor = selectedOption.color
                        )
                    )
                }

                // --- 4. VISTA PREVIA ---
                item {
                    val cant = cantidadText.toIntOrNull() ?: 0
                    if (cant > 0) {
                        val nuevoStock = when(selectedOption) {
                            AjusteUIOption.ENTRADA -> producto.stockActual + cant
                            AjusteUIOption.SALIDA -> producto.stockActual - cant
                            AjusteUIOption.CONTEO -> producto.stockActual + cant
                        }
                        
                        Surface(
                            color = selectedOption.color.copy(alpha = 0.05f),
                            shape = Shape.Grande,
                            border = BorderStroke(1.dp, selectedOption.color.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(StockCubaSpacing.Md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Resultado: ", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${producto.stockActual}", 
                                    style = MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp).size(16.dp))
                                Text(
                                    "$nuevoStock ${producto.unidadMedida.name.lowercase()}", 
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = selectedOption.color
                                )
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun OptionSelectorCard(
    option: AjusteUIOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) option.color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) option.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(StockCubaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) option.color else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon, 
                    contentDescription = null, 
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = option.label, 
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) option.color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = option.description, 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = option.color)
            }
        }
    }
}

enum class AjusteUIOption(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
) {
    ENTRADA(
        "Entrada (Compra)",
        "Aumenta la existencia del producto",
        Icons.Default.AddBusiness,
        Color(0xFF2DD4BF) // Teal
    ),
    SALIDA(
        "Salida (Baja/Merma)",
        "Disminuye la existencia del producto",
        Icons.Default.RemoveCircleOutline,
        Color(0xFFFB7185) // Coral
    ),
    CONTEO(
        "Auditoría (Ajuste)",
        "Corrige descuadres encontrados",
        Icons.Default.FactCheck,
        Color(0xFF6366F1) // Indigo
    )
}
