package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.repository.TangemPayCardDetailsRepository
import java.math.BigDecimal

class SetTangemPayCardLimitUseCase(
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {
    suspend operator fun invoke(
        cardId: String,
        userWalletId: UserWalletId,
        amount: BigDecimal,
    ): Either<UniversalError, Unit> {
        return cardDetailsRepository.updateCardLimit(cardId, userWalletId, amount.toPlainString())
            .onRight { paymentAccountStatusFetcher.invoke(userWalletId) }
    }
}