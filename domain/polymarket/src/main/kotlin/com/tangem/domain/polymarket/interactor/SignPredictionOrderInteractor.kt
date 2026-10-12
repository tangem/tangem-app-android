package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import com.tangem.domain.polymarket.signing.PredictionOrderPayload
import com.tangem.domain.polymarket.usecase.BuildPredictionOrderDraftUseCase
import com.tangem.domain.polymarket.usecase.ValidatePredictionOrderUseCase
import java.security.SecureRandom

class SignPredictionOrderInteractor(
    private val buildDraft: BuildPredictionOrderDraftUseCase,
    private val validateOrder: ValidatePredictionOrderUseCase,
    private val signer: PolymarketTypedDataSigner,
) {

    suspend operator fun invoke(
        request: PredictionSignRequest,
    ): Either<PredictionOrderFailure, PredictionSignedOrder> = either {
        val addresses = request.addresses

        val draft = buildDraft(
            quote = request.quote,
            assetId = request.assetId,
            isNegRisk = request.isNegRisk,
            side = request.side,
        )
            .mapLeft(PredictionOrderFailure::Draft)
            .bind()

        val timestampMillis = System.currentTimeMillis()
        val signedOrder = signer.signOrder(
            userWalletId = addresses.userWalletId,
            payload = PredictionOrderPayload(
                draft = draft,
                depositWalletAddress = addresses.depositWalletAddress,
                salt = newSalt(),
                timestampMillis = timestampMillis,
            ),
        )
            .mapLeft(PredictionOrderFailure::Signing)
            .bind()

        validateOrder(
            signedOrder = signedOrder,
            draft = draft,
            depositWalletAddress = addresses.depositWalletAddress,
            marketTokenId = request.assetId,
            isNegRisk = request.isNegRisk,
            quoteReceivedAtMillis = request.quoteReceivedAtMillis,
            nowMillis = System.currentTimeMillis(),
        )
            .mapLeft(PredictionOrderFailure::Invalid)
            .bind()

        signedOrder
    }

    private fun newSalt(): Long = random.nextLong() and SALT_MASK

    private companion object {
        const val SALT_BITS = 52
        const val SALT_MASK = (1L shl SALT_BITS) - 1

        val random = SecureRandom()
    }
}

data class PredictionSignRequest(
    val addresses: PolymarketAddresses,
    val quote: PredictionOrderQuote,
    val assetId: String,
    val side: PredictionOrderSide,
    val isNegRisk: Boolean,
    val quoteReceivedAtMillis: Long,
)