package com.tangem.tap.domain.walletCurrencies

import com.tangem.common.CompletionResult
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.features.wallet.models.Currency

interface WalletCurrenciesManager {
    /**
     * Update [UserWallet] currencies with same blockchain as provided [Currency] blockchain
     * [UserWallet] currencies updates can be observed
     * with [com.tangem.tap.domain.walletStores.WalletStoresManager.getAll]
     * or [com.tangem.tap.domain.walletStores.WalletStoresManager.get]
     *
     * @param userWallet [UserWallet] which currencies will be updated
     * @param currency [Currency] to find other currencies to update
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun update(
        userWallet: UserWallet,
        currency: Currency,
    ): CompletionResult<Unit>

    /**
     * Add list of [Currency] to [UserWallet].
     * [UserWallet] currencies updates can be observed
     * with [com.tangem.tap.domain.walletStores.WalletStoresManager.getAll]
     * or [com.tangem.tap.domain.walletStores.WalletStoresManager.get]
     *
     * @param userWallet [UserWallet] to add currencies
     * @param currenciesToAdd list of [Currency] to add [UserWallet]
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun addCurrencies(
        userWallet: UserWallet,
        currenciesToAdd: List<Currency>,
    ): CompletionResult<Unit>

    /**
     * Remove [Currency] from [UserWallet]
     * [UserWallet] currencies updates can be observed
     * with [com.tangem.tap.domain.walletStores.WalletStoresManager.getAll]
     * or [com.tangem.tap.domain.walletStores.WalletStoresManager.get]
     *
     * @param userWallet [UserWallet] which currency will be removed
     * @param currencyToRemove [Currency] to remove from [UserWallet]
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun removeCurrency(
        userWallet: UserWallet,
        currencyToRemove: Currency,
    ): CompletionResult<Unit>

    /**
     * Remove list of [Currency] from [UserWallet]
     * [UserWallet] currencies updates can be observed
     * with [com.tangem.tap.domain.walletStores.WalletStoresManager.getAll]
     * or [com.tangem.tap.domain.walletStores.WalletStoresManager.get]
     *
     * @param userWallet [UserWallet] which currency will be removed
     * @param currenciesToRemove list of [Currency] to remove from [UserWallet]
     *
     * @return [CompletionResult] of operation
     * */
    suspend fun removeCurrencies(
        userWallet: UserWallet,
        currenciesToRemove: List<Currency>,
    ): CompletionResult<Unit>

    // For provider
    companion object
}
