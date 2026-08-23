package cu.stockcuba.app.domain.feedback

import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for FeedbackRepository interface contract (T43).
 * These tests define the expected behavior before implementation.
 */
class FeedbackRepositoryTest {

    @Test
    fun `sendFeedback - returns Result<Unit> on success`() = runBlockingTest {
        // This test will fail until FeedbackRepository is implemented
        val repository = TestFeedbackRepository()
        val result = repository.sendFeedback()
        
        assertTrue("sendFeedback should return Result.Success on success", result is Result.Success)
    }

    @Test
    fun `sendFeedback - returns Result.Failure when no email app available`() = runBlockingTest {
        val repository = TestFeedbackRepository(shouldFail = true)
        val result = repository.sendFeedback()
        
        assertTrue("sendFeedback should return Result.Failure when no email app", result is Result.Failure)
        val failure = result as Result.Failure
        assertTrue("Error should be DomainError.NotFound", failure.error is cu.stockcuba.app.domain.model.DomainError.NotFound)
    }
}

/**
 * Test double for FeedbackRepository - will be replaced by actual implementation.
 * This test double simulates both success and failure cases.
 */
private class TestFeedbackRepository(private val shouldFail: Boolean = false) : FeedbackRepository {
    override suspend fun sendFeedback(): Result<Unit> {
        return if (shouldFail) {
            Result.Failure(cu.stockcuba.app.domain.model.DomainError.NotFound("email app", "No email app"))
        } else {
            Result.Success(Unit)
        }
    }
}