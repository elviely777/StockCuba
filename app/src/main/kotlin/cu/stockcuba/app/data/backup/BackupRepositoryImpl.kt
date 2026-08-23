package cu.stockcuba.app.data.backup

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
// import com.jakewharton.processphoenix.ProcessPhoenix
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
            // Verify schema version on the source file
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.Failure(DomainError.DatabaseError(IOException("Failed to open input stream")))

            // Create a temporary file to verify schema
            val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.db")
            tempFile.parentFile?.mkdirs()
            
            try {
                copyStream(inputStream, FileOutputStream(tempFile))
            } finally {
                inputStream.close()
            }

            // Verify schema using SQLite
            val db = SQLiteDatabase.openDatabase(tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            try {
                val userVersion = db.version
                if (userVersion != 1) {
                    return@withContext Result.Failure(DomainError.InvalidOperation("Schema version mismatch: expected 1, got $userVersion"))
                }
            } finally {
                db.close()
            }

            // Delete current database and WAL/SHM files
            context.deleteDatabase(databaseName)
            val dbDir = context.getDatabasePath(databaseName).parentFile
            File(dbDir, "${databaseName}-wal").delete()
            File(dbDir, "${databaseName}-shm").delete()

            // Copy the verified file to the database location
            val targetFile = context.getDatabasePath(databaseName)
            targetFile.parentFile?.mkdirs()
            
            val targetStream = FileOutputStream(targetFile)
            try {
                copyStream(File(tempFile.absolutePath).inputStream(), targetStream)
            } finally {
                targetStream.close()
            }

            // Clean up temp file
            tempFile.delete()

            // Note: App restart needed for clean Room reinitialization
            // ProcessPhoenix.triggerRebirth(context) - manual restart required
            
            Result.Success(Unit)
        } catch (e: Exception) {
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
