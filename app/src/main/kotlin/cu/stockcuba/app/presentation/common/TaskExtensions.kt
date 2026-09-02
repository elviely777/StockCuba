package cu.stockcuba.app.presentation.common

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result as T)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Unknown Task Error"))
        }
    }
}
