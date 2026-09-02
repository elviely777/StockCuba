package cu.stockcuba.app.presentation.ajustes

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AjustesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        val NOMBRE_NEGOCIO_KEY = stringPreferencesKey("nombre_negocio")
        val DIRECCION_KEY = stringPreferencesKey("direccion")
        val TELEFONO_KEY = stringPreferencesKey("telefono")
        val MONEDA_KEY = stringPreferencesKey("moneda")
        val IMPUESTO_KEY = doublePreferencesKey("impuesto")
        val TEMA_KEY = stringPreferencesKey("tema")
        
        // PIN security keys (T33)
        val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        val PIN_SALT_KEY = stringPreferencesKey("pin_salt")

        // Trial keys
        val INSTALL_DATE_KEY = longPreferencesKey("install_date")

        // Firebase / Multinegocio keys (Facturación)
        val BUSINESS_ID_KEY = stringPreferencesKey("business_id")
        val POS_ID_KEY = stringPreferencesKey("pos_id")
        val POS_NOMBRE_KEY = stringPreferencesKey("pos_nombre")
        val ESTADO_NEGOCIO_KEY = stringPreferencesKey("estado_negocio")
    }

    val nombreNegocio: Flow<String> = dataStore.data
        .map { it[NOMBRE_NEGOCIO_KEY] ?: "Mi Negocio" }
        .distinctUntilChanged()

    val direccion: Flow<String> = dataStore.data
        .map { it[DIRECCION_KEY] ?: "" }
        .distinctUntilChanged()

    val telefono: Flow<String> = dataStore.data
        .map { it[TELEFONO_KEY] ?: "" }
        .distinctUntilChanged()

    val moneda: Flow<Moneda> = dataStore.data
        .map { it[MONEDA_KEY]?.let { Moneda.valueOf(it) } ?: Moneda.CUP }
        .distinctUntilChanged()

    val impuesto: Flow<Double> = dataStore.data
        .map { it[IMPUESTO_KEY] ?: 0.0 }
        .distinctUntilChanged()

    val tema: Flow<String> = dataStore.data
        .map { it[TEMA_KEY] ?: "SYSTEM" }
        .distinctUntilChanged()

    // PIN flows (T33)
    val pinHash: Flow<String?> = dataStore.data
        .map { it[PIN_HASH_KEY] }
        .distinctUntilChanged()

    val pinSalt: Flow<String?> = dataStore.data
        .map { it[PIN_SALT_KEY] }
        .distinctUntilChanged()

    val fechaInstalacion: Flow<Long?> = dataStore.data
        .map { it[INSTALL_DATE_KEY] }
        .distinctUntilChanged()

    val businessId: Flow<String?> = dataStore.data
        .map { it[BUSINESS_ID_KEY] }
        .distinctUntilChanged()

    val posId: Flow<String?> = dataStore.data
        .map { it[POS_ID_KEY] }
        .distinctUntilChanged()

    val posNombre: Flow<String?> = dataStore.data
        .map { it[POS_NOMBRE_KEY] }
        .distinctUntilChanged()

    val estadoNegocio: Flow<String> = dataStore.data
        .map { it[ESTADO_NEGOCIO_KEY] ?: "ACTIVO" }
        .distinctUntilChanged()

    val isVinculado: Flow<Boolean> = dataStore.data
        .map { it[BUSINESS_ID_KEY] != null && it[POS_ID_KEY] != null }
        .distinctUntilChanged()

    suspend fun guardarNombreNegocio(nombre: String): Result<Unit> = guardarDato(NOMBRE_NEGOCIO_KEY, nombre)
    suspend fun guardarDireccion(direccion: String): Result<Unit> = guardarDato(DIRECCION_KEY, direccion)
    suspend fun guardarTelefono(telefono: String): Result<Unit> = guardarDato(TELEFONO_KEY, telefono)
    suspend fun guardarMoneda(moneda: Moneda): Result<Unit> = guardarDato(MONEDA_KEY, moneda.name)
    suspend fun guardarImpuesto(impuesto: Double): Result<Unit> = guardarDato(IMPUESTO_KEY, impuesto)
    suspend fun guardarTema(tema: String): Result<Unit> = guardarDato(TEMA_KEY, tema)

    suspend fun guardarEstadoNegocio(estado: String): Result<Unit> = guardarDato(ESTADO_NEGOCIO_KEY, estado)

    // PIN setters (T33)
    suspend fun guardarPinHash(hash: String): Result<Unit> = guardarDato(PIN_HASH_KEY, hash)
    suspend fun guardarPinSalt(salt: String): Result<Unit> = guardarDato(PIN_SALT_KEY, salt)

    suspend fun guardarFechaInstalacion(fecha: Long): Result<Unit> = guardarDato(INSTALL_DATE_KEY, fecha)

    suspend fun guardarVinculacion(businessId: String, posId: String, posNombre: String): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[BUSINESS_ID_KEY] = businessId
                preferences[POS_ID_KEY] = posId
                preferences[POS_NOMBRE_KEY] = posNombre
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    suspend fun desvincular(): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences.remove(BUSINESS_ID_KEY)
                preferences.remove(POS_ID_KEY)
                preferences.remove(POS_NOMBRE_KEY)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    suspend fun eliminarPin(): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences.remove(PIN_HASH_KEY)
                preferences.remove(PIN_SALT_KEY)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    private suspend fun <T> guardarDato(key: Preferences.Key<T>, value: T): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[key] = value
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }

    /**
     * Clears all DataStore keys except the provided preserved keys.
     * Used for reset functionality (T26).
     * Preserves: TEMA_KEY, PIN_HASH_KEY, PIN_SALT_KEY by default.
     */
    suspend fun clearAll(exceptKeys: Set<Preferences.Key<*>> = setOf(
        TEMA_KEY, PIN_HASH_KEY, PIN_SALT_KEY
    )): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                // Properly access all keys in MutablePreferences (T59)
                val allKeys = preferences.asMap().keys.toList()
                for (key in allKeys) {
                    if (key !in exceptKeys) {
                        preferences.remove(key)
                    }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(DomainError.DatabaseError(e))
        }
    }
}
