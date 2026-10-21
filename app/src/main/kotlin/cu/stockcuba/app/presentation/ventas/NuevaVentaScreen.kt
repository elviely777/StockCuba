package cu.stockcuba.app.presentation.ventas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.Moneda
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.presentation.common.*
import cu.stockcuba.app.presentation.dashboard.formatoAuto
import cu.stockcuba.app.presentation.dashboard.formatoMoneda
import cu.stockcuba.app.presentation.scanner.ScannerScreen
import cu.stockcuba.app.presentation.theme.ScColors
import cu.stockcuba.app.presentation.theme.Shape
import java.util.Locale

/**
 * Punto de venta · "Mostrador".
 *
 * La pantalla donde el dueño pasa más tiempo. Reglas de la retícula:
 *  - Buscador fijo arriba: nunca hay que subir para escribir.
 *  - Catálogo y carrito son paneles del sistema (borde de 1px, capa tonal).
 *  - Filas de carrito compactas: miniatura, nombre, precio unitario y stepper
 *    de 44dp; con cantidad 1 el "−" se vuelve papelera.
 *  - Barra inferior fija: total grande en ámbar + "Cobrar" (respeta teclado y
 *    barra de navegación con imePadding + navigationBarsPadding).
 *  - El único momento audaz de la pantalla es ese total ámbar.
 *
 * Toda la lógica vive en [NuevaVentaViewModel]: aquí solo se recompone la UI.
 */
