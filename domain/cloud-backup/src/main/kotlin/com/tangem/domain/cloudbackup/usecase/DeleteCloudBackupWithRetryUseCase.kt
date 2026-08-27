package com.tangem.domain.cloudbackup.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Deletes a cloud backup file, retrying transient failures a bounded number of times.
 *
 * Deletion happens best-effort during wallet teardown flows (forget / upgrade), so it must be
 * resilient to a temporarily unreachable cloud storage. Up to [maxAttempts] attempts are made with
 * [retryDelay] between them (defaults give a ~1 minute window: 3 attempts, 2 gaps of 30s).
 * Deleting an already-absent file is a success (see [CloudBackupRepository.deleteBackup]).
 */
class DeleteCloudBackupWithRetryUseCase @Inject constructor(
    private val cloudBackupRepository: CloudBackupRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        fileId: String,
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        retryDelay: Duration = DEFAULT_RETRY_DELAY,
    ): Either<CloudBackupError, Unit> {
        require(maxAttempts > 0) { "maxAttempts must be > 0" }
        return withContext(dispatchers.io) {
            var lastError: CloudBackupError? = null
            repeat(maxAttempts) { attempt ->
                cloudBackupRepository.deleteBackup(fileId).fold(
                    ifLeft = { error ->
                        lastError = error
                        // retrying a non-transient error (auth / cloud unavailable) only wastes the delay window
                        if (!error.isTransient() || attempt == maxAttempts - 1) return@withContext error.left()
                        delay(retryDelay)
                    },
                    ifRight = { return@withContext Unit.right() },
                )
            }
            lastError?.left() ?: Unit.right()
        }
    }

    private fun CloudBackupError.isTransient(): Boolean =
        this is CloudBackupError.NetworkError || this is CloudBackupError.Unknown

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
        val DEFAULT_RETRY_DELAY = 30.seconds
    }
}