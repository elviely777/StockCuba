package cu.stockcuba.app.data.supabase

import cu.stockcuba.app.domain.model.DomainError
import cu.stockcuba.app.domain.model.Result
import cu.stockcuba.app.domain.repository.StorageRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.io.File
import javax.inject.Inject

class SupabaseStorageRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val config: SupabaseConfig
) : StorageRepository {

    override suspend fun uploadFile(bytes: ByteArray, bucket: String, path: String): Result<String> {
        return try {
            val url = "${SupabaseConstants.SUPABASE_URL}/storage/v1/object/$bucket/$path"
            
            val response: HttpResponse = httpClient.post(url) {
                // Sobrescribimos el Content-Type por defecto (JSON) para subir el archivo binario
                contentType(ContentType.Image.JPEG) 
                header("x-upsert", "true")
                setBody(bytes)
            }

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                // Construimos la URL pública (asumiendo que el bucket es público)
                val publicUrl = "${SupabaseConstants.SUPABASE_URL}/storage/v1/object/public/$bucket/$path"
                Result.Success(publicUrl)
            } else {
                Result.Failure(DomainError.Unknown("Error al subir archivo: ${response.status}", null))
            }
        } catch (e: Exception) {
            Result.Failure(DomainError.NetworkError(e))
        }
    }
}
