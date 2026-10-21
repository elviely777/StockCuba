package cu.stockcuba.app.presentation.productos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.domain.model.Moneda
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.UnidadMedida
import cu.stockcuba.app.presentation.common.ScDivider
import cu.stockcuba.app.presentation.common.ScEmptyState
import cu.stockcuba.app.presentation.common.ScPanel
import cu.stockcuba.app.presentation.common.ScScreenHeader
import cu.stockcuba.app.presentation.common.ScSearchField
import cu.stockcuba.app.presentation.common.ScStatusPill
import cu.stockcuba.app.presentation.common.ScTone
import cu.stockcuba.app.presentation.dashboard.formatoAuto
import cu.stockcuba.app.presentation.dashboard.formatoCantidad
import cu.stockcuba.app.presentation.theme.ScColors
import cu.stockcuba.app.presentation.theme.Shape

/**
 * Catálogo de productos · sistema "Mostrador".
 *
 * La pantalla es un instrumento: cabecera con el conteo, buscador, filtros de
 * categoría en una fila desplazable y el catálogo agrupado por categoría en
 * paneles con separadores de 1px. Cada fila es compacta (miniatura 48dp,
 * nombre, stock y precio) y el jade marca lo positivo, el ámbar el dinero y el
 * coral el riesgo real de quedarse sin producto.
 *
 * La lógica (ViewModel, filtros, selección múltiple, navegación) es la misma que
 * antes: aquí solo cambia la composición visual.
 */
