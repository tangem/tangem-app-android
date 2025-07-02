package com.tangem.domain.swap

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapStatusModel
import com.tangem.domain.swap.models.SwapTransactionListModel
import com.tangem.domain.swap.models.SwapTransactionModel
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Swap repository for statuses
 */
interface SwapTransactionRepository {

    /**
     * Store new swap transaction
     *
     * @param userWalletId selected user wallet id
     * @param fromCryptoCurrency currency swap from
     * @param toCryptoCurrency currency swap to
     * @param transaction swap transaction
     */
    suspend fun storeTransaction(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        transaction: SwapTransactionModel,
    )

    /**
     * Get list of swap transactions
     *
     * @param userWallet selected user wallet
     * @param cryptoCurrencyId transactions for specific crypto currency
     */
    suspend fun getTransactions(
        userWallet: UserWallet,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): Flow<List<SwapTransactionListModel>?>

    /**
     * Remove stores swap transaction
     *
     * @param userWalletId selected user wallet id
     * @param fromCryptoCurrency currency swap from
     * @param toCryptoCurrency currency swap to
     * @param txId transaction id to remove
     */
    suspend fun removeTransaction(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        txId: String,
    )

    /**
     * Update swap transaction
     *
     * @param txId transaction id to update
     * @param status new transaction status
     * @param refundTokenCurrency refund token
     */
    suspend fun storeTransactionState(txId: String, status: SwapStatusModel, refundTokenCurrency: CryptoCurrency?)

    /**
     * Save last swapped crypto currency token
     *
     * @param userWalletId selected user wallet id
     * @param cryptoCurrencyId last swapped currency id
     */
    suspend fun storeLastSwappedCryptoCurrencyId(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID)

    /**
     * Get last swapped crypto currency token
     *
     * @param userWalletId selected user wallet id
     */
    suspend fun getLastSwappedCryptoCurrencyId(userWalletId: UserWalletId): String?
}