package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal

class GetMinimumTransactionAmountSyncUseCase(
    private val currencyChecksRepository: CurrencyChecksRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<Throwable, BigDecimal?> = either {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        currencyChecksRepository.getMinimumSendAmount(userWalletId, cryptoCurrency.network)
    }
}