package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.usecase.ProduceTangemPayInitialDataUseCase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class RenewTangemPaySessionUseCase(
    private val produceTangemPayInitialDataUseCase: ProduceTangemPayInitialDataUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Unit> {
        return produceTangemPayInitialDataUseCase(userWalletId).onRight {
            coroutineScope {
                launch { paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(userWalletId)) }
                launch { moneyAccountStatusFetcher.invoke(userWalletId) }
            }
        }
    }
}