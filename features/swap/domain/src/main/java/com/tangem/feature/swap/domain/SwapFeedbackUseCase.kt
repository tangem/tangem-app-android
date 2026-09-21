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

    /** Emits null while the rating for [txExternalId] is not loaded, and when the local read fails */
    fun observeRating(txExternalId: String): Flow<SwapRating?> = repository.observeRating(txExternalId)

    /** Loads the rating for [txExternalId] once; later calls for the same transaction hit the local store */
    suspend fun ensureLoaded(txExternalId: String) = repository.fetchRatingIfNeeded(txExternalId)

    /** The rating becomes visible via [observeRating] only once the survey vendor has accepted it */
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