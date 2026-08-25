package cu.stockcuba.app.data.backup

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.jakewharton.processphoenix.ProcessPhoenix
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BackupRepository {

    private val contentResolver: ContentResolver = context.contentResolver
    private val databaseName = "stockcuba_db"
    private val databaseDir = "databases"

    override suspend fun exportDatabase(): Result<Uri> = withContext(Dispatchers.IO) {
        return@withContext try {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val baseFileName = "stockcuba_${LocalDate.now().format(dateFormatter)}"
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/StockCuba/"

            // Get the database directory and force WAL checkpoint so all data is in main .db
            val dbFile = context.getDatabasePath(databaseName)
            val dbDir = dbFile.parentFile
            
            // Force WAL checkpoint to flush all transactions to main .db file
            try {
                val checkpointDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                )
                checkpointDb.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                checkpointDb.close()
            } catch (e: Exception) {
                android.util.Log.w("BackupRepo", "WAL checkpoint failed, continuing anyway", e)
            }

            // Export ONLY the main .db file (now contains all data after checkpoint)
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "${baseFileName}.db")
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1)
                put(MediaStore.Downloads.MIME_TYPE, "application/x-sqlite3")
            }

            val pendingUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.Failure(DomainError.DatabaseError(IOException("Failed to create MediaStore entry")))

            val outputStream = contentResolver.openOutputStream(pendingUri)
                ?: return@withContext Result.Failure(DomainError.DatabaseError(IOException("Failed to open output stream")))

            try {
                copyFile(dbFile, outputStream)
            } catch (e: Exception) {
                contentResolver.delete(pendingUri, null, null)
                return@withContext Result.Failure(DomainError.DatabaseError(e))
            } finally {
                outputStream.close()
            }

            // Publish the file
            val publishValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            val updated = contentResolver.update(pendingUri, publishValues, null, null)
            if (updated <= 0) {
                contentResolver.delete(pendingUri, null, null)
                return@withContext Result.Failure(DomainError.DatabaseError(IOException("Failed to publish MediaStore entry")))
            }

            Result.Success(pendingUri)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun importDatabase(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Copy the file to a temporary location to work with it
            val tempFile = File(context.cacheDir, "import_check.db")
            tempFile.delete()
            
            val initialInputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.Failure(DomainError.DatabaseError(IOException("No se pudo leer el archivo")))

            FileOutputStream(tempFile).use { output ->
                copyStream(initialInputStream, output)
            }
            initialInputStream.close()

            // Verify if it's a valid SQLite database by trying to open it (T60)
            try {
                val checkDb = android.database.sqlite.SQLiteDatabase.openDatabase(
                    tempFile.absolutePath, 
                    null, 
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                checkDb.close()
            } catch (e: Exception) {
                tempFile.delete()
                return@withContext Result.Failure(DomainError.InvalidOperation("El archivo seleccionado no es una base de datos válida o está protegido"))
            }

            // Close active database connections before replacing files
            try {
                cu.stockcuba.app.data.local.database.StockCubaDatabase.getInstance(context).close()
            } catch (e: Exception) {
                // Ignore
            }

            // Delete current database and WAL/SHM files
            context.deleteDatabase(databaseName)
            val dbFile = context.getDatabasePath(databaseName)
            val dbDir = dbFile.parentFile
            if (dbDir != null) {
                File(dbDir, "${databaseName}-wal").delete()
                File(dbDir, "${databaseName}-shm").delete()
            }

            // Copy the verified file from temp to final location
            dbFile.parentFile?.mkdirs()
            tempFile.inputStream().use { input ->
                dbFile.outputStream().use { output ->
                    copyStream(input, output)
                }
            }
            tempFile.delete()

            // auto-restart app
            ProcessPhoenix.triggerRebirth(context)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BackupRepo", "Error fatal en restauración", e)
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    private fun copyFile(source: File, outputStream: OutputStream) {
        val inputStream = source.inputStream()
        try {
            copyStream(inputStream, outputStream)
        } finally {
            inputStream.close()
        }
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(8192)
        var len: Int
        while (input.read(buffer).also { len = it } != -1) {
            output.write(buffer, 0, len)
        }
        output.flush()
    }
}
