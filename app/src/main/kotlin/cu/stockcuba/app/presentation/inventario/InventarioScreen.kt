package cu.stockcuba.app.presentation.inventario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.UnidadMedida
import cu.stockcuba.app.presentation.common.ScEmptyState
import cu.stockcuba.app.presentation.common.ScLevelBar
import cu.stockcuba.app.presentation.common.ScMetricCell
import cu.stockcuba.app.presentation.common.ScPanel
import cu.stockcuba.app.presentation.common.ScScreenHeader
import cu.stockcuba.app.presentation.common.ScSearchField
import cu.stockcuba.app.presentation.common.ScStatusPill
import cu.stockcuba.app.presentation.common.ScTone
import cu.stockcuba.app.presentation.dashboard.formatoCantidad
import cu.stockcuba.app.presentation.theme.ScColors
import cu.stockcuba.app.presentation.theme.Shape

/**
 * Inventario · sistema "Mostrador".
 *
 * El encargo: leer el estado del almacén de un vistazo. La franja superior es un
 * solo panel con tres celdas separadas por divisores verticales (OK / Bajo /
 * Crítico) en vez de tres tarjetas de colores; los filtros son chips; y cada
 * producto es una tarjeta compacta con su barra de nivel contra el mínimo.
 *
 * Misma lógica que antes: ViewModel, filtros, callbacks de ajuste e historial.
 *
 * Nota: Inventario es un destino del dock inferior, así que la cabecera no
 * dibuja flecha de volver. El parámetro [onBack] se conserva porque el NavHost
 * lo sigue pasando (y el gesto de atrás del sistema sigue funcionando).
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun InventarioScreen(
    onAjuste: (Producto) -> Unit,
    onHistorial: (Producto) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: InventarioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState as? InventarioUiState.Success
    val totalArticulos = state?.totalArticulos ?: 0
    val filtrando = state != null && (state.query.isNotBlank() || state.filtroStock != FiltroStock.TODOS)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScColors.canvas)
    ) {
        ScScreenHeader(
            title = "Inventario",
            subtitle = when {
                state == null -> null
                filtrando -> "${state.productos.size} de $totalArticulos ${if (totalArticulos == 1) "producto" else "productos"}"
                else -> "$totalArticulos ${if (totalArticulos == 1) "producto" else "productos"} en el catálogo"
            }
        )

        ScSearchField(
            value = state?.query.orEmpty(),
            onValueChange = { viewModel.setQuery(it) },
            placeholder = "Buscar producto",
            onClear = { viewModel.setQuery("") },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        when (val s = uiState) {
            is InventarioUiState.Loading -> {
                InventarioCargando(modifier = Modifier.fillMaxWidth().weight(1f))
            }

            is InventarioUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    InventarioError(
                        mensaje = s.message,
                        onRetry = { viewModel.limpiarFiltros() }
                    )
                }
            }

            is InventarioUiState.Success -> {
                Spacer(Modifier.height(8.dp))

                ResumenStock(
                    ok = s.totalOk,
                    bajo = s.totalBajo,
                    sin = s.totalSinStock,
                    onFiltro = { viewModel.setFiltroStock(it) }
                )

                ChipsFiltroStock(
                    filtroActual = s.filtroStock,
                    onFiltroChange = { viewModel.setFiltroStock(it) }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (s.productos.isEmpty()) {
                        InventarioVacio(
                            isFiltered = s.query.isNotEmpty() || s.filtroStock != FiltroStock.TODOS,
                            onClear = { viewModel.limpiarFiltros() }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(s.productos, key = { it.producto.id }) { item ->
                                TarjetaStock(
                                    item = item,
                                    onAjuste = { onAjuste(item.producto) },
                                    onHistorial = { onHistorial(item.producto) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================================
// FRANJA DE MÉTRICAS (UN SOLO PANEL CON DIVISORES)
// =====================================================================================

@Composable
private fun ResumenStock(
    ok: Int,
    bajo: Int,
    sin: Int,
    onFiltro: (FiltroStock) -> Unit
) {
    ScPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            ScMetricCell(
                label = "OK",
                value = ok.toString(),
                caption = "productos",
                tone = ScTone.Positive,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onFiltro(FiltroStock.OK) }
            )
            DivisorVertical()
            ScMetricCell(
                label = "Bajo",
                value = bajo.toString(),
                caption = "productos",
                tone = ScTone.Money,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onFiltro(FiltroStock.BAJO) }
            )
            DivisorVertical()
            ScMetricCell(
                label = "Crítico",
                value = sin.toString(),
                caption = "productos",
                tone = ScTone.Danger,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onFiltro(FiltroStock.SIN_STOCK) }
            )
        }
    }
}

@Composable
private fun DivisorVertical() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(ScColors.borde)
    )
}

// =====================================================================================
// FILTROS COMO CHIPS
// =====================================================================================

@Composable
private fun ChipsFiltroStock(
    filtroActual: FiltroStock,
    onFiltroChange: (FiltroStock) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        // 20dp al inicio y 20dp al final: el último chip entra completo al llegar al borde.
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FiltroStock.entries) { filtro ->
            ChipFiltro(
                texto = filtro.etiquetaFiltro(),
                seleccionado = filtroActual == filtro,
                onClick = { onFiltroChange(filtro) }
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
// TARJETA DE PRODUCTO CON BARRA DE NIVEL
// =====================================================================================

@Composable
private fun TarjetaStock(
    item: ProductoConStock,
    onAjuste: () -> Unit,
    onHistorial: () -> Unit
) {
    val producto = item.producto
    val tone = item.stockStatus.tonoStock()
    // La barra expresa la existencia contra el mínimo: el máximo son 2× el mínimo.
    val progreso = (item.porcentajeStock / 2f).coerceIn(0f, 1f)
    val colorCifra = when (item.stockStatus) {
        StockStatus.SIN_STOCK -> ScColors.alerta
        StockStatus.BAJO -> ScColors.dinero
        StockStatus.OK -> ScColors.texto
    }

    ScPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = ScColors.texto,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                ScStatusPill(
                    text = item.stockStatus.etiquetaStock(),
                    tone = tone
                )
                MenuProducto(
                    onAjuste = onAjuste,
                    onHistorial = onHistorial
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.stockDisponible.formatoCantidad(),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorCifra,
                    maxLines = 1
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = producto.unidadMedida.corta(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ScColors.textoDebil,
                    maxLines = 1
                )
                Spacer(Modifier.width(14.dp))
                ScLevelBar(
                    progress = progreso,
                    tone = tone,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "mín ${item.stockMinimo}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScColors.textoDebil,
                    maxLines = 1
                )
            }
        }
    }
}

/** Menú discreto por producto: mantiene ajuste e historial sin dos botones enormes. */
@Composable
private fun MenuProducto(
    onAjuste: () -> Unit,
    onHistorial: () -> Unit
) {
    var abierto by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { abierto = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Acciones del producto",
                tint = ScColors.textoDebil,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Ajustar existencias",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ScColors.texto
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.EditCalendar,
                        contentDescription = null,
                        tint = ScColors.textoSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    abierto = false
                    onAjuste()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Ver historial",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ScColors.texto
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = ScColors.textoSecundario,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    abierto = false
                    onHistorial()
                }
            )
        }
    }
}

