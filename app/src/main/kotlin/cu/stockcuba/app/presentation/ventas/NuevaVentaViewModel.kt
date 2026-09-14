package cu.stockcuba.app.presentation.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import cu.stockcuba.app.domain.model.*
import cu.stockcuba.app.domain.repository.ClienteRepository
import cu.stockcuba.app.domain.repository.ProductoRepository
import cu.stockcuba.app.domain.usecase.RegistrarVentaUseCase
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import cu.stockcuba.app.presentation.dashboard.formatoMoneda
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NuevaVentaViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val clienteRepository: ClienteRepository,
    private val categoriaRepository: cu.stockcuba.app.domain.repository.CategoriaRepository,
    private val registrarVentaUseCase: RegistrarVentaUseCase,
    private val ajustesDataStore: cu.stockcuba.app.presentation.ajustes.AjustesDataStore,
    private val pdfTicketService: cu.stockcuba.app.data.service.PdfTicketService,
    private val printerService: cu.stockcuba.app.data.service.BluetoothPrinterService,
    private val ventaRepository: cu.stockcuba.app.domain.repository.VentaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NuevaVentaUiState>(NuevaVentaUiState.empty)
    val uiState = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NuevaVentaUiState.empty)

    init {
        cargarDatosIniciales()
    }

    fun cargarDatosIniciales() {
        viewModelScope.launch {
            combine(
                ajustesDataStore.tasaUSD,
                ajustesDataStore.tasaMLC,
                ajustesDataStore.tasaEUR,
                ajustesDataStore.moneda,
                categoriaRepository.getAll(),
                productoRepository.getAll(),
                clienteRepository.getActivos()
            ) { params: Array<Any> ->
                val usd = params[0] as Double
                val mlc = params[1] as Double
                val eur = params[2] as Double
                val monedaBase = params[3] as Moneda
                val categorias = params[4] as List<Categoria>
                val productos = params[5] as List<Producto>
                val clientes = params[6] as List<Cliente>

                val tasasMap = mapOf(
                    Moneda.USD to usd,
                    Moneda.MLC to mlc,
                    Moneda.EUR to eur,
                    Moneda.CUP to 1.0
                )
                
                _uiState.update { state ->
                    if (state is NuevaVentaUiState.Editing) {
                        state.copy(
                            tasas = tasasMap,
                            monedaBase = monedaBase,
                            categorias = categorias,
                            productosDisponibles = productos.filter { it.activo && it.stockActual > 0 },
                            clientes = clientes.map { ClienteSimple(it.id, it.nombre, it.telefono) }
                        )
                    } else state
                }
            }.collect()
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

    fun buscarPorCodigoBarras(codigo: String) {
        val currentState = _uiState.value as? NuevaVentaUiState.Editing ?: return
        val producto = currentState.productosDisponibles.find { it.codigoBarras == codigo }
        if (producto != null) {
            agregarAlCarrito(producto)
        } else {
            setQuery(codigo)
        }
    }

    fun setSelectedCategory(categoryId: String?) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(selectedCategoryId = categoryId)
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

    fun setDescuento(valor: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(descuento = valor)
                else -> state
            }
        }
    }

    fun setTipoDescuento(isPorcentual: Boolean) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(isDescuentoPorcentual = isPorcentual)
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
                nuevoClienteCI = "",
                nuevoClienteTelefono = cliente.telefono ?: ""
            )
        }
        
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
                cargarDatosIniciales()
                setCliente(cliente.id)
                setShowNuevoClienteDialog(false)
            }
        }
    }

    fun agregarAlCarrito(producto: Producto) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> {
                    val existingIndex = state.carrito.indexOfFirst { it.producto.id == producto.id }
                    val newCarrito = state.carrito.toMutableList()

                    val precioCalculado = if (producto.vincularTasa) {
                        val tasa = state.tasas[producto.moneda] ?: 1.0
                        producto.precioVenta * tasa
                    } else {
                        null
                    }

                    if (existingIndex >= 0) {
                        val item = newCarrito[existingIndex]
                        if (item.cantidad < item.stockDisponible) {
                            newCarrito[existingIndex] = item.copy(cantidad = item.cantidad + 1, precioCalculado = precioCalculado)
                        }
                    } else {
                        if (producto.stockActual > 0) {
                            newCarrito.add(CarritoItem(producto = producto, cantidad = 1, precioCalculado = precioCalculado))
                        }
                    }
                    state.copy(carrito = newCarrito)
                }
                else -> state
            }
        }
    }

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

    fun decrementarCantidad(productoId: String) {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> {
                    val newCarrito = state.carrito.map { item ->
                        if (item.producto.id == productoId) {
                            if (item.cantidad > 1) {
                                item.copy(cantidad = item.cantidad - 1)
                            } else {
                                null
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

    fun limpiarCarrito() {
        _uiState.update { state ->
            when (state) {
                is NuevaVentaUiState.Editing -> state.copy(carrito = emptyList())
                else -> state
            }
        }
    }

    fun calcularTotales(state: NuevaVentaUiState.Editing): CarritoTotales {
        val base = state.carrito.sumOf { it.subtotal }
        val descValor = state.descuento.toDoubleOrNull() ?: 0.0
        val descCalculado = if (state.isDescuentoPorcentual) {
            base * (descValor / 100.0)
        } else {
            descValor
        }
        val final = (base - descCalculado).coerceAtLeast(0.0)
        return CarritoTotales(subtotal = base, total = final)
    }

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
        if (state.carrito.isEmpty()) errors["carrito"] = "El carrito está vacío"

        if (state.metodoPago == MetodoPago.MIXTO) {
            val efectivo = state.efectivoRecibido.toDoubleOrNull() ?: 0.0
            val transferencia = state.transferenciaMonto.toDoubleOrNull() ?: 0.0
            val total = calcularTotales(state).total
            if (kotlin.math.abs((efectivo + transferencia) - total) > 0.01) {
                errors["pagoMixto"] = "Suma incorrecta"
            }
        }
        return errors
    }

    private suspend fun construirVenta(state: NuevaVentaUiState.Editing): Venta {
        val items = state.carrito.map { item ->
            VentaItem(
                id = UUID.randomUUID().toString(),
                ventaId = "",
                productoId = item.producto.id,
                nombreProducto = item.producto.nombre,
                cantidad = item.cantidad,
                precioUnitario = item.precioUnitario,
                subtotal = item.subtotal
            )
        }

        val totales = calcularTotales(state)
        val nombreVendedor = if (ajustesDataStore.rolActual.first() == RolUsuario.DUENO) "Dueño" else ajustesDataStore.nombreVendedor.first()

        return Venta(
            id = UUID.randomUUID().toString(),
            fecha = java.time.Instant.now(),
            total = totales.total,
            totalOriginal = totales.subtotal,
            descuento = totales.subtotal - totales.total,
            metodoPago = state.metodoPago,
            items = items,
            clienteId = state.clienteId,
            vendedorNombre = nombreVendedor,
            montoEfectivo = if (state.metodoPago == MetodoPago.EFECTIVO) totales.total else 0.0,
            montoTransferencia = if (state.metodoPago == MetodoPago.TRANSFERENCIA) totales.total else 0.0,
            idTransferencia = state.idTransferencia
        )
    }

    suspend fun generarYCompartirTicket(ventaId: String): Uri? {
        val ventaResult = ventaRepository.getByIdSync(ventaId)
        if (ventaResult is Result.Success) {
            val itemsResult = ventaRepository.getItemsByVentaId(ventaId)
            val fullVenta = if (itemsResult is Result.Success) {
                ventaResult.value.copy(items = itemsResult.value)
            } else ventaResult.value
            
            val nombreNegocio = ajustesDataStore.nombreNegocio.first()
            return pdfTicketService.generarTicketPDF(fullVenta, nombreNegocio)
        }
        return null
    }

    suspend fun imprimirTicket(ventaId: String): Result<Unit> {
        val printerMac = ajustesDataStore.printerMac.first() ?: return Result.Failure(DomainError.Unknown("No hay impresora configurada", null))
        val ventaResult = ventaRepository.getByIdSync(ventaId)
        if (ventaResult is Result.Success) {
            val itemsResult = ventaRepository.getItemsByVentaId(ventaId)
            val fullVenta = if (itemsResult is Result.Success) {
                ventaResult.value.copy(items = itemsResult.value)
            } else ventaResult.value
            
            val nombreNegocio = ajustesDataStore.nombreNegocio.first()
            return printerService.printTicket(printerMac, fullVenta, nombreNegocio)
        }
        return Result.Failure(DomainError.NotFound("Venta", ventaId))
    }
}
