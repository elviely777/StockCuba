package cu.stockcuba.app.presentation.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.domain.model.Categoria
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionCategoriasScreen(
    onBack: () -> Unit,
    viewModel: GestionCategoriasViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var categoriaParaEditar by remember { mutableStateOf<Categoria?>(null) }
    var showErrorDialog by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Categorías", fontWeight = FontWeight.Bold) },
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
                Icon(Icons.Default.Add, contentDescription = "Añadir Categoría")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(StockCubaSpacing.Lg),
                    verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
                ) {
                    items(uiState.categorias, key = { it.id }) { categoria ->
                        CategoriaItem(
                            categoria = categoria,
                            onEdit = { categoriaParaEditar = it },
                            onDelete = { 
                                viewModel.eliminarCategoria(categoria.id) { count ->
                                    showErrorDialog = count
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            CategoriaDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { nombre, color ->
                    viewModel.crearCategoria(nombre, color)
                    showAddDialog = false
                }
            )
        }

        if (categoriaParaEditar != null) {
            CategoriaDialog(
                categoria = categoriaParaEditar,
                onDismiss = { categoriaParaEditar = null },
                onConfirm = { nombre, color ->
                    viewModel.actualizarCategoria(categoriaParaEditar!!.copy(nombre = nombre, color = color))
                    categoriaParaEditar = null
                }
            )
        }

        if (showErrorDialog != null) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = null },
                title = { Text("No se puede eliminar") },
                text = { Text("Esta categoría tiene $showErrorDialog productos vinculados. Debes moverlos a otra categoría o eliminarlos antes de borrar la categoría.") },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = null }) { Text("Entendido") }
                }
            )
        }
    }
}

@Composable
fun CategoriaItem(
    categoria: Categoria,
    onEdit: (Categoria) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(StockCubaSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(categoria.color))
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = categoria.nombre,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onEdit(categoria) }) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }
            if (categoria.id != "otros") {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CategoriaDialog(
    categoria: Categoria? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var nombre by remember { mutableStateOf(categoria?.nombre ?: "") }
    var selectedColor by remember { mutableStateOf(categoria?.color?.let { Color(it) } ?: Color(0xFF6366F1)) }

    val colores = listOf(
        Color(0xFF6366F1), Color(0xFFF44336), Color(0xFF4CAF50), 
        Color(0xFFFF9800), Color(0xFF2196F3), Color(0xFF9C27B0),
        Color(0xFFE91E63), Color(0xFF00BCD4), Color(0xFF795548)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (categoria == null) "Nueva Categoría" else "Editar Categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shape.Grande,
                    singleLine = true
                )
                
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colores.take(5).forEach { color ->
                        ColorCircle(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colores.drop(5).forEach { color ->
                        ColorCircle(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nombre, selectedColor.toArgb()) },
                enabled = nombre.isNotBlank(),
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
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
