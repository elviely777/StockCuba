package cu.stockcuba.app.data.security

import android.util.Base64
import cu.stockcuba.app.presentation.ajustes.AjustesDataStore
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.security.SecurityRepository
import kotlinx.coroutines.flow.first
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import java.security.SecureRandom

/**
 * Implementation of SecurityRepository using AjustesDataStore (T35).
 * Uses PBKDF2WithHmacSHA256 with 100,000 iterations and 256-bit key.
 */
@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val ajustesDataStore: AjustesDataStore
) : SecurityRepository {

    companion object {
        private const val ITERATIONS = 100_000
        private const val KEY_LENGTH = 256
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val SALT_LENGTH = 32 // 256 bits
    }

    override suspend fun setPin(pin: String): Result<Unit> {
        return try {
            // Generate random salt
            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)
            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)

            // Hash PIN with PBKDF2
            val hash = hashPin(pin, salt)
            val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

            // Store hash and salt
            val hashResult = ajustesDataStore.guardarPinHash(hashBase64)
            if (hashResult.isFailure) return hashResult
            val saltResult = ajustesDataStore.guardarPinSalt(saltBase64)
            if (saltResult.isFailure) return saltResult

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun verifyPin(pin: String): Result<Boolean> {
        return try {
            val storedHashBase64 = ajustesDataStore.pinHash.first()
            val storedSaltBase64 = ajustesDataStore.pinSalt.first()

            if (storedHashBase64 == null || storedSaltBase64 == null) {
                return Result.Success(false)
            }

            val storedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP)
            val storedSalt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)

            // Hash the provided PIN with the stored salt
            val computedHash = hashPin(pin, storedSalt)

            // Constant-time comparison
            val matches = constantTimeEquals(storedHash, computedHash)

            Result.Success(matches)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    override suspend fun hasPin(): Result<Boolean> {
        return try {
            val hash = ajustesDataStore.pinHash.first()
            val salt = ajustesDataStore.pinSalt.first()
            Result.Success(hash != null && salt != null)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    /**
     * Hashes a PIN using PBKDF2WithHmacSHA256.
     */
    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    /**
     * Constant-time array comparison to prevent timing attacks.
     */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
