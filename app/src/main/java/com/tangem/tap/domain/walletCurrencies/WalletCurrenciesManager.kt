package com.tangem.tap.domain.walletCurrencies

import com.tangem.common.CompletionResult
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.models.Currency

interface WalletCurrenciesManager {
    suspend fun update(
        userWallet: UserWallet,
        blockchainNetwork: BlockchainNetwork,
    ): CompletionResult<Unit>

    suspend fun addCurrencies(
        userWallet: UserWallet,
        currenciesToAdd: List<Currency>,
    ): CompletionResult<Unit>

    suspend fun removeCurrency(
        userWallet: UserWallet,
        currencyToRemove: Currency,
    ): CompletionResult<Unit>

    suspend fun removeCurrencies(
        userWallet: UserWallet,
        currenciesToRemove: List<Currency>,
    ): CompletionResult<Unit>

    companion object
}
