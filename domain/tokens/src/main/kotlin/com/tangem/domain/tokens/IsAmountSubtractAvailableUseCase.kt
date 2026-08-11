package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.blockchain.common.AmountType
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
        if (maybeGaslessFee != null && isGaslessFeePaidBySentToken(currency, maybeGaslessFee)) return@catch true

        when (val feeCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, currency.network)) {
            is FeePaidCurrency.Coin -> currency is CryptoCurrency.Coin
            is FeePaidCurrency.SameCurrency -> true
            is FeePaidCurrency.Token -> currency.id == feeCurrency.tokenId
            is FeePaidCurrency.FeeResource -> false
        }
    }

    /**
     * Gasless fees are token-denominated: EVM carries [Fee.Ethereum.TokenCurrency], Tron carries a
     * [Fee.Common] whose amount is a token — both surface as [AmountType.Token], so the amount type is
     * what identifies them, not the [Fee] subtype.
     *
     * The amount may be reduced only when that token is the one being sent, i.e. the fee is charged to
     * the very balance being spent. A cross-token fee comes out of a separate balance and leaves the
     * sent amount untouched.
     */
    private fun isGaslessFeePaidBySentToken(
        currency: CryptoCurrency,
        gaslessFee: Pair<CryptoCurrency.ID, Fee>,
    ): Boolean {
        val (feeCurrencyId, fee) = gaslessFee
        if (fee.amount.type !is AmountType.Token) return false
        val sentToken = currency as? CryptoCurrency.Token ?: return false
        return feeCurrencyId.contractAddress.equals(sentToken.contractAddress, ignoreCase = true)
    }
}