@Composable
fun ListaProductosScreen(
    onAgregar: () -> Unit,
    onDetalle: (String) -> Unit,
    onEditar: (String) -> Unit,
    viewModel: ListaProductosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState as? ListaProductosUiState.Success
    val isSelectionMode = state?.isSelectionMode == true
    val selectedCount = state?.selectedProductIds?.size ?: 0
    val conteo = state?.productos?.size ?: 0
    val filtrando = state != null && (state.query.isNotBlank() || state.categoriaSeleccionada != null)

    Scaffold(
        containerColor = ScColors.canvas,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isSelectionMode) {
                    BarraSeleccion(
                        count = selectedCount,
                        onCancelar = { viewModel.clearSelection() },
                        onEliminar = { viewModel.deleteSelected() }
                    )
                } else {
                    ScScreenHeader(
                        title = "Productos",
                        subtitle = when {
                            state == null -> null
                            filtrando -> "$conteo ${if (conteo == 1) "resultado" else "resultados"} de la búsqueda"
                            else -> "$conteo ${if (conteo == 1) "producto" else "productos"} en el catálogo"
                        }
                    )
                    ScSearchField(
                        value = state?.query.orEmpty(),
                        onValueChange = { viewModel.setQuery(it) },
                        placeholder = "Buscar por nombre o descripción",
                        onClear = { viewModel.setQuery("") },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                if (state != null) {
                    ChipsCategorias(
                        categorias = state.categorias,
                        seleccionada = state.categoriaSeleccionada,
                        onSelect = { viewModel.setCategoria(it) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FabAgregarProducto(onClick = onAgregar)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = uiState) {
                is ListaProductosUiState.Loading -> CatalogoCargando()
                is ListaProductosUiState.Error -> CatalogoError(mensaje = s.message)
                is ListaProductosUiState.Success -> {
                    if (s.productos.isEmpty()) {
                        CatalogoVacio(
                            isFiltered = s.query.isNotEmpty() || s.categoriaSeleccionada != null,
                            onClear = { viewModel.limpiarFiltros() },
                            onAgregar = onAgregar
                        )
                    } else {
                        CatalogoLista(
                            state = s,
                            isSelectionMode = isSelectionMode,
                            onDetalle = { id ->
                                if (s.isSelectionMode) viewModel.toggleSelection(id) else onDetalle(id)
                            },
                            onToggleSelection = { id -> viewModel.toggleSelection(id) }
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================================
// CABECERA EN MODO SELECCIÓN
// =====================================================================================

@Composable
private fun BarraSeleccion(
    count: Int,
    onCancelar: () -> Unit,
    onEliminar: () -> Unit
) {
    var confirmarEliminar by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$count ${if (count == 1) "seleccionado" else "seleccionados"}",
                style = MaterialTheme.typography.titleLarge,
                color = ScColors.texto,
                maxLines = 1
            )
            Text(
                text = "Mantén pulsado un producto para ajustar la selección",
                style = MaterialTheme.typography.bodySmall,
                color = ScColors.textoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { confirmarEliminar = true }) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Eliminar seleccionados",
                tint = ScColors.alerta,
                modifier = Modifier.size(22.dp)
            )
        }
        IconButton(onClick = onCancelar) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Cancelar selección",
                tint = ScColors.textoSecundario,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            shape = Shape.Grande,
            containerColor = ScColors.panel,
            titleContentColor = ScColors.texto,
            textContentColor = ScColors.textoSecundario,
            title = {
                Text(
                    text = "¿Eliminar $count ${if (count == 1) "producto" else "productos"}?",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Los productos saldrán del catálogo de forma permanente. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEliminar()
                        confirmarEliminar = false
                    }
                ) {
                    Text(text = "Eliminar", color = ScColors.alerta)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) {
                    Text(text = "Cancelar", color = ScColors.textoSecundario)
                }
            }
        )
    }
}

// =====================================================================================
// FILTROS DE CATEGORÍA
// =====================================================================================

@Composable
private fun ChipsCategorias(
    categorias: List<Categoria>,
    seleccionada: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        // 20dp al inicio y 20dp al final: el último chip entra completo al llegar al borde.
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "categoria-todas") {
            ChipFiltro(
                texto = "Todos",
                seleccionado = seleccionada == null,
                onClick = { onSelect(null) }
            )
        }
        items(categorias, key = { it.id }) { categoria ->
            ChipFiltro(
                texto = categoria.nombre,
                seleccionado = seleccionada == categoria.id,
                onClick = { onSelect(categoria.id) }
            )
        }
    }
}

/** Pastilla de filtro: jade tenue + borde jade cuando está activa, panel si no. */
@Composable
private fun ChipFiltro(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
        shape = Shape.Full,
        color = if (seleccionado) ScColors.acento.copy(alpha = 0.13f) else ScColors.panel,
        contentColor = if (seleccionado) ScColors.acento else ScColors.textoSecundario,
        border = BorderStroke(1.dp, if (seleccionado) ScColors.acento else ScColors.borde)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

// =====================================================================================
// LISTA AGRUPADA POR CATEGORÍA
// =====================================================================================

private data class GrupoCatalogo(
    val id: String,
    val titulo: String,
    val productos: List<Producto>
)

private fun agruparPorCategoria(
    productos: List<Producto>,
    categorias: List<Categoria>
): List<GrupoCatalogo> {
    val porCategoria = productos.groupBy { it.categoriaId }
    val grupos = mutableListOf<GrupoCatalogo>()

    categorias.forEach { categoria ->
        val delGrupo = porCategoria[categoria.id].orEmpty()
        if (delGrupo.isNotEmpty()) {
            grupos.add(GrupoCatalogo(id = categoria.id, titulo = categoria.nombre, productos = delGrupo))
        }
    }

    val idsConocidos = categorias.map { it.id }.toSet()
    val sinCategoria = productos.filter { it.categoriaId !in idsConocidos }
    if (sinCategoria.isNotEmpty()) {
        grupos.add(GrupoCatalogo(id = "sin-categoria", titulo = "Sin categoría", productos = sinCategoria))
    }

    return grupos
}

@Composable
private fun CatalogoLista(
    state: ListaProductosUiState.Success,
    isSelectionMode: Boolean,
    onDetalle: (String) -> Unit,
    onToggleSelection: (String) -> Unit
) {
    val grupos = remember(state.productos, state.categorias) {
        agruparPorCategoria(state.productos, state.categorias)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        grupos.forEach { grupo ->
            item(key = "grupo-${grupo.id}") {
                PanelCatalogo(
                    grupo = grupo,
                    tasas = state.tasas,
                    monedaBase = state.monedaBase,
                    seleccionados = state.selectedProductIds,
                    isSelectionMode = isSelectionMode,
                    onProductoClick = { producto ->
                        if (isSelectionMode) onToggleSelection(producto.id) else onDetalle(producto.id)
                    },
                    onProductoLongClick = { producto -> onToggleSelection(producto.id) }
                )
            }
        }
    }
}

@Composable
private fun PanelCatalogo(
    grupo: GrupoCatalogo,
    tasas: Map<Moneda, Double>,
    monedaBase: Moneda,
    seleccionados: Set<String>,
    isSelectionMode: Boolean,
    onProductoClick: (Producto) -> Unit,
    onProductoLongClick: (Producto) -> Unit
) {
    ScPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = grupo.titulo,
                style = MaterialTheme.typography.labelMedium,
                color = ScColors.textoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${grupo.productos.size}",
                style = MaterialTheme.typography.labelSmall,
                color = ScColors.textoDebil,
                maxLines = 1
            )
        }
        ScDivider()
        grupo.productos.forEachIndexed { index, producto ->
            if (index > 0) {
                ScDivider()
            }
            FilaCatalogo(
                producto = producto,
                tasas = tasas,
                monedaBase = monedaBase,
                seleccionado = seleccionados.contains(producto.id),
                isSelectionMode = isSelectionMode,
                onClick = { onProductoClick(producto) },
                onLongClick = { onProductoLongClick(producto) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilaCatalogo(
    producto: Producto,
    tasas: Map<Moneda, Double>,
    monedaBase: Moneda,
    seleccionado: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val status = producto.stockStatus()
    val precio = producto.precioVenta.formatoAuto(
        moneda = producto.moneda,
        vinculado = producto.vincularTasa,
        tasa = tasas[producto.moneda] ?: 1.0,
        monedaBase = monedaBase
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (seleccionado) ScColors.acentoContenedor else ScColors.panel)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Icon(
                imageVector = if (seleccionado) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (seleccionado) ScColors.acento else ScColors.textoDebil,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
        }

        MiniaturaProducto(imagenUrl = producto.imagenUrl)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleSmall,
                color = ScColors.texto,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${producto.stockActual.formatoCantidad()} ${producto.unidadMedida.corta()} · mín ${producto.stockMinimo.formatoCantidad()}",
                style = MaterialTheme.typography.bodySmall,
                color = ScColors.textoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = precio,
                style = MaterialTheme.typography.titleSmall,
                color = ScColors.dinero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            ScStatusPill(
                text = status.etiquetaCatalogo(),
                tone = status.tonoCatalogo()
            )
        }
    }
}

@Composable
private fun MiniaturaProducto(imagenUrl: String?) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(Shape.Pequeno)
            .background(ScColors.panelHundido)
            .border(1.dp, ScColors.borde, Shape.Pequeno),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Image,
            contentDescription = null,
            tint = ScColors.textoDebil,
            modifier = Modifier.size(20.dp)
        )
        if (!imagenUrl.isNullOrBlank()) {
            AsyncImage(
                model = if (imagenUrl.startsWith("/")) java.io.File(imagenUrl) else imagenUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// =====================================================================================
// ACCIÓN FLOTANTE
// =====================================================================================

@Composable
private fun FabAgregarProducto(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = ScColors.acento,
        contentColor = ScColors.sobreAcento
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Agregar producto",
                tint = ScColors.sobreAcento,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

// =====================================================================================
// ESTADOS: CARGANDO, ERROR, VACÍO
// =====================================================================================

@Composable
private fun CatalogoCargando() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(Shape.Grande)
                    .background(ScColors.panel)
            )
        }
    }
}

