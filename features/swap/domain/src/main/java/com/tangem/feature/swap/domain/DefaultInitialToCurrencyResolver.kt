package com.tangem.feature.swap.domain

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.swap.domain.models.ui.TokensDataStateExpress
import java.math.BigDecimal

internal class DefaultInitialToCurrencyResolver(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val swapTransactionRepository: SwapTransactionRepository,
) : InitialToCurrencyResolver {

    override suspend fun tryGetFromCache(
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
    ): CryptoCurrencyStatus? {
        val selectedId = getSelectedWalletSyncUseCase().getOrNull() ?: return null
        val id = swapTransactionRepository.getLastSwappedCryptoCurrencyId(selectedId.walletId) ?: return null

        return if (id != initialCryptoCurrency.id.value) {
            state.toGroup.available.find { it.currencyStatus.currency.id.value == id }?.currencyStatus
        } else {
            null
        }
    }

    override fun tryGetWithMaxAmount(state: TokensDataStateExpress): CryptoCurrencyStatus? {
        return state.toGroup.available.maxByOrNull {
            it.currencyStatus.value.fiatAmount ?: BigDecimal.ZERO
        }?.currencyStatus
    }
}