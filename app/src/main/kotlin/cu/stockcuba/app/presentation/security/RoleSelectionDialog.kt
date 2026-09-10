package cu.stockcuba.app.presentation.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cu.stockcuba.app.domain.model.RolUsuario
import cu.stockcuba.app.presentation.theme.Shape
import cu.stockcuba.app.presentation.theme.StockCubaColors
import cu.stockcuba.app.presentation.theme.StockCubaSpacing

/**
 * Dialog for initial role selection or switching turns (T39).
 */
@Composable
fun RoleSelectionDialog(
    onRoleSelected: (RolUsuario, String) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var selectedRol by remember { mutableStateOf<RolUsuario?>(null) }
    var nombreVendedor by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1: Select Role, 2: Enter Name (if Vendedor)

    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(dismissOnBackPress = onDismiss != null, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StockCubaSpacing.Md),
            shape = Shape.Grande,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(StockCubaSpacing.Lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StockCubaSpacing.Lg)
            ) {
                Text(
                    text = if (step == 1) "¿Quién eres hoy?" else "Identificación",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                if (step == 1) {
                    RoleOptionCard(
                        title = "Dueño",
                        description = "Acceso total a finanzas y costos",
                        icon = Icons.Default.AdminPanelSettings,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            selectedRol = RolUsuario.DUENO
                            onRoleSelected(RolUsuario.DUENO, "")
                        }
                    )

                    RoleOptionCard(
                        title = "Vendedor",
                        description = "Acceso a ventas e inventario",
                        icon = Icons.Default.Storefront,
                        color = Color(0xFF6366F1),
                        onClick = {
                            selectedRol = RolUsuario.VENDEDOR
                            step = 2
                        }
                    )
                } else {
                    Text(
                        text = "Introduce tu nombre para registrar tus ventas en el turno.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = nombreVendedor,
                        onValueChange = { nombreVendedor = it },
                        label = { Text("Nombre del Vendedor") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shape.Grande,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(StockCubaSpacing.Md)
                    ) {
                        TextButton(
                            onClick = { step = 1 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Atrás")
                        }
                        Button(
                            onClick = {
                                if (nombreVendedor.isNotBlank()) {
                                    onRoleSelected(RolUsuario.VENDEDOR, nombreVendedor)
                                }
                            },
                            enabled = nombreVendedor.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = Shape.Grande
                        ) {
                            Text("Entrar")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = Shape.Grande,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(StockCubaSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = Shape.Full,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(StockCubaSpacing.Lg))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
