package com.tangem.tap.domain.walletStores.repository

import com.tangem.common.CompletionResult
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel

interface WalletAmountsRepository {
    suspend fun update(
        userWallets: List<UserWallet>,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit>

    suspend fun update(
        userWallet: UserWallet,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit>

    suspend fun update(
        userWallet: UserWallet,
        walletStore: WalletStoreModel,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit>

    companion object
}
