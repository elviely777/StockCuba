package cu.stockcuba.app.domain.security

import cu.stockcuba.app.domain.model.Result

/**
 * Repository interface for security operations (T34).
 * Handles PIN management with PBKDF2 hashing and biometric authentication.
 */
interface SecurityRepository {

    /**
     * Sets a new PIN using PBKDF2WithHmacSHA256 with 100,000 iterations and 256-bit key.
     * Generates a random salt and stores both hash and salt.
     */
    suspend fun setPin(pin: String): Result<Unit>

    /**
     * Verifies a PIN against the stored hash using constant-time comparison.
     * Returns Result.Success(true) if PIN matches, Result.Success(false) otherwise.
     */
    suspend fun verifyPin(pin: String): Result<Boolean>

    /**
     * Checks if a PIN has been set.
     */
    suspend fun hasPin(): Result<Boolean>

    /**
     * Gets whether biometric authentication is enabled.
     */
    suspend fun getBiometricEnabled(): Result<Boolean>

    /**
     * Enables or disables biometric authentication.
     */
    suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit>
}