@Composable
fun NuevaVentaScreen(
    ventaIdParaEditar: String? = null,
    onComplete: () -> Unit,
    viewModel: NuevaVentaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showScanner by remember { mutableStateOf(false) }

    LaunchedEffect(ventaIdParaEditar) {
        if (ventaIdParaEditar != null) {
            viewModel.cargarVentaParaEditar(ventaIdParaEditar)
        }
    }

    if (showScanner) {
        ScannerScreen(
            onBarcodeScanned = { code ->
                viewModel.buscarPorCodigoBarras(code)
                showScanner = false
            },
            onClose = { showScanner = false }
        )
        return
    }

    Scaffold(
        containerColor = ScColors.canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val editing = uiState as? NuevaVentaUiState.Editing
            if (editing != null && (editing.carrito.isNotEmpty() || editing.productosDisponibles.isNotEmpty())) {
                BarraCobro(
                    state = editing,
                    totales = viewModel.calcularTotales(editing),
                    onCobrar = { viewModel.confirmarVenta() }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is NuevaVentaUiState.Saving -> PantallaCargandoVenta("Registrando venta...")
                is NuevaVentaUiState.Error -> PantallaErrorVenta(state.message, onRetry = { viewModel.cargarDatosIniciales() })
                is NuevaVentaUiState.Saved -> PantallaExitoVenta(
                    ventaId = state.ventaId,
                    onContinue = onComplete,
                    onShareTicket = { id ->
                        scope.launch {
                            val uri = viewModel.generarYCompartirTicket(id)
                            if (uri != null) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Compartir Recibo"))
                            }
                        }
                    },
                    onPrintTicket = { id ->
                        scope.launch {
                            viewModel.imprimirTicket(id).onFailure { error ->
                                scope.launch { snackbarHostState.showSnackbar("Error de impresión: $error") }
                            }.onSuccess {
                                scope.launch { snackbarHostState.showSnackbar("Enviado a impresora") }
                            }
                        }
                    }
                )
                is NuevaVentaUiState.Editing -> {
                    if (state.isLoading && state.productosDisponibles.isEmpty()) {
                        PantallaCargandoVenta("Cargando catálogo...")
                    } else {
                        NuevaVentaContenido(
                            state = state,
                            viewModel = viewModel,
                            onShowScanner = { showScanner = true },
                            onBack = onComplete
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

// =====================================================================================
// CONTENIDO
// =====================================================================================

@Composable
private fun NuevaVentaContenido(
    state: NuevaVentaUiState.Editing,
    viewModel: NuevaVentaViewModel,
    onShowScanner: () -> Unit,
    onBack: () -> Unit
) {
    val productosFiltrados = state.productosDisponibles.filter {
        (it.nombre.lowercase().contains(state.query.lowercase()) ||
            it.descripcion?.lowercase()?.contains(state.query.lowercase()) == true) &&
            (state.selectedCategoryId == null || it.categoriaId == state.selectedCategoryId)
    }
    val totales = viewModel.calcularTotales(state)
    val cantidadItems = state.carrito.sumOf { it.cantidad }
    val listaState = rememberLazyListState()
    val buscando = state.query.isNotBlank()

    // Con el buscador fijo, empezar a escribir devuelve la vista al catálogo:
    // el resultado aparece arriba sin tener que subir a mano.
    LaunchedEffect(buscando) {
        if (buscando) {
            listaState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 1. BARRA SUPERIOR COMPACTA ---
        BarraSuperiorVenta(
            titulo = if (state.isEditing) "Editar venta" else "Nueva venta",
            subtitulo = when {
                state.isEditing -> "Ajusta el carrito y guarda los cambios"
                cantidadItems == 0 -> "Carrito vacío"
                cantidadItems == 1 -> "1 artículo en el carrito"
                else -> "$cantidadItems artículos en el carrito"
            },
            onBack = onBack,
            onScanClick = onShowScanner
        )

        // --- 2. BUSCADOR FIJO ---
        BuscadorProductos(
            query = state.query,
            onQueryChange = { viewModel.setQuery(it) }
        )

        // --- 3. CONTENIDO DESPLAZABLE ---
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            state = listaState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PanelCatalogo(
                    productos = productosFiltrados,
                    totalCatalogo = state.productosDisponibles.size,
                    categorias = state.categorias,
                    categoriaSeleccionada = state.selectedCategoryId,
                    query = state.query,
                    tasas = state.tasas,
                    monedaBase = state.monedaBase,
                    onAgregar = { viewModel.agregarAlCarrito(it) },
                    onCategoria = { viewModel.setSelectedCategory(it) },
                    onLimpiarFiltros = {
                        viewModel.setQuery("")
                        viewModel.setSelectedCategory(null)
                    }
                )
            }

            item {
                PanelCarrito(
                    carrito = state.carrito,
                    monedaBase = state.monedaBase,
                    onIncrease = { viewModel.incrementarCantidad(it) },
                    onDecrease = { viewModel.decrementarCantidad(it) },
                    onRemove = { viewModel.eliminarDelCarrito(it) },
                    onVaciar = { viewModel.limpiarCarrito() },
                    onShowScanner = onShowScanner
                )
            }

            // El resto del cobro solo aparece cuando hay algo que cobrar.
            if (state.carrito.isNotEmpty()) {
                item {
                    PanelDescuento(
                        descuento = state.descuento,
                        esPorcentual = state.isDescuentoPorcentual,
                        descuentoAplicado = totales.subtotal - totales.total,
                        monedaBase = state.monedaBase,
                        onValorChange = { viewModel.setDescuento(it) },
                        onTipoChange = { viewModel.setTipoDescuento(it) }
                    )
                }

                item {
                    SelectorCliente(
                        clientes = state.clientes,
                        clienteSeleccionadoId = state.clienteId,
                        onSelect = { viewModel.setCliente(it) },
                        onNuevoClienteClick = { viewModel.setShowNuevoClienteDialog(true) },
                        onEditClienteClick = { viewModel.startEditingCliente(it) }
                    )
                }

                item {
                    PanelMetodoPago(
                        metodoPago = state.metodoPago,
                        efectivoRecibido = state.efectivoRecibido,
                        transferenciaMonto = state.transferenciaMonto,
                        idTransferencia = state.idTransferencia,
                        total = totales.total,
                        monedaBase = state.monedaBase,
                        clienteSeleccionado = state.clientes.find { it.id == state.clienteId }?.nombre,
                        errores = state.errors,
                        onMetodoPago = { viewModel.setMetodoPago(it) },
                        onEfectivo = { viewModel.setEfectivoRecibido(it) },
                        onTransferencia = { viewModel.setTransferenciaMonto(it) },
                        onIdTransferencia = { viewModel.setIdTransferencia(it) },
                        onMontoExacto = { viewModel.setMontoExacto() }
                    )
                }
            }
        }
    }
}

// =====================================================================================
// BARRA SUPERIOR Y BUSCADOR
// =====================================================================================

@Composable
private fun BarraSuperiorVenta(
    titulo: String,
    subtitulo: String,
    onBack: () -> Unit,
    onScanClick: () -> Unit
) {
    ScTopBar(
        title = titulo,
        subtitle = subtitulo,
        onBack = onBack,
        actions = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(Shape.Mediano)
                    .background(ScColors.acento.copy(alpha = 0.13f))
                    .border(1.dp, ScColors.acento.copy(alpha = 0.40f), Shape.Mediano)
                    .clickable(onClick = onScanClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Escanear código de barras",
                    tint = ScColors.acento,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

@Composable
private fun BuscadorProductos(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        ScSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Buscar producto o código...",
            onClear = { onQueryChange("") }
        )
    }
}

// =====================================================================================
// CATÁLOGO (resultados rápidos, fijo junto al buscador, en tira horizontal)
// =====================================================================================

@Composable
private fun PanelCatalogo(
    productos: List<Producto>,
    totalCatalogo: Int,
    categorias: List<Categoria>,
    categoriaSeleccionada: String?,
    query: String,
    tasas: Map<Moneda, Double>,
    monedaBase: Moneda,
    onAgregar: (Producto) -> Unit,
    onCategoria: (String?) -> Unit,
    onLimpiarFiltros: () -> Unit
) {
    ScPanel(contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Catálogo",
                style = MaterialTheme.typography.titleMedium,
                color = ScColors.texto
            )
            Spacer(Modifier.weight(1f))
            ScLabel(
                text = if (productos.size == 1) "1 producto" else "${productos.size} productos",
                color = ScColors.textoDebil
            )
        }

        if (categorias.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    ChipFiltro(
                        texto = "Todos",
                        seleccionado = categoriaSeleccionada == null,
                        onClick = { onCategoria(null) }
                    )
                }
                items(categorias, key = { it.id }) { categoria ->
                    ChipFiltro(
                        texto = categoria.nombre,
                        seleccionado = categoriaSeleccionada == categoria.id,
                        onClick = { onCategoria(categoria.id) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (productos.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(productos, key = { it.id }) { producto ->
                    ProductoTile(
                        producto = producto,
                        tasas = tasas,
                        monedaBase = monedaBase,
                        onClick = { onAgregar(producto) }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 14.dp)
            ) {
                Text(
                    text = if (totalCatalogo == 0) {
                        "Aún no hay productos en el catálogo. Créalos desde Productos para poder venderlos."
                    } else {
                        "Ningún producto coincide con esa búsqueda. Prueba con otro nombre o cambia la categoría."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ScColors.textoSecundario
                )
                if (totalCatalogo > 0 && (query.isNotBlank() || categoriaSeleccionada != null)) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onLimpiarFiltros,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Limpiar filtros",
                            style = MaterialTheme.typography.labelLarge,
                            color = ScColors.acento
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipFiltro(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = Shape.Full,
        color = if (seleccionado) ScColors.acento.copy(alpha = 0.14f) else ScColors.panel,
        border = BorderStroke(1.dp, if (seleccionado) ScColors.acento else ScColors.borde)
    ) {
        Box(
            modifier = Modifier.height(44.dp).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                color = if (seleccionado) ScColors.acento else ScColors.textoSecundario,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProductoTile(
    producto: Producto,
    tasas: Map<Moneda, Double>,
    monedaBase: Moneda,
    onClick: () -> Unit
) {
    // Mismo criterio de precio que el resto de la app (moneda + tasa si está vinculado).
    val precioDisplay = producto.precioVenta.formatoAuto(
        moneda = producto.moneda,
        vinculado = producto.vincularTasa,
        tasa = tasas[producto.moneda] ?: 1.0,
        monedaBase = monedaBase
    )
    val agotado = producto.sinStock

    Surface(
        onClick = onClick,
        enabled = !agotado,
        modifier = Modifier.width(142.dp).height(104.dp),
        shape = Shape.Mediano,
        color = ScColors.panelAlto,
        border = BorderStroke(1.dp, if (agotado) ScColors.alerta.copy(alpha = 0.35f) else ScColors.borde)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = if (agotado) ScColors.textoDebil else ScColors.texto,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Column {
                Text(
                    text = precioDisplay,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (agotado) ScColors.textoDebil else ScColors.dinero,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        agotado -> "Agotado"
                        producto.stockBajo -> "Quedan ${producto.stockActual}"
                        else -> "${producto.stockActual} en stock"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (agotado || producto.stockBajo) ScColors.alerta else ScColors.textoDebil,
                    maxLines = 1
                )
            }
        }
    }
}

// =====================================================================================
// CARRITO
// =====================================================================================

@Composable
private fun PanelCarrito(
    carrito: List<CarritoItem>,
    monedaBase: Moneda,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onVaciar: () -> Unit,
    onShowScanner: () -> Unit
) {
    ScPanel(contentPadding = PaddingValues(0.dp)) {
        CabeceraCarrito(
            cantidad = carrito.sumOf { it.cantidad },
            hayItems = carrito.isNotEmpty(),
            onVaciar = onVaciar
        )

        if (carrito.isEmpty()) {
            ScEmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "El carrito está vacío",
                message = "Busca un producto arriba o escanea su código para empezar el cobro.",
                actionLabel = "Escanear código",
                onAction = onShowScanner
            )
        } else {
            carrito.forEachIndexed { index, item ->
                if (index > 0) {
                    ScDivider(modifier = Modifier.padding(start = 62.dp))
                }
                FilaCarrito(
                    item = item,
                    monedaBase = monedaBase,
                    onMenos = {
                        if (item.cantidad > 1) {
                            onDecrease(item.producto.id)
                        } else {
                            onRemove(item.producto.id)
                        }
                    },
                    onMas = { onIncrease(item.producto.id) }
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CabeceraCarrito(
    cantidad: Int,
    hayItems: Boolean,
    onVaciar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = ScColors.acento,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Carrito",
            style = MaterialTheme.typography.titleMedium,
            color = ScColors.texto
        )
        if (hayItems) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .background(ScColors.panelAlto, Shape.Full)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$cantidad ${if (cantidad == 1) "artículo" else "artículos"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScColors.textoSecundario,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (hayItems) {
            TextButton(
                onClick = onVaciar,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Vaciar",
                    style = MaterialTheme.typography.labelLarge,
                    color = ScColors.alerta
                )
            }
        }
    }
}

@Composable
private fun FilaCarrito(
    item: CarritoItem,
    monedaBase: Moneda,
    onMenos: () -> Unit,
    onMas: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniaturaProducto(producto = item.producto)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.producto.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = ScColors.texto,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${item.precioUnitario.formatoAuto(item.producto.moneda, item.producto.vincularTasa, monedaBase = monedaBase)} c/u",
                style = MaterialTheme.typography.bodySmall,
                color = ScColors.textoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        StepperCantidad(
            cantidad = item.cantidad,
            puedeAumentar = item.puedeAumentar,
            onMenos = onMenos,
            onMas = onMas
        )
    }
}

@Composable
private fun MiniaturaProducto(producto: Producto) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(Shape.Pequeno)
            .background(ScColors.panelHundido),
        contentAlignment = Alignment.Center
    ) {
        val url = producto.imagenUrl
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = if (url.startsWith("/")) java.io.File(url) else url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = ScColors.textoDebil,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StepperCantidad(
    cantidad: Int,
    puedeAumentar: Boolean,
    onMenos: () -> Unit,
    onMas: () -> Unit
) {
    Surface(
        shape = Shape.Mediano,
        color = ScColors.panelAlto,
        border = BorderStroke(1.dp, ScColors.borde)
    ) {
        Row(modifier = Modifier.height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
                    .clickable(onClick = onMenos),
                contentAlignment = Alignment.Center
            ) {
                if (cantidad <= 1) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Quitar del carrito",
                        tint = ScColors.alerta,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Menos",
                        tint = ScColors.textoSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = cantidad.toString(),
                modifier = Modifier.width(26.dp),
                style = MaterialTheme.typography.titleSmall,
                color = ScColors.texto,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
                    .clickable(enabled = puedeAumentar, onClick = onMas),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Más",
                    tint = if (puedeAumentar) ScColors.acento else ScColors.textoDebil,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// =====================================================================================
// DESCUENTO
// =====================================================================================

@Composable
private fun PanelDescuento(
    descuento: String,
    esPorcentual: Boolean,
    descuentoAplicado: Double,
    monedaBase: Moneda,
    onValorChange: (String) -> Unit,
    onTipoChange: (Boolean) -> Unit
) {
    ScPanel(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocalOffer,
                contentDescription = null,
                tint = ScColors.dinero,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Descuento",
                style = MaterialTheme.typography.titleSmall,
                color = ScColors.texto
            )
            Spacer(Modifier.weight(1f))
            if (descuentoAplicado > 0) {
                Text(
                    text = "-${descuentoAplicado.formatoMoneda(monedaBase)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ScColors.dinero,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CampoTexto(
                value = descuento,
                onValueChange = { nuevo ->
                    if (nuevo.isEmpty() || nuevo.all { c -> c.isDigit() || c == '.' }) {
                        onValorChange(nuevo)
                    }
                },
                label = "Valor",
                modifier = Modifier.weight(1f),
                suffix = if (esPorcentual) "%" else monedaBase.name,
                keyboardType = KeyboardType.Decimal
            )
            Spacer(Modifier.width(8.dp))
            ControlTipoDescuento(
                esPorcentual = esPorcentual,
                monedaBase = monedaBase,
                onTipoChange = onTipoChange
            )
        }
    }
}

@Composable
private fun ControlTipoDescuento(
    esPorcentual: Boolean,
    monedaBase: Moneda,
    onTipoChange: (Boolean) -> Unit
) {
    Surface(
        shape = Shape.Mediano,
        color = ScColors.panelHundido,
        border = BorderStroke(1.dp, ScColors.borde)
    ) {
        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Segmento(
                texto = "%",
                seleccionado = esPorcentual,
                onClick = { onTipoChange(true) },
                modifier = Modifier.width(46.dp).height(44.dp)
            )
            Segmento(
                texto = if (monedaBase.name.length <= 4) monedaBase.name else "$",
                seleccionado = !esPorcentual,
                onClick = { onTipoChange(false) },
                modifier = Modifier.width(52.dp).height(44.dp)
            )
        }
    }
}

// =====================================================================================
// CLIENTE (hoja inferior)
// =====================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCliente(
    clientes: List<ClienteSimple>,
    clienteSeleccionadoId: String?,
    onSelect: (String?) -> Unit,
    onNuevoClienteClick: () -> Unit,
    onEditClienteClick: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clienteActual = clientes.find { it.id == clienteSeleccionadoId }

    Surface(
        onClick = { showSheet = true },
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Mediano,
        color = ScColors.panel,
        border = BorderStroke(1.dp, ScColors.borde)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(ScColors.panelAlto, Shape.Pequeno),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (clienteActual == null) Icons.Default.PersonOutline else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (clienteActual == null) ScColors.textoSecundario else ScColors.acento,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                ScLabel(text = "Cliente")
                Text(
                    text = clienteActual?.nombre ?: "Consumidor final",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (clienteActual == null) ScColors.textoSecundario else ScColors.texto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Cambiar cliente",
                tint = ScColors.textoDebil,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = ScColors.canvas,
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

@Composable
private fun ClientePickerContent(
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
            .fillMaxHeight(0.82f)
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Seleccionar cliente",
            style = MaterialTheme.typography.titleLarge,
            color = ScColors.texto
        )
        Spacer(Modifier.height(12.dp))
        ScSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Buscar por nombre o teléfono...",
            onClear = { searchQuery = "" }
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ScPanel(contentPadding = PaddingValues(0.dp)) {
                    FilaCliente(
                        icon = Icons.Default.PersonOutline,
                        titulo = "Consumidor final",
                        subtitulo = "Sin datos de cliente",
                        onClick = { onSelect(null) }
                    )
                    ScDivider(modifier = Modifier.padding(start = 62.dp))
                    FilaCliente(
                        icon = Icons.Default.PersonAdd,
                        titulo = "Agregar nuevo cliente",
                        subtitulo = "Nombre, CI y teléfono",
                        tint = ScColors.acento,
                        onClick = onNuevoClienteClick
                    )

                    if (filteredClientes.isNotEmpty()) {
                        ScDivider(modifier = Modifier.padding(start = 62.dp))
                        Text(
                            text = if (filteredClientes.size == 1) "1 cliente" else "${filteredClientes.size} clientes",
                            style = MaterialTheme.typography.labelMedium,
                            color = ScColors.textoDebil,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                        filteredClientes.forEachIndexed { index, cliente ->
                            if (index > 0) {
                                ScDivider(modifier = Modifier.padding(start = 62.dp))
                            }
                            FilaCliente(
                                icon = Icons.Default.Person,
                                titulo = cliente.nombre,
                                subtitulo = cliente.telefono,
                                tint = ScColors.acento,
                                onClick = { onSelect(cliente.id) },
                                trailing = {
                                    ScIconButton(
                                        icon = Icons.Default.Edit,
                                        contentDescription = "Editar cliente",
                                        onClick = { onEditClick(cliente.id) },
                                        modifier = Modifier.size(44.dp),
                                        tint = ScColors.textoSecundario,
                                        containerColor = ScColors.panelAlto
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (filteredClientes.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    ScEmptyState(
                        icon = Icons.Default.Search,
                        title = "Sin coincidencias",
                        message = "No hay clientes con ese nombre o teléfono. Puedes crear uno nuevo.",
                        actionLabel = "Agregar nuevo cliente",
                        onAction = onNuevoClienteClick
                    )
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun FilaCliente(
    icon: ImageVector,
    titulo: String,
    subtitulo: String? = null,
    tint: Color = ScColors.textoSecundario,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(tint.copy(alpha = 0.12f), Shape.Pequeno),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                color = ScColors.texto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = ScColors.textoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

// =====================================================================================
// MÉTODO DE PAGO Y MONTOS
// =====================================================================================

@Composable
private fun PanelMetodoPago(
    metodoPago: MetodoPago,
    efectivoRecibido: String,
    transferenciaMonto: String,
    idTransferencia: String,
    total: Double,
    monedaBase: Moneda,
    clienteSeleccionado: String?,
    errores: Map<String, String>,
    onMetodoPago: (MetodoPago) -> Unit,
    onEfectivo: (String) -> Unit,
    onTransferencia: (String) -> Unit,
    onIdTransferencia: (String) -> Unit,
    onMontoExacto: () -> Unit
) {
    ScPanel(contentPadding = PaddingValues(16.dp)) {
        Text(
            text = "Método de pago",
            style = MaterialTheme.typography.titleMedium,
            color = ScColors.texto
        )
        Spacer(Modifier.height(10.dp))
        ControlMetodoPago(metodoPago = metodoPago, onSelect = onMetodoPago)
        Spacer(Modifier.height(14.dp))

        when (metodoPago) {
            MetodoPago.EFECTIVO -> {
                val efectivo = efectivoRecibido.toDoubleOrNull() ?: 0.0
                val cambio = (efectivo - total).coerceAtLeast(0.0)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InputMonto(
                        label = "Efectivo recibido",
                        value = efectivoRecibido,
                        onValueChange = onEfectivo,
                        onExactClick = onMontoExacto,
                        error = errores["efectivo"]
                    )
                    if (efectivo > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ScColors.acento.copy(alpha = 0.10f), Shape.Mediano)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = ScColors.acento,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Cambio a devolver",
                                style = MaterialTheme.typography.bodySmall,
                                color = ScColors.textoSecundario,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = cambio.formatoMoneda(monedaBase),
                                style = MaterialTheme.typography.titleSmall,
                                color = ScColors.acento,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            MetodoPago.TRANSFERENCIA -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ScColors.info,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Confirma que recibiste ${total.formatoMoneda(monedaBase)} en tu cuenta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ScColors.textoSecundario
                        )
                    }
                    CampoTexto(
                        value = idTransferencia,
                        onValueChange = onIdTransferencia,
                        label = "ID de transacción / comprobante",
                        icono = Icons.Default.ConfirmationNumber
                    )
                }
            }

            MetodoPago.MIXTO -> {
                val efectivo = efectivoRecibido.toDoubleOrNull() ?: 0.0
                val transferencia = transferenciaMonto.toDoubleOrNull() ?: 0.0
                val suma = efectivo + transferencia
                val resta = total - suma
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScLabel(text = "Reparto de montos")
                    InputMonto(
                        label = "Monto en efectivo",
                        value = efectivoRecibido,
                        onValueChange = onEfectivo,
                        error = errores["efectivo"]
                    )
                    InputMonto(
                        label = "Monto por transferencia",
                        value = transferenciaMonto,
                        onValueChange = onTransferencia,
                        error = errores["transferencia"]
                    )
                    CampoTexto(
                        value = idTransferencia,
                        onValueChange = onIdTransferencia,
                        label = "ID de transacción (transferencia)",
                        icono = Icons.Default.ConfirmationNumber
                    )
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Suma",
                            style = MaterialTheme.typography.bodySmall,
                            color = ScColors.textoSecundario,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = suma.formatoMoneda(monedaBase),
                            style = MaterialTheme.typography.titleSmall,
                            color = ScColors.texto,
                            maxLines = 1
                        )
                    }
                    if (resta != 0.0) {
                        Text(
                            text = if (resta > 0) {
                                "Faltan ${resta.formatoMoneda(monedaBase)}"
                            } else {
                                "Sobran ${(-resta).formatoMoneda(monedaBase)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (resta > 0) ScColors.alerta else ScColors.acento,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    errores["pagoMixto"]?.let { TextoError(it) }
                }
            }

            MetodoPago.CREDITO -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = null,
                            tint = ScColors.acento,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (clienteSeleccionado != null) {
                                "Venta a cuenta de $clienteSeleccionado"
                            } else {
                                "Selecciona un cliente para fiar esta venta."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (clienteSeleccionado != null) ScColors.texto else ScColors.alerta
                        )
                    }
                    errores["metodoPago"]?.let { TextoError(it) }
                }
            }
        }
    }
}

@Composable
private fun ControlMetodoPago(
    metodoPago: MetodoPago,
    onSelect: (MetodoPago) -> Unit
) {
    Surface(
        shape = Shape.Mediano,
        color = ScColors.panelHundido,
        border = BorderStroke(1.dp, ScColors.borde)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Segmento(
                    texto = "Efectivo",
                    seleccionado = metodoPago == MetodoPago.EFECTIVO,
                    onClick = { onSelect(MetodoPago.EFECTIVO) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    icono = Icons.Default.Payments
                )
                Segmento(
                    texto = "Transferencia",
                    seleccionado = metodoPago == MetodoPago.TRANSFERENCIA,
                    onClick = { onSelect(MetodoPago.TRANSFERENCIA) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    icono = Icons.Default.AccountBalance
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Segmento(
                    texto = "Mixto",
                    seleccionado = metodoPago == MetodoPago.MIXTO,
                    onClick = { onSelect(MetodoPago.MIXTO) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    icono = Icons.Default.SwapHoriz
                )
                Segmento(
                    texto = "Crédito",
                    seleccionado = metodoPago == MetodoPago.CREDITO,
                    onClick = { onSelect(MetodoPago.CREDITO) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    icono = Icons.Default.CreditCard
                )
            }
        }
    }
}

/** Segmento del control: jade tenue + borde jade cuando está activo. */
@Composable
private fun Segmento(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null
) {
    val contenido = if (seleccionado) ScColors.acento else ScColors.textoSecundario
    Box(
        modifier = modifier
            .clip(Shape.Mediano)
            .background(if (seleccionado) ScColors.acento.copy(alpha = 0.14f) else ScColors.panelHundido)
            .border(1.dp, if (seleccionado) ScColors.acento else ScColors.panelHundido, Shape.Mediano)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icono != null) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = contenido,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                color = contenido,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TextoError(mensaje: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = ScColors.alerta,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.labelSmall,
            color = ScColors.alerta
        )
    }
}

// =====================================================================================
// CAMPOS DE TEXTO DEL SISTEMA
// =====================================================================================

@Composable
private fun campoColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ScColors.panelAlto,
    unfocusedContainerColor = ScColors.panelAlto,
    focusedBorderColor = ScColors.acento,
    unfocusedBorderColor = ScColors.borde,
    cursorColor = ScColors.acento,
    focusedTextColor = ScColors.texto,
    unfocusedTextColor = ScColors.texto,
    focusedLabelColor = ScColors.textoSecundario,
    unfocusedLabelColor = ScColors.textoDebil
)

@Composable
private fun CampoTexto(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    suffix: String? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = if (icono != null) {
            {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = ScColors.textoDebil,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        suffix = if (suffix != null) {
            {
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.labelMedium,
                    color = ScColors.textoSecundario
                )
            }
        } else null,
        supportingText = supportingText,
        isError = isError,
        singleLine = singleLine,
        shape = Shape.Mediano,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = campoColors()
    )
}

@Composable
private fun InputMonto(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onExactClick: (() -> Unit)? = null,
    error: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { nuevo ->
            if (nuevo.isEmpty() || nuevo.all { c -> c.isDigit() || c == '.' }) {
                onValueChange(nuevo)
            }
        },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Money,
                contentDescription = null,
                tint = ScColors.textoDebil,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (onExactClick != null) {
            {
                TextButton(
                    onClick = onExactClick,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Exacto",
                        style = MaterialTheme.typography.labelMedium,
                        color = ScColors.acento
                    )
                }
            }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Mediano,
        singleLine = true,
        isError = error != null,
        supportingText = if (error != null) {
            {
                Text(
                    text = error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = campoColors()
    )
}

// =====================================================================================
// DIÁLOGO DE CLIENTE
// =====================================================================================

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
        containerColor = ScColors.panel,
        shape = Shape.Grande,
        titleContentColor = ScColors.texto,
        textContentColor = ScColors.textoSecundario,
        title = {
            Text(
                text = if (isEditing) "Editar cliente" else "Nuevo cliente",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    value = nombre,
                    onValueChange = onNombreChange,
                    label = "Nombre completo *",
                    icono = Icons.Default.Badge
                )
                CampoTexto(
                    value = ci,
                    onValueChange = { nuevo ->
                        if (nuevo.length <= 11 && nuevo.all { c -> c.isDigit() }) onCIChange(nuevo)
                    },
                    label = "Carnet de identidad *",
                    icono = Icons.Default.Fingerprint,
                    keyboardType = KeyboardType.Number,
                    supportingText = {
                        Text(
                            text = "${ci.length}/11 dígitos",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScColors.textoDebil,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
                CampoTexto(
                    value = telefono,
                    onValueChange = onTelefonoChange,
                    label = "Teléfono (opcional)",
                    icono = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = nombre.isNotBlank() && ci.length == 11,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ScColors.acento,
                    disabledContentColor = ScColors.textoDebil
                )
            ) {
                Text(
                    text = if (isEditing) "Guardar cambios" else "Registrar y seleccionar",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = ScColors.textoSecundario)
            ) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}

// =====================================================================================
// ESTADOS DE PANTALLA
// =====================================================================================

@Composable
fun PantallaCargandoVenta(mensaje: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ScColors.acento)
            Spacer(Modifier.height(16.dp))
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = ScColors.textoSecundario
            )
        }
    }
}

@Composable
fun PantallaExitoVenta(
    ventaId: String,
    onContinue: () -> Unit,
    onShareTicket: (String) -> Unit,
    onPrintTicket: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(ScColors.acento.copy(alpha = 0.13f), Shape.Full)
                .border(1.dp, ScColors.acento.copy(alpha = 0.45f), Shape.Full),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ScColors.acento,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Venta registrada",
            style = MaterialTheme.typography.headlineMedium,
            color = ScColors.texto,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "El inventario y la caja ya incluyen este cobro.",
            style = MaterialTheme.typography.bodyMedium,
            color = ScColors.textoSecundario,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        ScPrimaryButton(
            text = "Imprimir ticket",
            onClick = { onPrintTicket(ventaId) },
            icon = Icons.Default.Print
        )
        Spacer(Modifier.height(10.dp))
        ScSecondaryButton(
            text = "Compartir PDF",
            onClick = { onShareTicket(ventaId) },
            icon = Icons.Default.Share
        )
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Volver",
                style = MaterialTheme.typography.labelLarge,
                color = ScColors.acento
            )
        }
    }
}

@Composable
fun PantallaErrorVenta(mensaje: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(ScColors.alerta.copy(alpha = 0.12f), Shape.Full)
                .border(1.dp, ScColors.alerta.copy(alpha = 0.40f), Shape.Full),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = ScColors.alerta,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            // PantallaErrorVenta también la usa DetalleVentaScreen: título neutro
            // y el detalle concreto llega en `mensaje`.
            text = "Algo salió mal",
            style = MaterialTheme.typography.titleLarge,
            color = ScColors.texto,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = ScColors.textoSecundario,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        ScPrimaryButton(text = "Reintentar", onClick = onRetry)
    }
}

// =====================================================================================
// BARRA INFERIOR FIJA DE COBRO
// =====================================================================================

@Composable
private fun BarraCobro(
    state: NuevaVentaUiState.Editing,
    totales: CarritoTotales,
    onCobrar: () -> Unit
) {
    val cobrando = state.isLoading && state.carrito.isNotEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScColors.canvas)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ScColors.borde)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ScLabel(
                    text = if (state.isEditing) "Total a guardar" else "Total a cobrar",
                    color = ScColors.textoSecundario
                )
                ScMoney(
                    value = "%,.2f".format(Locale.US, totales.total),
                    unit = state.monedaBase.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = ScColors.dinero
                )
            }
            Spacer(Modifier.width(12.dp))
            ScPrimaryButton(
                text = when {
                    cobrando -> "Guardando"
                    state.isEditing -> "Guardar"
                    else -> "Cobrar"
                },
                onClick = onCobrar,
                modifier = Modifier.width(138.dp),
                icon = if (cobrando) null else Icons.Default.Check,
                enabled = state.carrito.isNotEmpty() && !state.isLoading
            )
        }
    }
}
