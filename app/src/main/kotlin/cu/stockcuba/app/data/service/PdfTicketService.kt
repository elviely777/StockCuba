package cu.stockcuba.app.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import cu.stockcuba.app.domain.model.Venta
import cu.stockcuba.app.presentation.dashboard.formatoCUP
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfTicketService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun generarTicketPDF(venta: Venta, nombreNegocio: String): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var y = 30f
        
        // Logo
        try {
            val iconId = cu.stockcuba.app.R.mipmap.ic_launcher
            val bitmap = BitmapFactory.decodeResource(context.resources, iconId)
            if (bitmap != null) {
                val size = 50
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
                canvas.drawBitmap(scaledBitmap, 125f, y, paint)
                y += size + 20f
            }
        } catch (e: Exception) {
            y += 10f
        }

        // Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(nombreNegocio, 150f, y, paint)
        
        y += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a").withZone(ZoneId.systemDefault())
        canvas.drawText(formatter.format(venta.fecha), 150f, y, paint)

        y += 20f
        canvas.drawText("Ticket: #${venta.id.take(8).uppercase()}", 150f, y, paint)
        
        y += 30f
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Producto", 20f, y, paint)
        canvas.drawText("Cant.", 180f, y, paint)
        canvas.drawText("Total", 230f, y, paint)
        
        y += 10f
        paint.strokeWidth = 1f
        canvas.drawLine(20f, y, 280f, y, paint)
        
        y += 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        venta.items.forEach { item ->
            canvas.drawText(item.nombreProducto.take(20), 20f, y, paint)
            canvas.drawText(item.cantidad.toString(), 180f, y, paint)
            canvas.drawText(item.subtotal.toInt().toString(), 230f, y, paint)
            y += 15f
        }
        
        y += 10f
        canvas.drawLine(20f, y, 280f, y, paint)
        
        y += 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("TOTAL:", 20f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(venta.total.formatoCUP(), 280f, y, paint)
        
        if (venta.descuento > 0) {
            y += 15f
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Descuento: -${venta.descuento.formatoCUP()}", 280f, y, paint)
        }

        y += 40f
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("¡Gracias por su compra!", 150f, y, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "ticket_${venta.id.take(8)}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            android.util.Log.e("PdfService", "Error writing PDF", e)
            null
        }
    }
}
