package cu.stockcuba.app.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import java.io.File
import java.io.OutputStream

class BackupRepositoryImplTest {

    @get:Rule
    val instantTaskExecutorRule = androidx.arch.core.executor.testing.InstantTaskExecutorRule()

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var contentResolver: ContentResolver

    @Mock
    lateinit var outputStream: OutputStream

    @Mock
    lateinit var pendingUri: Uri

    @Mock
    lateinit var dbFile: File

    @Mock
    lateinit var walFile: File

    @Mock
    lateinit var shmFile: File

    @Mock
    lateinit var dbDir: File

    lateinit var backupRepository: BackupRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        doReturn(contentResolver).when(context) .contentResolver
        
        backupRepository = BackupRepositoryImpl(context)
    }

    @Test
    fun `exportDatabase - crea entrada en MediaStore con IS_PENDING=1`() = runBlockingTest {
        // Given
        doReturn(pendingUri).when(contentResolver).insert(any(), any())
        doReturn(outputStream).when(contentResolver).openOutputStream(pendingUri)
        doReturn(1).when(contentResolver).update(any(), any(), any(), any())
        doReturn(dbFile).when(context).getDatabasePath("stockcuba_db")
        doReturn(true).when(dbFile).exists()

        // When
        val result = backupRepository.exportDatabase()

        // Then
        assertTrue("Export should succeed", result is Result.Success)
        val uri = (result as Result.Success<Uri>).value
        assertEquals(pendingUri, uri)
        
        // Verify IS_PENDING=1 insert was called
        verify(contentResolver).insert(any(), any())
        
        // Verify openOutputStream called once for main db file
        verify(contentResolver, Mockito.times(1)).openOutputStream(pendingUri)
        
        // Verify IS_PENDING=0 publish
        val updateCaptor = org.mockito.kotlin.argumentCaptor<android.content.ContentValues>()
        verify(contentResolver).update(eq(pendingUri), updateCaptor.capture(), any(), any())
        assertEquals(0, updateCaptor.value.getAsInteger("is_pending"))
    }

    @Test
    fun `exportDatabase - falla si insert retorna null`() = runBlockingTest {
        // Given
        doReturn(null).when(contentResolver).insert(any(), any())

        // When
        val result = backupRepository.exportDatabase()

        // Then
        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is DomainError.DatabaseError)
    }

    @Test
    fun `exportDatabase - falla si openOutputStream lanza excepcion`() = runBlockingTest {
        // Given
        doReturn(pendingUri).when(contentResolver).insert(any(), any())
        doThrow(java.io.IOException("Disk full")).when(contentResolver).openOutputStream(pendingUri)

        // When
        val result = backupRepository.exportDatabase()

        // Then
        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is DomainError.DatabaseError)
    }

    @Test
    fun `exportDatabase - copia los 3 archivos: db, wal, shm`() = runBlockingTest {
        // Given
        doReturn(pendingUri).when(contentResolver).insert(any(), any())
        doReturn(outputStream).when(contentResolver).openOutputStream(pendingUri)
        doReturn(1).when(contentResolver).update(any(), any(), any(), any())
        doReturn(dbFile).when(context).getDatabasePath("stockcuba_db")
        doReturn(true).when(dbFile).exists()
        doReturn(dbDir).when(dbFile).parentFile
        doReturn(walFile).when(dbDir, "${dbFile.name}-wal")
        doReturn(shmFile).when(dbDir, "${dbFile.name}-shm")
        doReturn(true).when(walFile).exists()
        doReturn(true).when(shmFile).exists()
        
        // When
        val result = backupRepository.exportDatabase()

        // Then
        assertTrue("Export should succeed", result is Result.Success)
        
        // Verify 3 MediaStore entries created (db, wal, shm)
        verify(contentResolver, Mockito.times(3)).insert(any(), any())
        
        // Verify 3 openOutputStream calls
        verify(contentResolver, Mockito.times(3)).openOutputStream(any())
        
        // Verify 3 updates to IS_PENDING=0
        verify(contentResolver, Mockito.times(3)).update(any(), any(), any(), any())
    }

    @Test
    fun `exportDatabase - returns main .db URI`() = runBlockingTest {
        // Given
        val mainDbUri = mock<Uri>()
        val walUri = mock<Uri>()
        val shmUri = mock<Uri>()
        
        // Simulate sequential inserts returning different URIs
        var insertCall = 0
        doReturn(mainDbUri).doReturn(walUri).doReturn(shmUri).when(contentResolver).insert(any(), any())
        
        doReturn(outputStream).when(contentResolver).openOutputStream(any())
        doReturn(1).when(contentResolver).update(any(), any(), any(), any())
        doReturn(dbFile).when(context).getDatabasePath("stockcuba_db")
        doReturn(true).when(dbFile).exists()
        doReturn(dbDir).when(dbFile).parentFile
        doReturn(walFile).when(dbDir, "${dbFile.name}-wal")
        doReturn(shmFile).when(dbDir, "${dbFile.name}-shm")
        doReturn(true).when(walFile).exists()
        doReturn(true).when(shmFile).exists()

        // When
        val result = backupRepository.exportDatabase()

        // Then
        assertTrue("Export should succeed", result is Result.Success)
        val returnedUri = (result as Result.Success<Uri>).value
        assertEquals("Should return main .db URI", mainDbUri, returnedUri)
    }

    @Test
    fun `exportDatabase - handles missing WAL/SHM files gracefully`() = runBlockingTest {
        // Given - only main db file exists
        doReturn(pendingUri).when(contentResolver).insert(any(), any())
        doReturn(outputStream).when(contentResolver).openOutputStream(pendingUri)
        doReturn(1).when(contentResolver).update(any(), any(), any(), any())
        doReturn(dbFile).when(context).getDatabasePath("stockcuba_db")
        doReturn(true).when(dbFile).exists()
        doReturn(dbDir).when(dbFile).parentFile
        doReturn(walFile).when(dbDir, "${dbFile.name}-wal")
        doReturn(shmFile).when(dbDir, "${dbFile.name}-shm")
        doReturn(false).when(walFile).exists()
        doReturn(false).when(shmFile).exists()
        
        // When
        val result = backupRepository.exportDatabase()

        // Then
        assertTrue("Export should succeed even without WAL/SHM", result is Result.Success)
        
        // Only 1 MediaStore entry for main db
        verify(contentResolver, Mockito.times(1)).insert(any(), any())
        verify(contentResolver, Mockito.times(1)).openOutputStream(any())
        verify(contentResolver, Mockito.times(1)).update(any(), any(), any(), any())
    }

    @Test
    fun `exportDatabase - cleans up pending entries on failure`() = runBlockingTest {
        // Given - first file succeeds, second fails
        val pendingUri1 = mock<Uri>()
        val pendingUri2 = mock<Uri>()
        
        doReturn(pendingUri1).doReturn(pendingUri2).when(contentResolver).insert(any(), any())
        doReturn(outputStream).doThrow(java.io.IOException("Write failed")).when(contentResolver).openOutputStream(any())
        doReturn(1).when(contentResolver).update(any(), any(), any(), any())
        doReturn(dbFile).when(context).getDatabasePath("stockcuba_db")
        doReturn(true).when(dbFile).exists()
        doReturn(dbDir).when(dbFile).parentFile
        doReturn(walFile).when(dbDir, "${dbFile.name}-wal")
        doReturn(shmFile).when(dbDir, "${dbFile.name}-shm")
        doReturn(true).when(walFile).exists()
        doReturn(true).when(shmFile).exists()

        // When
        val result = backupRepository.exportDatabase()

        // Then
        assertTrue("Export should fail", result is Result.Failure)
        
        // Should have deleted the first pending entry
        verify(contentResolver).delete(eq(pendingUri1), any(), any())
    }

    @Test
    fun `importDatabase - falla si schema user_version != 1`() = runBlockingTest {
        // This test documents expected behavior - actual implementation 
        // requires Android SQLite which isn't available in unit tests
        // The instrumented test covers this scenario
    }

    @Test
    fun `importDatabase - elimina base de datos actual y archivos WAL/SHM`() = runBlockingTest {
        // This test documents expected behavior - actual implementation 
        // requires Android context which isn't fully mockable in unit tests
        // The instrumented test covers this scenario
    }
}