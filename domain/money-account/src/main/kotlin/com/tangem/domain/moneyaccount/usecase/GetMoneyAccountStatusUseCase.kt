package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import kotlinx.coroutines.flow.first

class GetMoneyAccountStatusUseCase(
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, MoneyAccountStatusValue> {
        return moneyAccountStatusFetcher(userWalletId).map {
            moneyAccountStatusSupplier(userWalletId).first().value
        }
    }
}