@Composable
private fun CatalogoError(mensaje: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ScEmptyState(
            icon = Icons.Filled.Warning,
            title = "No se pudo cargar el catálogo",
            message = mensaje.ifBlank {
                "Revisa que la base de datos esté disponible y vuelve a abrir la pantalla."
            }
        )
    }
}

@Composable
private fun CatalogoVacio(
    isFiltered: Boolean,
    onClear: () -> Unit,
    onAgregar: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isFiltered) {
            ScEmptyState(
                icon = Icons.Filled.SearchOff,
                title = "Sin coincidencias",
                message = "Ningún producto coincide con la búsqueda o la categoría. Prueba con otro término o quita el filtro.",
                actionLabel = "Limpiar filtros",
                onAction = onClear
            )
        } else {
            ScEmptyState(
                icon = Icons.Filled.Inventory2,
                title = "El catálogo está vacío",
                message = "Agrega tu primer producto para venderlo y controlar su stock desde el mostrador.",
                actionLabel = "Agregar producto",
                onAction = onAgregar
            )
        }
    }
}

// =====================================================================================
// HELPERS DE PRESENTACIÓN
// =====================================================================================

/** Unidades compactas: la fila no necesita explicar "UNIDAD" completo. */
private fun UnidadMedida.corta(): String = when (this) {
    UnidadMedida.UNIDAD -> "ud"
    UnidadMedida.LIBRA -> "lb"
    UnidadMedida.KG -> "kg"
    UnidadMedida.LITRO -> "L"
    UnidadMedida.PAQUETE -> "paq"
}

/** El color comunica el estado: jade sano, ámbar atención, coral riesgo. */
private fun StockStatus.tonoCatalogo(): ScTone = when (this) {
    StockStatus.OK -> ScTone.Positive
    StockStatus.BAJO -> ScTone.Money
    StockStatus.SIN_STOCK -> ScTone.Danger
}

private fun StockStatus.etiquetaCatalogo(): String = when (this) {
    StockStatus.OK -> "OK"
    StockStatus.BAJO -> "Bajo"
    StockStatus.SIN_STOCK -> "Agotado"
}
