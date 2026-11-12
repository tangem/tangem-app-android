package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.account.CardDisplayName
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.repository.TangemPayCardDetailsRepository

class UpdateTangemPayCardNameUseCase(
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(
        cardId: String,
        userWalletId: UserWalletId,
        displayName: CardDisplayName,
    ): Either<UniversalError, Unit> {
        return cardDetailsRepository.updateCardDisplayName(cardId, userWalletId, displayName)
            .onRight { paymentAccountStatusFetcher(userWalletId) }
    }
}