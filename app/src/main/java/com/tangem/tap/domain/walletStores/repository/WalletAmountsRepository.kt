package com.tangem.tap.domain.walletStores.repository

import com.tangem.common.CompletionResult
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel

interface WalletAmountsRepository {

    /**
     * Fetch wallet amounts and fiat rates then update [com.tangem.tap.domain.walletStores.storage.WalletStoresStorage]
     * and [com.tangem.tap.domain.walletStores.storage.WalletManagerStorage] with new data
     * @param userWallets list of [UserWallet] which will be used to get the list of associated [WalletStoreModel]
     * @param fiatCurrency current app [FiatCurrency]
     * */
    suspend fun updateAmountsForUserWallets(
        userWallets: List<UserWallet>,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit>

    /**
     * Fetch wallet amounts and fiat rates then update [com.tangem.tap.domain.walletStores.storage.WalletStoresStorage]
     * and [com.tangem.tap.domain.walletStores.storage.WalletManagerStorage] with new data
     * @param userWallet [UserWallet] which will be used to get the list of associated [WalletStoreModel]
     * @param fiatCurrency current app [FiatCurrency]
     * */
    suspend fun updateAmountsForUserWallet(
        userWallet: UserWallet,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit>

    suspend fun updateAmountsForWalletStores(
        walletStores: List<WalletStoreModel>,
        userWallet: UserWallet,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit>

    /**
     * Fetch wallet amounts and fiat rates then update [com.tangem.tap.domain.walletStores.storage.WalletStoresStorage]
     * and [com.tangem.tap.domain.walletStores.storage.WalletManagerStorage] with new data
     * @param walletStore [WalletStoreModel] to update
     * @param userWallet [UserWallet] associated with provided [walletStore]
     * @param fiatCurrency current app [FiatCurrency]
     * */
    suspend fun updateAmountsForWalletStore(
        walletStore: WalletStoreModel,
        userWallet: UserWallet,
        fiatCurrency: FiatCurrency,
    ): CompletionResult<Unit>

    companion object
}
