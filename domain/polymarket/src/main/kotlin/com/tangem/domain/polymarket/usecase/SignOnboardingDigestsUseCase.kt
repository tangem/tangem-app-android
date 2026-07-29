package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.approval.PolymarketApprovalCalls
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketSignedOnboarding
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketClobAuthData
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import java.math.BigInteger

/**
 * Signs both onboarding payloads in a single signing session and returns everything the following steps
 * must reuse unchanged: the signatures, the timestamp that was signed, and the approvals payload it covers.
 */
class SignOnboardingDigestsUseCase(
    private val signer: PolymarketTypedDataSigner,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
        relayerNonce: BigInteger,
    ): Either<PolymarketOnboardingError, PolymarketSignedOnboarding> {
        val timestamp = System.currentTimeMillis() / MILLIS_IN_SECOND
        val approvals = PolymarketApprovalsPayload(
            depositWalletAddress = addresses.depositWalletAddress,
            nonce = relayerNonce.toString(),
            deadline = (timestamp + DEADLINE_OFFSET_SECONDS).toString(),
            calls = PolymarketApprovalCalls.build(),
        )

        return signer
            .signOnboarding(
                userWalletId = addresses.userWalletId,
                clobAuth = PolymarketClobAuthData(timestamp = timestamp.toString()),
                approvals = approvals,
            )
            .mapLeft { it.toOnboardingError() }
            .map { signatures ->
                PolymarketSignedOnboarding(
                    l1Signature = signatures.l1Signature,
                    clobAuthTimestamp = timestamp.toString(),
                    batchSignature = signatures.batchSignature,
                    approvals = approvals,
                )
            }
    }

    private companion object {
        const val MILLIS_IN_SECOND = 1000L
        const val DEADLINE_OFFSET_SECONDS = 600L
    }
}