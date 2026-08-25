package cu.stockcuba.app.presentation.ventas

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import cu.stockcuba.app.presentation.dashboard.formatoCantidad
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaVentaScreen(
    onComplete: () -> Unit,
    viewModel: NuevaVentaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Punto de Venta", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) 
                },
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                actions = {
                    if (uiState is NuevaVentaUiState.Editing) {
                        val state = uiState as NuevaVentaUiState.Editing
                        if (state.carrito.isNotEmpty()) {
                            TextButton(onClick = { viewModel.limpiarCarrito() }) {
                                Text("Vaciar", color = StockCubaColors.CoralAlerta)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is NuevaVentaUiState.Editing) {
                val state = uiState as NuevaVentaUiState.Editing
                val totales = CarritoTotales.calcular(state.carrito)
                
                if (state.carrito.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.confirmarVenta() },
                        containerColor = StockCubaColors.VerdeExito,
                        contentColor = Color(0xFF001E1C),
                        shape = Shape.Grande,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF001E1C), strokeWidth = 3.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Vender • ${totales.total.formatoCUP()}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is NuevaVentaUiState.Saving -> PantallaCargandoVenta("Registrando venta...")
                is NuevaVentaUiState.Error -> PantallaErrorVenta(state.message, onRetry = { viewModel.cargarDatosIniciales() })
                is NuevaVentaUiState.Saved -> PantallaExitoVenta(onContinue = onComplete)
                is NuevaVentaUiState.Editing -> {
                    if (state.isLoading && state.productosDisponibles.isEmpty()) {
                        PantallaCargandoVenta("Cargando catálogo...")
                    } else {
                        NuevaVentaContenidoModerno(
                            state = state,
                            viewModel = viewModel
                        )
                    }

                    // --- DIÁLOGO FORMULARIO CLIENTE (T55) ---
                    if (state.showNuevoClienteDialog) {
                        NuevoClienteDialog(
                            isEditing = state.editingClienteId != null,
                            nombre = state.nuevoClienteNombre,
                            ci = state.nuevoClienteCI,
                            telefono = state.nuevoClienteTelefono,
                            onNombreChange = { viewModel.updateNuevoClienteNombre(it) },
                            onCIChange = { viewModel.updateNuevoClienteCI(it) },
                            onTelefonoChange = { viewModel.updateNuevoClienteTelefono(it) },
                            onDismiss = { viewModel.setShowNuevoClienteDialog(false) },
                            onConfirm = { viewModel.guardarNuevoCliente() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NuevaVentaContenidoModerno(
    state: NuevaVentaUiState.Editing,
    viewModel: NuevaVentaViewModel
) {
    val productosFiltrados = state.productosDisponibles.filter { 
        it.nombre.lowercase().contains(state.query.lowercase()) || 
        it.descripcion?.lowercase()?.contains(state.query.lowercase()) == true 
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(StockCubaSpacing.Lg),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
    ) {
        // --- 1. BUSCADOR ---
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.setQuery(it) },
                placeholder = { Text("Buscar producto...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = Shape.Grande,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }

        // --- 2. SELECTOR DE PRODUCTOS (Horizontal) ---
        item {
            Text("Catálogo", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
            ) {
                items(productosFiltrados) { producto ->
                    ProductoVentaCard(
                        producto = producto,
                        onClick = { viewModel.agregarAlCarrito(producto) }
                    )
                }
                if (productosFiltrados.isEmpty()) {
                    item {
                        Text("No hay productos disponibles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // --- 3. EL CARRITO ---
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Detalle de Venta", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = Shape.Full
                ) {
                    Text(
                        "${state.carrito.sumOf { it.cantidad }} items",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (state.carrito.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Selecciona productos arriba para comenzar", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.carrito) { item ->
                ItemCarritoCard(
                    item = item,
                    onIncrease = { viewModel.incrementarCantidad(item.producto.id) },
                    onDecrease = { viewModel.decrementarCantidad(item.producto.id) },
                    onRemove = { viewModel.eliminarDelCarrito(item.producto.id) }
                )
            }
        }

        // --- 4. CLIENTE ---
        if (state.carrito.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cliente", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.height(8.dp))
                SelectorClienteModerno(
                    clientes = state.clientes,
                    clienteSeleccionadoId = state.clienteId,
                    onSelect = { viewModel.setCliente(it) },
                    onNuevoClienteClick = { viewModel.setShowNuevoClienteDialog(true) },
                    onEditClienteClick = { viewModel.startEditingCliente(it) }
                )
            }

            // --- 5. MÉTODO DE PAGO ---
            item {
                Text("Método de Pago", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Xs)
                ) {
                    MetodoPagoChip(
                        label = "Efectivo",
                        icon = Icons.Default.Payments,
                        isSelected = state.metodoPago == MetodoPago.EFECTIVO,
                        onClick = { viewModel.setMetodoPago(MetodoPago.EFECTIVO) },
                        modifier = Modifier.weight(1f)
                    )
                    MetodoPagoChip(
                        label = "Transferencia",
                        icon = Icons.Default.AccountBalance,
                        isSelected = state.metodoPago == MetodoPago.TRANSFERENCIA,
                        onClick = { viewModel.setMetodoPago(MetodoPago.TRANSFERENCIA) },
                        modifier = Modifier.weight(1f)
                    )
                    MetodoPagoChip(
                        label = "Mixto",
                        icon = Icons.Default.SwapHoriz,
                        isSelected = state.metodoPago == MetodoPago.MIXTO,
                        onClick = { viewModel.setMetodoPago(MetodoPago.MIXTO) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 6. ENTRADA DE MONTOS SEGÚN PAGO ---
            item {
                val totales = CarritoTotales.calcular(state.carrito)
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = Shape.Grande
                ) {
                    Column(modifier = Modifier.padding(StockCubaSpacing.Md), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                        
                        if (state.metodoPago == MetodoPago.EFECTIVO) {
                            val efectivo = state.efectivoRecibido.toDoubleOrNull() ?: 0.0
                            val cambio = (efectivo - totales.total).coerceAtLeast(0.0)
                            
                            InputMonto(
                                label = "Efectivo Recibido",
                                value = state.efectivoRecibido,
                                onValueChange = { viewModel.setEfectivoRecibido(it) },
                                onExactClick = { viewModel.setMontoExacto() },
                                error = state.errors["efectivo"]
                            )
                            
                            if (efectivo > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cambio a devolver:", style = MaterialTheme.typography.bodyMedium)
                                    Text(cambio.formatoCUP(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        
                        if (state.metodoPago == MetodoPago.TRANSFERENCIA) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Confirma la recepción de ${totales.total.formatoCUP()} en tu cuenta.", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            OutlinedTextField(
                                value = state.idTransferencia,
                                onValueChange = { viewModel.setIdTransferencia(it) },
                                label = { Text("ID de Transacción / Comprobante") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = Shape.Grande,
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(20.dp)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        
                        if (state.metodoPago == MetodoPago.MIXTO) {
                            Text("Reparto de montos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            
                            InputMonto(
                                label = "Monto en Efectivo",
                                value = state.efectivoRecibido,
                                onValueChange = { viewModel.setEfectivoRecibido(it) },
                                error = state.errors["efectivo"]
                            )
                            
                            InputMonto(
                                label = "Monto por Transferencia",
                                value = state.transferenciaMonto,
                                onValueChange = { viewModel.setTransferenciaMonto(it) },
                                error = state.errors["transferencia"]
                            )

                            OutlinedTextField(
                                value = state.idTransferencia,
                                onValueChange = { viewModel.setIdTransferencia(it) },
                                label = { Text("ID de Transacción (Transferencia)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = Shape.Grande,
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.ConfirmationNumber, null, modifier = Modifier.size(20.dp)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            )
                            
                            val efectivo = state.efectivoRecibido.toDoubleOrNull() ?: 0.0
                            val transferencia = state.transferenciaMonto.toDoubleOrNull() ?: 0.0
                            val suma = efectivo + transferencia
                            val resta = totales.total - suma
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Suma total:", style = MaterialTheme.typography.bodySmall)
                                Text(suma.formatoCUP(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            
                            if (resta != 0.0) {
                                Text(
                                    text = if (resta > 0) "Faltan ${resta.formatoCUP()}" else "Sobran ${(-resta).formatoCUP()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (resta > 0) StockCubaColors.CoralAlerta else MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            if (state.errors["pagoMixto"] != null) {
                                Text(state.errors["pagoMixto"]!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

@Composable
fun ProductoVentaCard(producto: Producto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(StockCubaSpacing.Md),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                producto.nombre,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Column {
                Text(
                    producto.precioVenta.formatoCUP(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Stock: ${producto.stockActual}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ItemCarritoCard(
    item: CarritoItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(StockCubaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.producto.nombre, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text(item.precioUnitario.formatoCUP(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            ) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp))
                }
                Text(
                    item.cantidad.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp), enabled = item.puedeAumentar) {
                    Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp))
                }
            }
            
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = StockCubaColors.CoralAlerta)
            }
        }
    }
}

@Composable
fun MetodoPagoChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = Shape.Grande,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorClienteModerno(
    clientes: List<ClienteSimple>,
    clienteSeleccionadoId: String?,
    onSelect: (String?) -> Unit,
    onNuevoClienteClick: () -> Unit,
    onEditClienteClick: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val clienteActual = clientes.find { it.id == clienteSeleccionadoId }?.nombre ?: "Consumidor Final"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { showSheet = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = Shape.Grande,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (clienteSeleccionadoId == null) Icons.Default.PersonOutline else Icons.Default.Person,
                        contentDescription = null, 
                        tint = if (clienteSeleccionadoId == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = clienteActual, 
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (clienteSeleccionadoId == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                ClientePickerContent(
                    clientes = clientes,
                    onSelect = { 
                        onSelect(it)
                        showSheet = false
                    },
                    onNuevoClienteClick = {
                        showSheet = false
                        onNuevoClienteClick()
                    },
                    onEditClick = {
                        showSheet = false
                        onEditClienteClick(it)
                    }
                )
            }
        }
    }
}

@Composable
fun ClientePickerContent(
    clientes: List<ClienteSimple>,
    onSelect: (String?) -> Unit,
    onNuevoClienteClick: () -> Unit,
    onEditClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredClientes = remember(searchQuery, clientes) {
        clientes.filter { 
            it.nombre.contains(searchQuery, ignoreCase = true) || 
            (it.telefono?.contains(searchQuery) == true) 
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(horizontal = StockCubaSpacing.Lg)
    ) {
        Text(
            "Seleccionar Cliente", 
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = StockCubaSpacing.Md)
        )

        // Buscador interno del Picker
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar por nombre o teléfono...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = Shape.Grande,
            singleLine = true,
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null) } }
            } else null
        )

        Spacer(Modifier.height(StockCubaSpacing.Md))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
        ) {
            // Opción fija: Consumidor Final
            item {
                ListOptionItem(
                    title = "Consumidor Final",
                    icon = Icons.Default.PersonOutline,
                    onClick = { onSelect(null) }
                )
            }

            // Opción fija: Nuevo Cliente
            item {
                ListOptionItem(
                    title = "Agregar Nuevo Cliente",
                    icon = Icons.Default.PersonAdd,
                    iconColor = StockCubaColors.VerdeExito,
                    onClick = onNuevoClienteClick
                )
            }

            if (filteredClientes.isNotEmpty()) {
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                
                items(filteredClientes) { cliente ->
                    ListOptionItem(
                        title = cliente.nombre,
                        subtitle = cliente.telefono,
                        icon = Icons.Default.Person,
                        onClick = { onSelect(cliente.id) },
                        trailingContent = {
                            IconButton(onClick = { onEditClick(cliente.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit, 
                                    contentDescription = "Editar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )
                }
            } else if (searchQuery.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(StockCubaSpacing.Xl), contentAlignment = Alignment.Center) {
                        Text("No se encontraron clientes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
fun ListOptionItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

@Composable
fun NuevoClienteDialog(
    isEditing: Boolean = false,
    nombre: String,
    ci: String,
    telefono: String,
    onNombreChange: (String) -> Unit,
    onCIChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar Cliente" else "Nuevo Cliente", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre Completo *") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande,
                    singleLine = true
                )
                OutlinedTextField(
                    value = ci,
                    onValueChange = { if (it.length <= 11 && it.all { char -> char.isDigit() }) onCIChange(it) },
                    label = { Text("Carnet de Identidad *") },
                    leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("${ci.length}/11 dígitos", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) }
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = onTelefonoChange,
                    label = { Text("Teléfono (Opcional)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = nombre.isNotBlank() && ci.length == 11,
                shape = Shape.Grande
            ) {
                Text(if (isEditing) "Guardar Cambios" else "Registrar y Seleccionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun InputMonto(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onExactClick: (() -> Unit)? = null,
    error: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { char -> it.isEmpty() || char.isDigit() || char == '.' }) onValueChange(it) },
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
        trailingIcon = if (onExactClick != null) {
            {
                TextButton(onClick = onExactClick) {
                    Text("EXACTO")
                }
            }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun PantallaCargandoVenta(mensaje: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = StockCubaColors.VerdeExito)
            Spacer(Modifier.height(16.dp))
            Text(mensaje)
        }
    }
}

@Composable
fun PantallaExitoVenta(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(100.dp), tint = StockCubaColors.VerdeExito)
        Spacer(Modifier.height(24.dp))
        Text("¡Venta Exitosa!", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Text("La venta ha sido registrada y el inventario actualizado.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), shape = Shape.Grande) {
            Text("Continuar")
        }
    }
}

@Composable
fun PantallaErrorVenta(mensaje: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(64.dp), tint = StockCubaColors.CoralAlerta)
        Spacer(Modifier.height(16.dp))
        Text("Error en la venta", style = MaterialTheme.typography.titleLarge)
        Text(mensaje, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, shape = Shape.Grande) { Text("Reintentar") }
    }
}
