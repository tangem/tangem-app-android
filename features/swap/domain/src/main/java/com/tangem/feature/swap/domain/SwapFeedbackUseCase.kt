package com.tangem.feature.swap.domain

import arrow.core.Either
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.swap.domain.api.SwapFeedbackRepository
import com.tangem.feature.swap.domain.models.domain.SwapFeedbackParams
import com.tangem.feature.swap.domain.models.domain.SwapRating
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SwapFeedbackUseCase @Inject constructor(
    private val repository: SwapFeedbackRepository,
) {

    /** Emits null while the rating for [txExternalId] is not loaded */
    fun observeRating(txExternalId: String): Flow<SwapRating?> = repository.observeRating(txExternalId)

    /** Loads the rating for [txExternalId] on a cache miss; a failed load is retried on the next call */
    suspend fun ensureLoaded(txExternalId: String) = repository.fetchRatingIfNeeded(txExternalId)

    /** Optimistic submit: the rating is visible via [observeRating] immediately and rolled back on error */
    suspend fun submit(params: SubmitParams): Either<Throwable, Unit> {
        return repository.submitFeedback(
            SwapFeedbackParams(
                userWalletIdHash = params.userWalletId.value.calculateSha256().toHexString(),
                providerName = params.providerName,
                txUrl = params.txExternalUrl,
                txExternalId = params.txExternalId,
                rating = params.rating,
                feedback = params.feedback,
            ),
        )
    }

    data class SubmitParams(
        val txExternalId: String,
        val providerName: String,
        val txExternalUrl: String,
        val userWalletId: UserWalletId,
        val rating: Int,
        val feedback: String,
    )
}