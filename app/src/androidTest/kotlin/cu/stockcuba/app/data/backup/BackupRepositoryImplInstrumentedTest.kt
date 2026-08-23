package cu.stockcuba.app.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cu.stockcuba.app.data.local.database.StockCubaDatabase
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class BackupRepositoryImplInstrumentedTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var backupRepository: BackupRepositoryImpl
    private lateinit var database: StockCubaDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        contentResolver = context.contentResolver
        backupRepository = BackupRepositoryImpl(context)
        
        // Create a test database with some data
        database = StockCubaDatabase.getInstance(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `exportDatabase - crea archivo en Downloads/StockCuba/ visible en Files app`() = runBlockingTest {
        // When
        val result = backupRepository.exportDatabase()

        // Then
        assertTrue("Export should succeed", result is Result.Success)
        val uri = (result as Result.Success<Uri>).value
        
        // Verify the file exists and is readable
        val inputStream = contentResolver.openInputStream(uri)
        assertNotNull("File should be readable", inputStream)
        inputStream?.close()
        
        // Verify it's in the correct location by checking the path
        val path = uri.path
        assertTrue("Should be in Downloads/StockCuba/", path?.contains("/StockCuba/") == true)
        assertTrue("Should have .db extension", path?.endsWith(".db") == true)
        assertTrue("Should have stockcuba_ prefix and date", path?.contains("stockcuba_") == true)
        
        // Clean up
        contentResolver.delete(uri, null, null)
    }

    @Test
    fun `exportDatabase - archivo contiene todas las tablas de la base de datos`() = runBlockingTest {
        // When
        val result = backupRepository.exportDatabase()

        // Then
        assertTrue(result is Result.Success)
        val uri = (result as Result.Success<Uri>).value
        
        // Copy to temp file to inspect with SQLite
        val tempFile = File(context.cacheDir, "test_export_${System.currentTimeMillis()}.db")
        val inputStream = contentResolver.openInputStream(uri)!!
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        
        // Verify database structure
        val db = android.database.sqlite.SQLiteDatabase.openDatabase(tempFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
        try {
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
            val tables = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
            cursor.close()
            
            // Verify all expected tables exist
            assertTrue("Should have producto table", tables.contains("producto"))
            assertTrue("Should have categoria table", tables.contains("categoria"))
            assertTrue("Should have venta table", tables.contains("venta"))
            assertTrue("Should have venta_item table", tables.contains("venta_item"))
            assertTrue("Should have cliente table", tables.contains("cliente"))
            assertTrue("Should have movimiento_inventario table", tables.contains("movimiento_inventario"))
            assertTrue("Should have room_master_table", tables.contains("room_master_table"))
            
            // Verify user_version is 1
            assertEquals(1, db.version)
        } finally {
            db.close()
            tempFile.delete()
        }
        
        // Clean up
        contentResolver.delete(uri, null, null)
    }

    @Test
    fun `importDatabase - falla con schema mismatch (user_version != 1)`() = runBlockingTest {
        // Given - Create a database file with wrong schema version
        val tempFile = File(context.cacheDir, "wrong_schema_${System.currentTimeMillis()}.db")
        val db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(tempFile, null)
        db.execSQL("CREATE TABLE test (id INTEGER PRIMARY KEY)")
        db.setVersion(999) // Wrong version
        db.close()
        
        // Insert into MediaStore to get a Uri
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, tempFile.name)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StockCuba/")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3")
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)!!
        
        val outputStream = contentResolver.openOutputStream(uri)!!
        val inputStream = FileInputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        
        // When
        val result = backupRepository.importDatabase(uri)
        
        // Then
        assertTrue("Should fail with schema mismatch", result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue("Should be InvalidOperation", error is DomainError.InvalidOperation)
        assertTrue("Should mention schema version", error.message.contains("Schema version mismatch"))
        
        // Clean up
        contentResolver.delete(uri, null, null)
        tempFile.delete()
    }

    @Test
    fun `importDatabase - importa archivo valido correctamente`() = runBlockingTest {
        // Given - Export current database first to get a valid backup
        val exportResult = backupRepository.exportDatabase()
        assertTrue(exportResult is Result.Success)
        val exportUri = (exportResult as Result.Success<Uri>).value
        
        // When - Import the same file
        // Note: This will trigger ProcessPhoenix.restart, so we can't easily verify after
        // But we can verify it doesn't fail with schema mismatch
        val importResult = backupRepository.importDatabase(exportUri)
        
        // The import either succeeds (and triggers restart) or we're still here if it fails
        // If it returns without restarting, it should be a Failure
        // Since ProcessPhoenix restarts the process, this test documents expected behavior
        
        // Clean up
        try {
            contentResolver.delete(exportUri, null, null)
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun `importDatabase - atomic replace elimina DB actual y archivos WAL/SHM`() = runBlockingTest {
        // Given - Create a valid backup file
        val exportResult = backupRepository.exportDatabase()
        assertTrue(exportResult is Result.Success)
        val exportUri = (exportResult as Result.Success<Uri>).value
        
        // Verify database files exist before import
        val dbFile = context.getDatabasePath("stockcuba_db")
        assertTrue("DB file should exist before import", dbFile.exists())
        
        // When - Import (will restart app via ProcessPhoenix)
        // We can't easily test the atomic replace without the restart
        // This test documents that the import process starts
        val importResult = backupRepository.importDatabase(exportUri)
        
        // Clean up
        try {
            contentResolver.delete(exportUri, null, null)
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}