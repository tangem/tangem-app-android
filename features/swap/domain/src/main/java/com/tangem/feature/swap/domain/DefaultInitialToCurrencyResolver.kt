package com.tangem.feature.swap.domain

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.domain.models.ui.TokensDataStateExpress
import com.tangem.feature.swap.domain.models.ui.getGroupWithReverse
import java.math.BigDecimal

internal class DefaultInitialToCurrencyResolver(
    private val swapTransactionRepository: SwapTransactionRepository,
) : InitialToCurrencyResolver {

    override suspend fun tryGetFromCache(
        userWallet: UserWallet,
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
        isReverseFromTo: Boolean,
    ): CryptoCurrencyStatus? {
        val id = swapTransactionRepository.getLastSwappedCryptoCurrencyId(userWallet.walletId) ?: return null

        return if (id != initialCryptoCurrency.id.value) {
            val group = state.getGroupWithReverse(isReverseFromTo)
            group.available.find { it.currencyStatus.currency.id.value == id }?.currencyStatus
        } else {
            null
        }
    }

    override fun tryGetWithMaxAmount(state: TokensDataStateExpress, isReverseFromTo: Boolean): CryptoCurrencyStatus? {
        val group = state.getGroupWithReverse(isReverseFromTo)
        return group.available.maxByOrNull {
            it.currencyStatus.value.fiatAmount ?: BigDecimal.ZERO
        }?.currencyStatus
    }
}