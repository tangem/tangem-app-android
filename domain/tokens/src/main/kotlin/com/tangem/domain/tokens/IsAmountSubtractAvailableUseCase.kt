package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository

/**
 * Use case for checking if currency amount can be subtracted.
 * Amount can be subtracted if only it is paying fee
 */
class IsAmountSubtractAvailableUseCase(
    private val currenciesRepository: CurrenciesRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        maybeGaslessFee: Pair<CryptoCurrency.ID, Fee>? = null,
    ): Either<Throwable, Boolean> = Either.catch {
        val maybeTokenCurrency = currency as? CryptoCurrency.Token
        if (maybeGaslessFee != null &&
            maybeGaslessFee.second is Fee.Ethereum.TokenCurrency &&
            maybeGaslessFee.first.contractAddress.equals(maybeTokenCurrency?.contractAddress, true)
        ) {
            return@catch true
        }
        when (val feeCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, currency.network)) {
            is FeePaidCurrency.Coin -> currency is CryptoCurrency.Coin
            is FeePaidCurrency.SameCurrency -> true
            is FeePaidCurrency.Token -> currency.id == feeCurrency.tokenId
            is FeePaidCurrency.FeeResource -> false
        }
    }
}