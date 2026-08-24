package cu.stockcuba.app.presentation.productos

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import cu.stockcuba.app.presentation.dashboard.formatoCantidad
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

/**
 * Pantalla Lista de Productos - Rediseño Moderno e Inmersivo.
 */
@Composable
fun ListaProductosScreen(
    onAgregar: () -> Unit,
    onDetalle: (String) -> Unit,
    onEditar: (String) -> Unit,
    viewModel: ListaProductosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            ListaProductosHeader(
                count = (uiState as? ListaProductosUiState.Success)?.productos?.size ?: 0,
                query = (uiState as? ListaProductosUiState.Success)?.query ?: "",
                onQueryChange = { viewModel.setQuery(it) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAgregar,
                containerColor = StockCubaColors.VerdeExito,
                contentColor = Color(0xFF001E1C),
                shape = Shape.Grande,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ===== BARRA DE CATEGORÍAS (CHIPS MODERNOS) =====
            if (uiState is ListaProductosUiState.Success) {
                val state = uiState as ListaProductosUiState.Success
                CategoriasBarraHorizontal(
                    categorias = state.categorias,
                    seleccionada = state.categoriaSeleccionada,
                    onSelect = { viewModel.setCategoria(it) }
                )
            }

            // ===== CONTENIDO PRINCIPAL =====
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is ListaProductosUiState.Loading -> ListaProductosSkeleton()
                    is ListaProductosUiState.Error -> ListaProductosError(state.message)
                    is ListaProductosUiState.Success -> {
                        if (state.productos.isEmpty()) {
                            ListaProductosVacia(
                                isFiltered = state.query.isNotEmpty() || state.categoriaSeleccionada != null,
                                onClear = { viewModel.limpiarFiltros() },
                                onAgregar = onAgregar
                            )
                        } else {
                            ListaProductosLazy(
                                productos = state.productos,
                                categorias = state.categorias,
                                onDetalle = onDetalle,
                                onEditar = onEditar
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaProductosHeader(
    count: Int,
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = StockCubaSpacing.Lg, vertical = StockCubaSpacing.Md)
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Productos",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        "$count artículos en inventario",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = StockCubaColors.VerdeExito.copy(alpha = 0.1f),
                    shape = Shape.Full
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        tint = StockCubaColors.VerdeExito,
                        modifier = Modifier.padding(8.dp).size(24.dp)
                    )
                }
            }

            // Barra de búsqueda moderna
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar por nombre o descripción...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(18.dp))
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = Shape.Grande,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = StockCubaColors.VerdeExito
                )
            )
        }
    }
}

@Composable
fun CategoriasBarraHorizontal(
    categorias: List<Categoria>,
    seleccionada: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = StockCubaSpacing.Lg, vertical = StockCubaSpacing.Sm),
        horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
    ) {
        item {
            CategoriaChip(
                nombre = "Todos",
                isSelected = seleccionada == null,
                onClick = { onSelect(null) }
            )
        }
        items(categorias) { cat ->
            CategoriaChip(
                nombre = cat.nombre,
                isSelected = seleccionada == cat.id,
                color = Color(cat.color),
                onClick = { onSelect(cat.id) }
            )
        }
    }
}

@Composable
fun CategoriaChip(
    nombre: String,
    isSelected: Boolean,
    color: Color = StockCubaColors.VerdeExito,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        shape = Shape.Full,
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isSelected) color else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = nombre,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ListaProductosLazy(
    productos: List<Producto>,
    categorias: List<Categoria>,
    onDetalle: (String) -> Unit,
    onEditar: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = StockCubaSpacing.Sm, start = StockCubaSpacing.Lg, end = StockCubaSpacing.Lg, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
    ) {
        items(productos, key = { it.id }) { producto ->
            val categoria = categorias.find { it.id == producto.categoriaId }
            ProductoCardModerno(
                producto = producto,
                categoriaNombre = categoria?.nombre ?: "Sin categoría",
                categoriaColor = categoria?.let { Color(it.color) } ?: MaterialTheme.colorScheme.outline,
                onClick = { onDetalle(producto.id) },
                onLongClick = { onEditar(producto.id) }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProductoCardModerno(
    producto: Producto,
    categoriaNombre: String,
    categoriaColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val statusLabel = producto.stockStatusLabel()
    val statusColor = producto.stockStatusColor()
    val statusBg = producto.stockStatusBackground()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shape.Grande)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barra lateral de color de categoría
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(categoriaColor)
            )

            Column(
                modifier = Modifier.padding(StockCubaSpacing.Md).weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Badge de Precio
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = Shape.Pequeno
                    ) {
                        Text(
                            producto.precioVenta.formatoCUP(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = categoriaNombre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${producto.stockActual.formatoCantidad()} ${producto.unidadMedida.name.lowercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Status Pill
                    Surface(
                        color = statusBg,
                        shape = Shape.Full
                    ) {
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListaProductosSkeleton() {
    Column(modifier = Modifier.padding(StockCubaSpacing.Lg), verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(Shape.Grande)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun ListaProductosError(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = StockCubaColors.CoralAlerta)
        Spacer(Modifier.height(16.dp))
        Text("No se pudo cargar la lista", style = MaterialTheme.typography.titleMedium)
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ListaProductosVacia(isFiltered: Boolean, onClear: () -> Unit, onAgregar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(StockCubaSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val icon = if (isFiltered) Icons.Default.SearchOff else Icons.Default.AddShoppingCart
        val title = if (isFiltered) "No hay coincidencias" else "Inventario vacío"
        val desc = if (isFiltered) "Prueba con otros filtros o términos de búsqueda" else "Empieza agregando productos para gestionar tu negocio"

        Surface(modifier = Modifier.size(100.dp), shape = Shape.Full, color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        if (isFiltered) {
            OutlinedButton(onClick = onClear, shape = Shape.Grande) { Text("Limpiar filtros") }
        } else {
            Button(onClick = onAgregar, shape = Shape.Grande) { Text("Agregar mi primer producto") }
        }
    }
}
