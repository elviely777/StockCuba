package cu.stockcuba.app.presentation.productos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import cu.stockcuba.app.presentation.navigation.Screen
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaTheme

/**
 * Pantalla Lista de Productos - Réplica fiel del diseño de Stitch.
 *
 * Elementos:
 * - Header con título y contador
 * - Search bar (estilo Stitch: fondo surface L2, borde focus teal)
 * - Filtro de categoría (chip dropdown)
 * - LazyColumn con tarjetas de producto
 * - Cada tarjeta: nombre, categoría, precio, stock con badge color
 * - FAB "Agregar Producto" (teal con glow)
 */
@Composable
fun ListaProductosScreen(
    onAgregar: () -> Unit,
    onDetalle: (String) -> Unit,
    onEditar: (String) -> Unit,
    viewModel: ListaProductosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val queryText = remember { mutableStateOf("") }
    val showCategoriaMenu = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(StockCubaSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
        ) {
            // ===== HEADER =====
            ListaProductosHeader(queryText = queryText, onQueryChange = { text ->
                queryText.value = text
                viewModel.setQuery(text)
            })

            // ===== BARRA DE FILTROS =====
            FiltrosBarra(
                categorias = when (uiState) {
                    is ListaProductosUiState.Success -> uiState.categorias
                    else -> emptyList()
                },
                categoriaSeleccionada = when (uiState) {
                    is ListaProductosUiState.Success -> uiState.categoriaSeleccionada
                    else -> null
                },
                onCategoriaClick = { categoriaId ->
                    viewModel.setCategoria(categoriaId)
                    showCategoriaMenu.value = false
                },
                onLimpiarFiltros = { viewModel.limpiarFiltros() },
                showCategoriaMenu = showCategoriaMenu
            )

            // ===== LISTA DE PRODUCTOS =====
            when (val state = uiState) {
                is ListaProductosUiState.Loading -> ListaProductosSkeleton()
                is ListaProductosUiState.Error -> ListaProductosError(message = state.message)
                is ListaProductosUiState.Success -> {
                    if (state.productos.isEmpty()) {
                        ListaProductosVacia(onAgregar = onAgregar)
                    } else {
                        ListaProductosContenido(
                            productos = state.productos,
                            categorias = state.categorias,
                            onDetalle = onDetalle,
                            onEditar = onEditar
                        )
                    }
                }
            }
        }

        // ===== FAB AGREGAR =====
        FloatingActionButtonAgregar(onClick = onAgregar)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaProductosHeader(
    queryText: androidx.compose.runtime.MutableState<String>,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text("Productos", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Catálogo completo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Search Bar estilo Stitch - Rediseñado para centrado vertical perfecto
        BasicTextField(
            value = queryText.value,
            onValueChange = onQueryChange,
            modifier = Modifier
                .width(280.dp)
                .height(40.dp)
                .background(StockCubaColors.InputFondo, Shape.Full),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (queryText.value.isEmpty()) {
                            Text(
                                text = "Buscar productos...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                    if (queryText.value.isNotEmpty()) {
                        IconButton(
                            onClick = { queryText.value = ""; onQueryChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltrosBarra(
    categorias: List<Categoria>,
    categoriaSeleccionada: String?,
    onCategoriaClick: (String?) -> Unit,
    onLimpiarFiltros: () -> Unit,
    showCategoriaMenu: androidx.compose.runtime.MutableState<Boolean>
) {
    val categoriaNombre = categorias.firstOrNull { it.id == categoriaSeleccionada }?.nombre ?: "Todas"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm)
    ) {
        // Chip categoría (dropdown)
        Box {
            FilterChip(
                selected = true,
                onClick = { showCategoriaMenu.value = !showCategoriaMenu.value },
                leadingIcon = { Icon(imageVector = Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                label = { Text("$categoriaNombre ▼", style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = StockCubaColors.InputFondo,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = Shape.ExtraGrande,
                modifier = Modifier
                    .width(160.dp)
                    .height(40.dp)
            )

            DropdownMenu(
                expanded = showCategoriaMenu.value,
                onDismissRequest = { showCategoriaMenu.value = false }
            ) {
                // Items del menú
                DropdownMenuItem(
                    onClick = { onCategoriaClick(null); showCategoriaMenu.value = false },
                    text = { Text("Todas las categorías", style = MaterialTheme.typography.bodyMedium) }
                )
                categorias.forEach { cat ->
                    DropdownMenuItem(
                        onClick = { onCategoriaClick(cat.id); showCategoriaMenu.value = false },
                        text = { Text(cat.nombre, style = MaterialTheme.typography.bodyMedium) }
                    )
                }
            }
        }

        // Botón limpiar filtros (visible si hay filtro activo)
        if (categoriaSeleccionada != null) {
            OutlinedButton(
                onClick = onLimpiarFiltros,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = Shape.ExtraGrande
            ) {
                Text("Limpiar", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ListaProductosContenido(
    productos: List<Producto>,
    categorias: List<Categoria>,
    onDetalle: (String) -> Unit,
    onEditar: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp), // espacio para FAB
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm),
        contentPadding = PaddingValues(StockCubaSpacing.Md)
    ) {
        items(productos) { producto ->
            ProductoCard(
                producto = producto,
                categorias = categorias,
                onClick = { onDetalle(producto.id) },
                onLongClick = { onEditar(producto.id) }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProductoCard(
    producto: Producto,
    categorias: List<Categoria>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val status = producto.stockStatus()
    val statusColor = producto.stockStatusColor()
    val statusBg = producto.stockStatusBackground()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 0.dp)
            .clip(Shape.Grande)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = Shape.Grande,
        border = BorderStroke(1.dp, StockCubaColors.BordeSutil)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(StockCubaSpacing.Md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(Shape.Grande)
                    .background(Color.Transparent),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info principal
                Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = producto.nombre,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Categoría: ${categorias.find { it.id == producto.categoriaId }?.nombre ?: "Desconocida"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Precio: ${producto.precioVenta.formatoCUP()} | Costo: ${producto.costoUnitario.formatoCUP()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stock badge + precio
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Badge de stock
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .background(statusBg, Shape.Pequeno)
                    ) {
                        Text(
                            text = producto.stockStatusLabel(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                    Text(
                        text = "${producto.stockActual.formatoCantidad()} ${producto.unidadMedida.name.lowercase(java.util.Locale.getDefault())}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ListaProductosSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Sm),
        contentPadding = PaddingValues(StockCubaSpacing.Md)
    ) {
        items(6) {
            Card(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                shape = Shape.Grande
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun ListaProductosError(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StockCubaColors.CoralAlerta, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Error al cargar", style = MaterialTheme.typography.titleMedium, color = StockCubaColors.CoralAlerta)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ListaProductosVacia(onAgregar: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StockCubaColors.VerdeExito.copy(alpha = 0.15f)),
                shape = Shape.ExtraGrande
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = StockCubaColors.VerdeExito, modifier = Modifier.size(64.dp).padding(StockCubaSpacing.Lg))
            }
            Text("No hay productos", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Comienza agregando tu primer producto", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onAgregar, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), shape = Shape.ExtraGrande) {
                Text("Agregar Producto", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun FloatingActionButtonAgregar(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = StockCubaColors.FabFondo,
            contentColor = StockCubaColors.FabTexto,
            shape = Shape.Full,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp,
                focusedElevation = 12.dp
            ),
            modifier = Modifier.padding(StockCubaSpacing.Lg)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar producto", modifier = Modifier.size(28.dp))
        }
    }
}