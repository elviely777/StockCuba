package cu.stockcuba.app.domain.feedback

import cu.stockcuba.app.domain.model.Result

/**
 * Repository interface for sending user feedback via email (T43).
 * Opens a mailto: intent with prefilled context including:
 * - App version
 * - Device model
 * - Android version
 * - Current theme
 * - Current currency
 * - PIN enabled status
 */
interface FeedbackRepository {

    /**
     * Sends feedback by opening an email client with prefilled context.
     * Returns Result.Success(Unit) if email intent was launched successfully.
     * Returns Result.Failure(DomainError.NotFound) if no email app is available.
     */
    suspend fun sendFeedback(): Result<Unit>
}