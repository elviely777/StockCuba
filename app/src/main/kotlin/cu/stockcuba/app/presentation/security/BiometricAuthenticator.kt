package cu.stockcuba.app.presentation.security

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper for BiometricPrompt with CryptoObject support (T37).
 * Provides a simple callback-based API for biometric authentication.
 */
@Singleton
class BiometricAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var biometricPrompt: BiometricPrompt? = null
    private var currentCallback: ((Result<Boolean>) -> Unit)? = null

    /**
     * Initializes the BiometricPrompt. Must be called from a FragmentActivity.
     */
    fun initialize(activity: FragmentActivity) {
        val executor = ContextCompat.getMainExecutor(activity)
        biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                currentCallback?.invoke(Result.Failure(DomainError.Unknown(errString.toString(), null)))
                currentCallback = null
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                currentCallback?.invoke(Result.Success(true))
                currentCallback = null
            }

            override fun onAuthenticationFailed() {
                currentCallback?.invoke(Result.Success(false))
                currentCallback = null
            }
        })
    }


    /**
     * Starts biometric authentication.
     * @param callback Called with Result.Success(true) on success, Result.Success(false) on failure, Result.Failure on error
     * @param cryptoObject Optional CryptoObject for cryptographic operations
     */
    fun authenticate(
        callback: (Result<Boolean>) -> Unit,
        cryptoObject: BiometricPrompt.CryptoObject? = null
    ) {
        currentCallback = callback
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación biométrica")
            .setSubtitle("Usa tu huella o rostro para acceder")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        if (cryptoObject != null) {
            biometricPrompt?.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt?.authenticate(promptInfo)
        }
    }

    /**
     * Cancels any ongoing biometric authentication.
     */
    fun cancel() {
        biometricPrompt?.cancelAuthentication()
        currentCallback = null
    }

    /**
     * Checks if biometric hardware is available and enrolled.
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        val result = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }
}
