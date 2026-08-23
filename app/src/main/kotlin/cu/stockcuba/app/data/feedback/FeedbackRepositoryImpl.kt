package cu.stockcuba.app.data.feedback

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import cu.stockcuba.app.domain.feedback.FeedbackRepository
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FeedbackRepository that opens a mailto: intent with prefilled context (T44).
 * Includes:
 * - Subject: "StockCuba Feedback"
 * - Body: version, device model, Android version, tema, moneda, PIN enabled (yes/no)
 * Uses Intent.ACTION_SENDTO with Uri.parse("mailto:") and Intent.FLAG_ACTIVITY_NEW_TASK
 * Catches ActivityNotFoundException → returns Result.Failure(DomainError.NotFound("No email app"))
 */
@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ajustesDataStore: AjustesDataStore
) : FeedbackRepository {

    override suspend fun sendFeedback(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Build mailto URI with subject and body
            val mailtoUri = buildMailtoUri()
            
            // Create intent
            val intent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Verify there's an app that can handle the intent
            val packageManager = context.packageManager
            val activities: List<ResolveInfo> = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            
            if (activities.isEmpty()) {
                return@withContext Result.Failure(DomainError.NotFound("email app", "No email app"))
            }

            // Launch the intent
            context.startActivity(intent)
            
            Result.Success(Unit)
        } catch (e: ActivityNotFoundException) {
            Result.Failure(DomainError.NotFound("email app", "No email app"))
        } catch (e: Exception) {
            Result.Failure(DomainError.Unknown("Failed to send feedback: ${e.message}", e))
        }
    }

    /**
     * Builds the mailto: URI with subject and body containing app context.
     */
    private suspend fun buildMailtoUri(): Uri {
        val subject = "StockCuba Feedback"
        
        val body = buildString {
            appendLine("--- Feedback Context ---")
            appendLine("App Version: ${getAppVersion()}")
            appendLine("Device Model: ${Build.MODEL}")
            appendLine("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Tema: ${getTheme()}")
            appendLine("Moneda: ${getCurrency()}")
            appendLine("PIN habilitado: ${getPinEnabled()}")
            appendLine("")
            appendLine("--- Tu feedback ---")
        }

        val encodedSubject = Uri.encode(subject)
        val encodedBody = Uri.encode(body)
        
        return Uri.parse("mailto:?subject=$encodedSubject&body=$encodedBody")
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private suspend fun getTheme(): String {
        // Read theme from DataStore synchronously for the email body
        return try {
            ajustesDataStore.tema.first()
        } catch (e: Exception) {
            "SYSTEM"
        }
    }

    private suspend fun getCurrency(): String {
        return try {
            ajustesDataStore.moneda.first().name
        } catch (e: Exception) {
            "CUP"
        }
    }

    private suspend fun getPinEnabled(): String {
        return try {
            val hasPin = ajustesDataStore.pinHash.first() != null && ajustesDataStore.pinSalt.first() != null
            if (hasPin) "Sí" else "No"
        } catch (e: Exception) {
            "No"
        }
    }
}
