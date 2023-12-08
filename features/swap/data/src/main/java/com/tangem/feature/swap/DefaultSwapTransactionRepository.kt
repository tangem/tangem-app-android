package com.tangem.feature.swap

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectList
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.SwapTransactionRepository
import com.tangem.feature.swap.domain.models.domain.SavedLastSwappedCryptoCurrency
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionListModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultSwapTransactionRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : SwapTransactionRepository {

    override suspend fun storeTransaction(
        userWalletId: UserWalletId,
        fromCryptoCurrencyId: CryptoCurrency.ID,
        toCryptoCurrencyId: CryptoCurrency.ID,
        transaction: SavedSwapTransactionModel,
    ) {
        appPreferencesStore.editData { mutablePreferences ->
            val savedTransactions: List<SavedSwapTransactionListModel>? = mutablePreferences.getObjectList(
                key = PreferencesKeys.SWAP_TRANSACTIONS_KEY,
            )

            val tokenTransactions = savedTransactions
                ?.firstOrNull {
                    it.checkId(
                        checkUserWalletId = userWalletId,
                        fromCurrencyId = fromCryptoCurrencyId,
                        toCurrencyId = toCryptoCurrencyId,
                    )
                }
                ?.transactions
                ?.addOrReplace(
                    item = transaction,
                    predicate = { it.txId == transaction.txId },
                ) ?: listOf(transaction)

            mutablePreferences.setObject(
                key = PreferencesKeys.SWAP_TRANSACTIONS_KEY,
                value = savedTransactions?.updateList(
                    userWalletId = userWalletId,
                    fromCryptoCurrencyId = fromCryptoCurrencyId,
                    toCryptoCurrencyId = toCryptoCurrencyId,
                    transactions = tokenTransactions,
                ) ?: listOf(
                    SavedSwapTransactionListModel(
                        userWalletId = userWalletId.stringValue,
                        fromCryptoCurrencyId = fromCryptoCurrencyId.value,
                        toCryptoCurrencyId = toCryptoCurrencyId.value,
                        transactions = tokenTransactions,
                    ),
                ),
            )
        }
    }

    override fun getTransactions(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): Flow<List<SavedSwapTransactionListModel>?> {
        return appPreferencesStore.getObjectList<SavedSwapTransactionListModel>(
            key = PreferencesKeys.SWAP_TRANSACTIONS_KEY,
        ).map { savedTransactions ->
            savedTransactions
                ?.filter {
                    it.userWalletId == userWalletId.stringValue &&
                        (
                            it.toCryptoCurrencyId == cryptoCurrencyId.value ||
                                it.fromCryptoCurrencyId == cryptoCurrencyId.value
                            )
                }
        }
    }

    override suspend fun removeTransaction(
        userWalletId: UserWalletId,
        fromCryptoCurrencyId: CryptoCurrency.ID,
        toCryptoCurrencyId: CryptoCurrency.ID,
        txId: String,
    ) {
        appPreferencesStore.editData { mutablePreferences ->
            val savedList: List<SavedSwapTransactionListModel>? = mutablePreferences.getObjectList(
                key = PreferencesKeys.SWAP_TRANSACTIONS_KEY,
            )
            val tokenTransactions = savedList
                ?.first {
                    it.checkId(
                        checkUserWalletId = userWalletId,
                        fromCurrencyId = fromCryptoCurrencyId,
                        toCurrencyId = toCryptoCurrencyId,
                    )
                }
                ?.transactions
                ?.filterNot { it.txId == txId }

            val editedList =
                if (tokenTransactions.isNullOrEmpty()) {
                    savedList?.filterNot {
                        it.checkId(
                            checkUserWalletId = userWalletId,
                            fromCurrencyId = fromCryptoCurrencyId,
                            toCurrencyId = toCryptoCurrencyId,
                        )
                    }
                } else {
                    savedList.updateList(
                        userWalletId = userWalletId,
                        fromCryptoCurrencyId = fromCryptoCurrencyId,
                        toCryptoCurrencyId = toCryptoCurrencyId,
                        transactions = tokenTransactions,
                    )
                }

            if (editedList.isNullOrEmpty()) {
                mutablePreferences.remove(key = PreferencesKeys.SWAP_TRANSACTIONS_KEY)
            } else {
                mutablePreferences.setObject(
                    key = PreferencesKeys.SWAP_TRANSACTIONS_KEY,
                    value = editedList,
                )
            }
        }
    }

    override suspend fun getLastSwappedCryptoCurrencyId(userWalletId: UserWalletId): String? {
        val lastSwappedCurrencies = appPreferencesStore.getObjectListSync<SavedLastSwappedCryptoCurrency>(
            key = PreferencesKeys.LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY,
        )

        return lastSwappedCurrencies.find { userWalletId.stringValue == it.userWalletId }?.cryptoCurrencyId
    }

    override suspend fun storeLastSwappedCryptoCurrencyId(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
    ) {
        appPreferencesStore.editData { mutablePreferences ->
            val lastSwappedCryptoCurrencies: List<SavedLastSwappedCryptoCurrency>? = mutablePreferences.getObjectList(
                key = PreferencesKeys.LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY,
            )

            val newList = if (lastSwappedCryptoCurrencies != null) {
                lastSwappedCryptoCurrencies.filter {
                    it.userWalletId != userWalletId.stringValue
                } + SavedLastSwappedCryptoCurrency(userWalletId.stringValue, cryptoCurrencyId.value)
            } else {
                listOf(SavedLastSwappedCryptoCurrency(userWalletId.stringValue, cryptoCurrencyId.value))
            }

            mutablePreferences.setObjectList(
                key = PreferencesKeys.LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY,
                value = newList,
            )
        }
    }

    private fun SavedSwapTransactionListModel.checkId(
        checkUserWalletId: UserWalletId,
        fromCurrencyId: CryptoCurrency.ID,
        toCurrencyId: CryptoCurrency.ID,
    ): Boolean {
        return userWalletId == checkUserWalletId.stringValue &&
            toCryptoCurrencyId == toCurrencyId.value &&
            fromCryptoCurrencyId == fromCurrencyId.value
    }

    private fun List<SavedSwapTransactionListModel>.updateList(
        userWalletId: UserWalletId,
        fromCryptoCurrencyId: CryptoCurrency.ID,
        toCryptoCurrencyId: CryptoCurrency.ID,
        transactions: List<SavedSwapTransactionModel>,
    ): List<SavedSwapTransactionListModel> {
        return addOrReplace(
            item = SavedSwapTransactionListModel(
                userWalletId = userWalletId.stringValue,
                fromCryptoCurrencyId = fromCryptoCurrencyId.value,
                toCryptoCurrencyId = toCryptoCurrencyId.value,
                transactions = transactions,
            ),
            predicate = {
                it.checkId(
                    checkUserWalletId = userWalletId,
                    fromCurrencyId = fromCryptoCurrencyId,
                    toCurrencyId = toCryptoCurrencyId,
                )
            },
        )
    }
}
