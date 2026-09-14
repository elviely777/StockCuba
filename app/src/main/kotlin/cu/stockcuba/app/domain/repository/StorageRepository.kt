package cu.stockcuba.app.domain.repository

import java.io.File

import cu.stockcuba.app.domain.model.Result

interface StorageRepository {
    /**
     * Sube un archivo al almacenamiento y retorna la URL pública.
     * @param bytes Los bytes del archivo a subir.
     * @param bucket El nombre del bucket.
     * @param path El path dentro del bucket (incluyendo nombre de archivo).
     * @return Result con la URL pública o el error.
     */
    suspend fun uploadFile(bytes: ByteArray, bucket: String, path: String): Result<String>
}
