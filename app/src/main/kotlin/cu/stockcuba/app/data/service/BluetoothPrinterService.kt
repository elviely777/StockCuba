package cu.stockcuba.app.data.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.model.DomainError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothPrinterService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return emptyList()
        return adapter.bondedDevices.toList()
    }

    @SuppressLint("MissingPermission")
    suspend fun printTicket(deviceAddress: String, venta: Venta, nombreNegocio: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter ?: return@withContext Result.Failure(DomainError.Unknown("Bluetooth no disponible", null))
            val device = adapter.getRemoteDevice(deviceAddress)

            conectar(device)
            
            val out = outputStream ?: return@withContext Result.Failure(DomainError.Unknown("No hay flujo de salida", null))

            // --- DISEÑO ESC/POS (58mm) ---
            val center = byteArrayOf(0x1B, 0x61, 0x01)
            val left = byteArrayOf(0x1B, 0x61, 0x00)
            val boldOn = byteArrayOf(0x1B, 0x45, 0x01)
            val boldOff = byteArrayOf(0x1B, 0x45, 0x00)
            val init = byteArrayOf(0x1B, 0x40)

            out.write(init)
            
            // Header
            out.write(center)
            out.write(boldOn)
            out.write("${nombreNegocio}\n".toByteArray())
            out.write(boldOff)
            
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a").withZone(ZoneId.systemDefault())
            out.write("${formatter.format(venta.fecha)}\n".toByteArray())
            out.write("Ticket: #${venta.id.take(8).uppercase()}\n".toByteArray())
            out.write("--------------------------------\n".toByteArray()) // 32 chars for 58mm
            
            // Items
            out.write(left)
            venta.items.forEach { item ->
                val line = "${item.nombreProducto.take(20).padEnd(20)} ${item.cantidad.toString().padStart(3)} ${item.subtotal.toInt().toString().padStart(7)}\n"
                out.write(line.toByteArray())
            }
            
            out.write("--------------------------------\n".toByteArray())
            
            // Totales
            out.write(boldOn)
            val totalLine = "TOTAL:".padEnd(20) + venta.total.formatoCUP().padStart(12) + "\n"
            out.write(totalLine.toByteArray())
            out.write(boldOff)
            
            if (venta.descuento > 0) {
                out.write("Descuento: -${venta.descuento.formatoCUP()}\n".toByteArray())
            }
            
            out.write("\n".toByteArray())
            out.write(center)
            out.write("¡Gracias por su compra!\n".toByteArray())
            out.write("\n\n\n\n".toByteArray()) // Feed for manual tear

            desconectar()
            Result.Success(Unit)
        } catch (e: Exception) {
            desconectar()
            Result.Failure(DomainError.Unknown("Error de impresión: ${e.message}", e))
        }
    }

    @SuppressLint("MissingPermission")
    private fun conectar(device: BluetoothDevice) {
        try {
            desconectar()
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
        } catch (e: IOException) {
            android.util.Log.e("Printer", "Conexión fallida", e)
            desconectar()
        }
    }

    private fun desconectar() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            // Ignore
        } finally {
            outputStream = null
            bluetoothSocket = null
        }
    }
}
