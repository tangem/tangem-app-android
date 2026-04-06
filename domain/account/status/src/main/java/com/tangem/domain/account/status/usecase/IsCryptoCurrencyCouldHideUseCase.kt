package com.tangem.domain.account.status.usecase

import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getTokens
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case to determine if a cryptocurrency can be hidden from the portfolio.
 *
 * Rules:
 * - [CryptoCurrency.Token] can always be hidden (returns `true`)
 * - [CryptoCurrency.Coin] can be hidden only if there are no tokens in the same network (returns `true` if no tokens)
 *
 * @property singleAccountListSupplier supplier to get the account list with all cryptocurrencies
 */
class IsCryptoCurrencyCouldHideUseCase(
    private val singleAccountListSupplier: SingleAccountListSupplier,
) {

    /**
     * Checks if the given cryptocurrency can be hidden.
     *
     * @param userWalletId the user wallet identifier
     * @param cryptoCurrency the cryptocurrency to check
     * @return `true` if the cryptocurrency can be hidden, `false` otherwise
     */
    suspend operator fun invoke(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        return when (cryptoCurrency) {
            is CryptoCurrency.Token -> true
            is CryptoCurrency.Coin -> {
                val accountList = singleAccountListSupplier.getSyncOrNull(userWalletId = userWalletId)
                    ?: return true

                accountList.getTokens(cryptoCurrency).isEmpty()
            }
        }
    }
}