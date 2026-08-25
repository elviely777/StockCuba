package cu.stockcuba.app.presentation.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cu.stockcuba.app.domain.model.Cliente
import cu.stockcuba.app.domain.model.MetodoPago
import cu.stockcuba.app.domain.model.Producto
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.domain.model.VentaItem
import cu.stockcuba.app.domain.repository.ClienteRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import cu.stockcuba.app.domain.usecase.RegistrarVentaUseCase
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NuevaVentaViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val clienteRepository: ClienteRepository,
    private val registrarVentaUseCase: RegistrarVentaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NuevaVentaUiState>(NuevaVentaUiState.empty)
    val uiState = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NuevaVentaUiState.empty)

    init {
        cargarDatosIniciales()
    }

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            // Cargar productos activos
            productoRepository.getAll().firstOrNull()?.let { productos ->
                _uiState.update { state ->
                    when (state) {
                        is NuevaVentaUiState.Editing -> state.copy(productosDisponibles = productos.filter { it.activo && it.stockActual > 0 })
                        else -> state
                    }
                }
            }

            // Cargar clientes
            clienteRepository.getActivos().firstOrNull()?.let { clientes ->
                _uiState.update { state ->
                    when (state) {
                        is NuevaVentaUiState.Editing -> state.copy(
                            clientes = clientes.map { ClienteSimple(it.id, it.nombre, it.telefono) }
                        )
                        else -> state
                    }
                }
            }
        }
    }

    fun setQuery(query: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(query = query)
                else -> state
            }
        }
    }

    fun setMetodoPago(metodo: MetodoPago) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(metodoPago = metodo)
                else -> state
            }
        }
    }

    fun setEfectivoRecibido(monto: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(
                    efectivoRecibido = monto,
                    errors = state.errors - "efectivo"
                )
                else -> state
            }
        }
    }

    fun setTransferenciaMonto(monto: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(
                    transferenciaMonto = monto,
                    errors = state.errors - "transferencia"
                )
                else -> state
            }
        }
    }

    fun setIdTransferencia(id: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(idTransferencia = id)
                else -> state
            }
        }
    }

    fun setMontoExacto() {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> {
                    val total = CarritoTotales.calcular(state.carrito).total
                    state.copy(
                        efectivoRecibido = total.toString(),
                        errors = state.errors - "efectivo"
                    )
                }
                else -> state
            }
        }
    }

    fun setCliente(clienteId: String?) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(clienteId = clienteId)
                else -> state
            }
        }
    }

    fun setShowNuevoClienteDialog(show: Boolean) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(
                    showNuevoClienteDialog = show,
                    editingClienteId = if (!show) null else state.editingClienteId,
                    nuevoClienteNombre = if (!show) "" else state.nuevoClienteNombre,
                    nuevoClienteCI = if (!show) "" else state.nuevoClienteCI,
                    nuevoClienteTelefono = if (!show) "" else state.nuevoClienteTelefono
                )
                else -> state
            }
        }
    }

    fun startEditingCliente(clienteId: String) {
        val currentState = _uiState.value as? NuevaVentaUiState.Editing ?: return
        val cliente = currentState.clientes.find { it.id == clienteId } ?: return

        _uiState.update { state ->
            (state as NuevaVentaUiState.Editing).copy(
                showNuevoClienteDialog = true,
                editingClienteId = clienteId,
                nuevoClienteNombre = cliente.nombre,
                nuevoClienteCI = "", // El CI no está en ClienteSimple, tendré que buscarlo o ignorarlo
                nuevoClienteTelefono = cliente.telefono ?: ""
            )
        }
        
        // Cargar el CI real desde el repositorio
        viewModelScope.launch {
            clienteRepository.getByIdSync(clienteId).onSuccess { fullCliente ->
                _uiState.update { state ->
                    (state as NuevaVentaUiState.Editing).copy(
                        nuevoClienteCI = fullCliente.ci
                    )
                }
            }
        }
    }

    fun updateNuevoClienteNombre(nombre: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(nuevoClienteNombre = nombre)
                else -> state
            }
        }
    }

    fun updateNuevoClienteCI(ci: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(nuevoClienteCI = ci)
                else -> state
            }
        }
    }

    fun updateNuevoClienteTelefono(telefono: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(nuevoClienteTelefono = telefono)
                else -> state
            }
        }
    }

    fun guardarNuevoCliente() {
        val currentState = _uiState.value as? NuevaVentaUiState.Editing ?: return
        if (currentState.nuevoClienteNombre.isBlank() || currentState.nuevoClienteCI.isBlank()) return

        viewModelScope.launch {
            val cliente = Cliente(
                id = currentState.editingClienteId ?: UUID.randomUUID().toString(),
                nombre = currentState.nuevoClienteNombre.trim(),
                ci = currentState.nuevoClienteCI.trim(),
                telefono = currentState.nuevoClienteTelefono.trim().takeIf { it.isNotBlank() },
                notas = if (currentState.editingClienteId != null) "Editado desde Nueva Venta" else "Registrado desde Nueva Venta"
            )
            
            val result = if (currentState.editingClienteId != null) {
                clienteRepository.update(cliente)
            } else {
                clienteRepository.insert(cliente)
            }

            result.onSuccess {
                // Actualizar lista de clientes y seleccionar el nuevo
                cargarDatosIniciales()
                setCliente(cliente.id)
                setShowNuevoClienteDialog(false)
            }
        }
    }

    /**
     * Agrega producto al carrito o incrementa cantidad.
     */
    fun agregarAlCarrito(producto: Producto) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> {
                    val existingIndex = state.carrito.indexOfFirst { it.producto.id == producto.id }
                    val newCarrito = state.carrito.toMutableList()

                    if (existingIndex >= 0) {
                        val item = newCarrito[existingIndex]
                        if (item.cantidad < item.stockDisponible) {
                            newCarrito[existingIndex] = item.copy(cantidad = item.cantidad + 1)
                        }
                    } else {
                        if (producto.stockActual > 0) {
                            newCarrito.add(CarritoItem(producto = producto, cantidad = 1))
                        }
                    }
                    state.copy(carrito = newCarrito)
                }
                else -> state
            }
        }
    }

    /**
     * Incrementa cantidad de un item en el carrito.
     */
    fun incrementarCantidad(productoId: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> {
                    val newCarrito = state.carrito.map { item ->
                        if (item.producto.id == productoId && item.puedeAumentar) {
                            item.copy(cantidad = item.cantidad + 1)
                        } else {
                            item
                        }
                    }
                    state.copy(carrito = newCarrito)
                }
                else -> state
            }
        }
    }

    /**
     * Decrementa cantidad de un item en el carrito.
     */
    fun decrementarCantidad(productoId: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> {
                    val newCarrito = state.carrito.map { item ->
                        if (item.producto.id == productoId) {
                            if (item.cantidad > 1) {
                                item.copy(cantidad = item.cantidad - 1)
                            } else {
                                null // Se eliminará en el filter
                            }
                        } else {
                            item
                        }
                    }.filterNotNull()
                    state.copy(carrito = newCarrito)
                }
                else -> state
            }
        }
    }

    /**
     * Elimina item del carrito.
     */
    fun eliminarDelCarrito(productoId: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> {
                    state.copy(carrito = state.carrito.filter { it.producto.id != productoId })
                }
                else -> state
            }
        }
    }

    /**
     * Limpiar carrito completo.
     */
    fun limpiarCarrito() {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(carrito = emptyList())
                else -> state
            }
        }
    }

    /**
     * Calcula totales del carrito.
     */
    fun calcularTotales(carrito: List<CarritoItem>): CarritoTotales {
        return CarritoTotales.calcular(carrito)
    }

    /**
     * Valida y confirma la venta.
     */
    fun confirmarVenta() {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> {
                    val errors = validarVenta(state)
                    if (errors.isNotEmpty()) {
                        state.copy(errors = errors)
                    } else {
                        state.copy(isLoading = true, errors = emptyMap())
                    }
                }
                else -> state
            }
        }

        viewModelScope.launch {
            val currentState = _uiState.value
            when (currentState) {
                is NuevaVentaUiState.Editing -> {
                    if (currentState.errors.isEmpty() && currentState.carrito.isNotEmpty()) {
                        val venta = construirVenta(currentState)
                        val result = registrarVentaUseCase(venta)

                        _uiState.update { _ ->
                            when (result) {
                                is Result.Success -> NuevaVentaUiState.Saved(venta.id)
                                is Result.Failure -> NuevaVentaUiState.Error(result.error.toString())
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private fun validarVenta(state: NuevaVentaUiState.Editing): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (state.carrito.isEmpty()) {
            errors["carrito"] = "El carrito está vacío"
        }

        // Validar método de pago mixto
        if (state.metodoPago == MetodoPago.MIXTO) {
            val efectivo = state.efectivoRecibido.toDoubleOrNull() ?: 0.0
            val transferencia = state.transferenciaMonto.toDoubleOrNull() ?: 0.0
            val total = CarritoTotales.calcular(state.carrito).total

            if (efectivo + transferencia != total) {
                errors["pagoMixto"] = "La suma de efectivo y transferencia debe ser igual al total"
            }
            if (efectivo < 0 || transferencia < 0) {
                errors["pagoMixto"] = "Los montos no pueden ser negativos"
            }
        }

        // Validar efectivo recibido
        if (state.metodoPago == MetodoPago.EFECTIVO || state.metodoPago == MetodoPago.MIXTO) {
            val efectivo = state.efectivoRecibido.toDoubleOrNull() ?: 0.0
            val total = CarritoTotales.calcular(state.carrito).total
            val montoEsperado = if (state.metodoPago == MetodoPago.EFECTIVO) total else (state.efectivoRecibido.toDoubleOrNull() ?: 0.0)

            if (state.metodoPago == MetodoPago.EFECTIVO && efectivo < total) {
                errors["efectivo"] = "Monto insuficiente (Faltan ${(total - efectivo).formatoCUP()})"
            }
        }

        return errors
    }

    private fun construirVenta(state: NuevaVentaUiState.Editing): Venta {
        val items = state.carrito.map { item ->
            VentaItem(
                id = UUID.randomUUID().toString(),
                ventaId = "", // Se llenará en el use case
                productoId = item.producto.id,
                nombreProducto = item.producto.nombre,
                cantidad = item.cantidad,
                precioUnitario = item.precioUnitario,
                subtotal = item.subtotal
            )
        }

        val total = CarritoTotales.calcular(state.carrito).total

        val montoEfectivo = when(state.metodoPago) {
            MetodoPago.EFECTIVO -> total
            MetodoPago.MIXTO -> state.efectivoRecibido.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

        val montoTransferencia = when(state.metodoPago) {
            MetodoPago.TRANSFERENCIA -> total
            MetodoPago.MIXTO -> state.transferenciaMonto.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

        return Venta(
            id = UUID.randomUUID().toString(),
            fecha = java.time.Instant.now(),
            total = total,
            metodoPago = state.metodoPago,
            items = items,
            clienteId = state.clienteId,
            montoEfectivo = montoEfectivo,
            montoTransferencia = montoTransferencia,
            idTransferencia = state.idTransferencia.takeIf { it.isNotBlank() }
        )
    }

    fun resetVenta() {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(
                    carrito = emptyList(),
                    query = "",
                    metodoPago = MetodoPago.EFECTIVO,
                    efectivoRecibido = "",
                    transferenciaMonto = "",
                    idTransferencia = "",
                    clienteId = null,
                    showSuccess = false,
                    errors = emptyMap()
                )
                else -> NuevaVentaUiState.empty
            }
        }
    }
}