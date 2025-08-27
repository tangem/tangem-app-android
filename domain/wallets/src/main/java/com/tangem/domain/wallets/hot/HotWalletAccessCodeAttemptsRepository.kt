package com.tangem.domain.wallets.hot

import com.tangem.hot.sdk.model.HotWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing access code attempts for hot wallets.
 * It tracks the number of attempts made to access a hot wallet and applies cooldowns or deletion
 * based on the number of attempts.
 */
interface HotWalletAccessCodeAttemptsRepository {

    /**
     * Increments the number of attempts for the given [AttemptId].
     * If the number of attempts exceeds [MAX_FAST_FORWARD_ATTEMPTS], a cooldown period is initiated.
     */
    suspend fun incrementAttempts(id: AttemptId)

    /**
     * Resets the attempts for the given [HotWalletId].
     * This is typically called when the user successfully authenticates or when the wallet is deleted.
     */
    suspend fun resetAttempts(hotWalletId: HotWalletId)

    /**
     * Retrieves the current attempts for the given [AttemptId].
     * The result is a flow that emits the current state of attempts.
     */
    fun getAttempts(id: AttemptId): Flow<Attempts>

    /**
     * Synchronously retrieves the current attempts for the given [AttemptId].
     * This is useful when you need to get the attempts without using a flow.
     */
    suspend fun getAttemptsSync(id: AttemptId): Attempts

    data class AttemptId(
        val hotWalletId: HotWalletId,
        val auth: Boolean,
    )

    sealed interface Attempts {
        val count: Int

        data class FastForward(
            override val count: Int,
        ) : Attempts

        data class WithDelay(
            override val count: Int,
            val remainingSeconds: Int,
        ) : Attempts

        data class BeforeDeletion(
            override val count: Int,
            val remainingSeconds: Int,
            val remainingAttemptsCountBeforeDeletion: Int,
        ) : Attempts

        data object Deletion : Attempts {
            override val count: Int = MAX_ATTEMPTS_BEFORE_DELETION
        }
    }

    companion object {
        const val COOLDOWN_SECONDS = 60
        const val MAX_FAST_FORWARD_ATTEMPTS = 5
        const val ATTEMPTS_BEFORE_DELETION = 20
        const val MAX_ATTEMPTS_BEFORE_DELETION = 30
    }
}