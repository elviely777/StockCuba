package cu.stockcuba.app.data.backup

import android.net.Uri
import cu.stockcuba.app.domain.model.Result

interface BackupRepository {
    suspend fun exportDatabase(): Result<Uri>
    suspend fun importDatabase(uri: Uri): Result<Unit>
}