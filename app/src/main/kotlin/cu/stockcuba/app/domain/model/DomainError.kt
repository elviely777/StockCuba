package cu.stockcuba.app.domain.model

sealed interface DomainError {
    data class NotFound(val entity: String, val id: String) : DomainError
    data class AlreadyExists(val entity: String, val id: String) : DomainError
    data class ValidationError(val field: String, val message: String) : DomainError
    data class InsufficientStock(val productId: String, val requested: Int, val available: Int) : DomainError
    data class InvalidOperation(val reason: String) : DomainError
    data class NetworkError(val cause: Throwable?) : DomainError
    data class DatabaseError(val cause: Throwable?) : DomainError
    data class Unknown(val message: String, val cause: Throwable?) : DomainError

    object InvalidQuantity : DomainError
    object ProductNotActive : DomainError
    object CategoryHasProducts : DomainError
}

sealed interface Result<out T> {
    data class Success<out T>(val value: T) : Result<T>
    data class Failure(val error: DomainError) : Result<Nothing>

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    fun onSuccess(action: (T) -> Unit): Result<T> = when (this) {
        is Success -> { action(value); this }
        is Failure -> this
    }

    fun onFailure(action: (DomainError) -> Unit): Result<T> = when (this) {
        is Success -> this
        is Failure -> { action(error); this }
    }

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    val valueOrNull: T?
        get() = when (this) {
            is Success -> value
            is Failure -> null
        }

    val errorOrNull: DomainError?
        get() = when (this) {
            is Success -> null
            is Failure -> error
        }

    /**
     * Folds the Result into a single value by applying either the success or failure handler.
     * This is the primary way to consume a Result when you need to handle both cases.
     */
    fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (DomainError) -> R
    ): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(error)
    }
}