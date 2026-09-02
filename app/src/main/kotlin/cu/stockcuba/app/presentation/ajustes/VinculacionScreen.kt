package cu.stockcuba.app.presentation.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VinculacionScreen(
    onBack: () -> Unit,
    viewModel: VinculacionViewModel = hiltViewModel()
) {
    val state by viewModel.businessInfo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var businessIdInput by remember { mutableStateOf("") }
    var posNombreInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Vinculación de Negocio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(StockCubaSpacing.Lg)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
        ) {
            if (state.isVinculado) {
                EstadoVinculado(
                    businessId = state.businessId,
                    posId = state.posId,
                    onDesvincular = { viewModel.desvincular() }
                )
            } else {
                FormularioVinculacion(
                    businessId = businessIdInput,
                    onBusinessIdChange = { businessIdInput = it },
                    posNombre = posNombreInput,
                    onPosNombreChange = { posNombreInput = it },
                    onVincular = { viewModel.vincular(businessIdInput, posNombreInput) },
                    isLoading = isLoading,
                    error = error
                )
            }
        }
    }
}

@Composable
fun FormularioVinculacion(
    businessId: String,
    onBusinessIdChange: (String) -> Unit,
    posNombre: String,
    onPosNombreChange: (String) -> Unit,
    onVincular: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(StockCubaSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
        ) {
            Icon(
                Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
            )
            Text(
                "Vincular Dispositivo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                "Ingresa el ID de tu negocio para sincronizar las ventas y centralizar tu facturación.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(StockCubaSpacing.Md))

            OutlinedTextField(
                value = businessId,
                onValueChange = onBusinessIdChange,
                label = { Text("ID del Negocio") },
                modifier = Modifier.fillMaxWidth(),
                shape = Shape.Grande,
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) }
            )

            OutlinedTextField(
                value = posNombre,
                onValueChange = onPosNombreChange,
                label = { Text("Nombre de este Punto de Venta") },
                placeholder = { Text("Ej: Sucursal Central") },
                modifier = Modifier.fillMaxWidth(),
                shape = Shape.Grande,
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
            )

            if (error != null) {
                Text(error, color = StockCubaColors.CoralAlerta, style = MaterialTheme.typography.labelSmall)
            }

            Button(
                onClick = onVincular,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = Shape.Grande,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Vincular Ahora")
                }
            }
        }
    }
}

@Composable
fun EstadoVinculado(
    businessId: String,
    posId: String,
    onDesvincular: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = StockCubaColors.VerdeExito.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(StockCubaSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = StockCubaColors.VerdeExito,
                modifier = Modifier.size(64.dp)
            )
            Text("Dispositivo Vinculado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            InfoRow(label = "ID de Negocio", value = businessId)
            InfoRow(label = "ID de Punto de Venta", value = posId)

            Spacer(Modifier.height(StockCubaSpacing.Lg))

            OutlinedButton(
                onClick = onDesvincular,
                modifier = Modifier.fillMaxWidth(),
                shape = Shape.Grande,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StockCubaColors.CoralAlerta)
            ) {
                Text("Desvincular este Dispositivo")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
