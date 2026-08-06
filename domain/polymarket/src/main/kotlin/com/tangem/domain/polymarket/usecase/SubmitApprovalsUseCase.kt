package com.tangem.domain.polymarket.usecase

import arrow.core.Either
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketSignedOnboarding
import com.tangem.domain.polymarket.model.PolymarketWalletStatus

/**
 * Relays the signed approvals batch. Every signed field is copied from [PolymarketSignedOnboarding] as-is:
 * the signature covers exactly these values, so recomputing any of them makes the relayer reject the batch.
 */
class SubmitApprovalsUseCase(
    private val polymarketRepository: PolymarketRepository,
) {

    suspend operator fun invoke(
        addresses: PolymarketAddresses,
        signed: PolymarketSignedOnboarding,
    ): Either<PolymarketOnboardingError, PolymarketWalletStatus> = polymarketRepository
        .submitApprovals(
            batch = PolymarketApprovalsBatch(
                ownerAddress = addresses.ownerAddress,
                depositWalletAddress = signed.approvals.depositWalletAddress,
                nonce = signed.approvals.nonce,
                deadline = signed.approvals.deadline,
                calls = signed.approvals.calls,
                signature = signed.batchSignature,
            ),
        )
        .mapLeft { it.toOnboardingError() }
}