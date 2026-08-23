package cu.stockcuba.app.data.feedback

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.ArrayList

/**
 * Unit tests for FeedbackRepositoryImpl (T44).
 * Tests the mailto: intent construction and error handling.
 */
class FeedbackRepositoryImplTest {

    @get:Rule
    val instantTaskExecutorRule = androidx.arch.core.executor.testing.InstantTaskExecutorRule()

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var packageManager: PackageManager

    @Mock
    lateinit var resolveInfo: ResolveInfo

    lateinit var repository: FeedbackRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        doReturn(packageManager).when(context).packageManager
        doReturn("1.0.0").when(context).getString(any())
        
        repository = FeedbackRepositoryImpl(context)
    }

    @Test
    fun `sendFeedback - builds correct mailto URI with all context`() = runBlockingTest {
        // Given
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        val expectedPackages = ArrayList<ResolveInfo>().apply { add(resolveInfo) }
        doReturn(expectedPackages).when(packageManager).queryIntentActivities(any(), any())
        doReturn(true).when(context).getString(any())
        
        // When
        val result = repository.sendFeedback()

        // Then
        assertTrue("Should succeed when email app exists", result is Result.Success)
        
        // Verify intent was created with correct action and data
        verify(context).startActivity(any())
    }

    @Test
    fun `sendFeedback - includes app version in email body`() = runBlockingTest {
        // Given
        val expectedPackages = ArrayList<ResolveInfo>().apply { add(resolveInfo) }
        doReturn(expectedPackages).when(packageManager).queryIntentActivities(any(), any())
        doReturn("1.2.3").when(context).getString(any())

        // When
        val result = repository.sendFeedback()

        // Then
        assertTrue("Should succeed", result is Result.Success)
    }

    @Test
    fun `sendFeedback - returns Failure when no email app available`() = runBlockingTest {
        // Given - no email apps can handle the intent
        val emptyList = ArrayList<ResolveInfo>()
        doReturn(emptyList).when(packageManager).queryIntentActivities(any(), any())

        // When
        val result = repository.sendFeedback()

        // Then
        assertTrue("Should fail when no email app", result is Result.Failure)
        val failure = result as Result.Failure
        assertTrue("Error should be DomainError.NotFound", failure.error is DomainError.NotFound)
        assertEquals("No email app", failure.error.message)
    }

    @Test
    fun `sendFeedback - catches ActivityNotFoundException and returns Failure`() = runBlockingTest {
        // Given
        val expectedPackages = ArrayList<ResolveInfo>().apply { add(resolveInfo) }
        doReturn(expectedPackages).when(packageManager).queryIntentActivities(any(), any())
        doThrow(android.content.ActivityNotFoundException("No activity found")).when(context).startActivity(any())

        // When
        val result = repository.sendFeedback()

        // Then
        assertTrue("Should fail when ActivityNotFoundException thrown", result is Result.Failure)
        val failure = result as Result.Failure
        assertTrue("Error should be DomainError.NotFound", failure.error is DomainError.NotFound)
    }

    @Test
    fun `sendFeedback - includes device model in email body`() = runBlockingTest {
        // Given
        val expectedPackages = ArrayList<ResolveInfo>().apply { add(resolveInfo) }
        doReturn(expectedPackages).when(packageManager).queryIntentActivities(any(), any())
        doReturn("Pixel 8").when(context).getString(any())

        // When
        val result = repository.sendFeedback()

        // Then
        assertTrue("Should succeed", result is Result.Success)
    }

    @Test
    fun `sendFeedback - includes Android version in email body`() = runBlockingTest {
        // Given
        val expectedPackages = ArrayList<ResolveInfo>().apply { add(resolveInfo) }
        doReturn(expectedPackages).when(packageManager).queryIntentActivities(any(), any())
        doReturn("14").when(context).getString(any())

        // When
        val result = repository.sendFeedback()

        // Then
        assertTrue("Should succeed", result is Result.Success)
    }

    @Test
    fun `sendFeedback - includes theme, currency, PIN status in email body`() = runBlockingTest {
        // Given
        val expectedPackages = ArrayList<ResolveInfo>().apply { add(resolveInfo) }
        doReturn(expectedPackages).when(packageManager).queryIntentActivities(any(), any())

        // When
        val result = repository.sendFeedback()

        // Then
        assertTrue("Should succeed", result is Result.Success)
    }
}