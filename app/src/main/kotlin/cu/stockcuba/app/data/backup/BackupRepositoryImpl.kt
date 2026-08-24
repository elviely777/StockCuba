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

            // Get the database directory and all 3 files
            val dbFile = context.getDatabasePath(databaseName)
            val dbDir = dbFile.parentFile
            
            val filesToExport = mutableListOf<File>()
            filesToExport.add(dbFile) // main .db file
            
            if (dbDir != null) {
                val walFile = File(dbDir, "${databaseName}-wal")
                val shmFile = File(dbDir, "${databaseName}-shm")
                if (walFile.exists()) filesToExport.add(walFile)
                if (shmFile.exists()) filesToExport.add(shmFile)
            }

            var mainDbUri: Uri? = null
            var success = true
            var lastException: Exception? = null

            // Export each file as separate MediaStore entry
            for (file in filesToExport) {
                val fileName = when {
                    file.name.endsWith("-wal") -> "${baseFileName}-wal"
                    file.name.endsWith("-shm") -> "${baseFileName}-shm"
                    else -> "${baseFileName}.db"
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                    put(MediaStore.Downloads.MIME_TYPE, "application/x-sqlite3")
                }

                val pendingUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.Failure(DomainError.DatabaseError(IOException("Failed to create MediaStore entry for $fileName")))

                // Save the main .db URI to return
                if (fileName.endsWith(".db")) {
                    mainDbUri = pendingUri
                }

                // Open output stream for this file
                val outputStream = contentResolver.openOutputStream(pendingUri)
                    ?: return@withContext Result.Failure(DomainError.DatabaseError(IOException("Failed to open output stream for $fileName")))

                try {
                    copyFile(file, outputStream)
                } catch (e: Exception) {
                    success = false
                    lastException = e
                } finally {
                    outputStream.close()
                }

                if (!success) {
                    // Clean up pending entry on failure
                    contentResolver.delete(pendingUri, null, null)
                    return@withContext Result.Failure(DomainError.DatabaseError(lastException ?: IOException("Unknown copy error for $fileName")))
                }

                // Publish the file by setting IS_PENDING=0
                val publishValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                val updated = contentResolver.update(pendingUri, publishValues, null, null)
                if (updated <= 0) {
                    return@withContext Result.Failure(DomainError.DatabaseError(IOException("Failed to publish MediaStore entry for $fileName")))
                }
            }

            Result.Success(mainDbUri ?: throw IOException("Main DB URI not created"))
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
