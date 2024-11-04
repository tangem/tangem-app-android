package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class GetMinimumTransactionAmountSyncUseCase(
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<Throwable, BigDecimal?> = withContext(dispatchers.io) {
        either {
            val cryptoCurrency = cryptoCurrencyStatus.currency
            currencyChecksRepository.getMinimumSendAmount(userWalletId, cryptoCurrency.network)
        }
    }
}