// =====================================================================================
// ESTADOS: CARGANDO, ERROR, VACÍO
// =====================================================================================

@Composable
private fun InventarioCargando(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(Shape.Grande)
                .background(ScColors.panel)
        )
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(Shape.Grande)
                    .background(ScColors.panel)
            )
        }
    }
}

@Composable
private fun InventarioError(mensaje: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ScEmptyState(
            icon = Icons.Filled.Warning,
            title = "No se pudo cargar el inventario",
            message = mensaje.ifBlank {
                "Vuelve a abrir la pantalla para reintentar la carga del almacén."
            },
            actionLabel = "Reintentar",
            onAction = onRetry
        )
    }
}

@Composable
private fun InventarioVacio(isFiltered: Boolean, onClear: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isFiltered) {
            ScEmptyState(
                icon = Icons.Filled.SearchOff,
                title = "Sin resultados",
                message = "Ningún producto coincide con la búsqueda o el filtro de stock. Ajusta los filtros para ver el almacén completo.",
                actionLabel = "Limpiar filtros",
                onAction = onClear
            )
        } else {
            ScEmptyState(
                icon = Icons.Filled.Inventory2,
                title = "Sin productos activos",
                message = "El catálogo no tiene productos activos. Agrégalos desde Productos y aquí verás su existencia contra el mínimo."
            )
        }
    }
}

// =====================================================================================
// HELPERS DE PRESENTACIÓN
// =====================================================================================

private fun FiltroStock.etiquetaFiltro(): String = when (this) {
    FiltroStock.TODOS -> "Todos"
    FiltroStock.OK -> "Existencia OK"
    FiltroStock.BAJO -> "Stock bajo"
    FiltroStock.SIN_STOCK -> "Agotados"
}

/** El color comunica el estado: jade sano, ámbar atención, coral riesgo. */
private fun StockStatus.tonoStock(): ScTone = when (this) {
    StockStatus.OK -> ScTone.Positive
    StockStatus.BAJO -> ScTone.Money
    StockStatus.SIN_STOCK -> ScTone.Danger
}

private fun StockStatus.etiquetaStock(): String = when (this) {
    StockStatus.OK -> "OK"
    StockStatus.BAJO -> "Bajo"
    StockStatus.SIN_STOCK -> "Agotado"
}

/** Unidades compactas para no romper la fila de cifra + barra + mínimo. */
private fun UnidadMedida.corta(): String = when (this) {
    UnidadMedida.UNIDAD -> "ud"
    UnidadMedida.LIBRA -> "lb"
    UnidadMedida.KG -> "kg"
    UnidadMedida.LITRO -> "L"
    UnidadMedida.PAQUETE -> "paq"
}
