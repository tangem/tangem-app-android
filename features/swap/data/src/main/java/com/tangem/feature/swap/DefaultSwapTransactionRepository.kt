package com.tangem.feature.swap

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectList
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.converters.SavedSwapTransactionListConverter
import com.tangem.feature.swap.domain.SwapTransactionRepository
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DefaultSwapTransactionRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
    excludedBlockchains: ExcludedBlockchains,
) : SwapTransactionRepository {

    private val converter = SavedSwapTransactionListConverter(excludedBlockchains)
    private val userTokensResponseFactory = UserTokensResponseFactory()

    override suspend fun storeTransaction(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        transaction: SavedSwapTransactionModel,
    ) {
        transaction.status?.let {
            storeTransactionState(
                txId = transaction.txId,
                status = it,
                refundTokenCurrency = null,
            )
        }
        appPreferencesStore.editData { mutablePreferences ->
            val savedTransactions: List<SavedSwapTransactionListModelInner>? = mutablePreferences.getObjectList(
                key = PreferencesKeys.SWAP_TRANSACTIONS_KEY,
            )
            val tokenTransactions = savedTransactions
                ?.firstOrNull {
                    it.checkId(
                        checkUserWalletId = userWalletId,
                        fromCurrencyId = fromCryptoCurrency.id,
                        toCurrencyId = toCryptoCurrency.id,
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
                    fromCryptoCurrency = fromCryptoCurrency,
                    toCryptoCurrency = toCryptoCurrency,
                    transactions = tokenTransactions,
                ) ?: listOf(
                    converter.default(
                        userWalletId = userWalletId,
                        fromCryptoCurrency = fromCryptoCurrency,
                        toCryptoCurrency = toCryptoCurrency,
                        tokenTransactions = listOf(transaction),
                    ),
                ),
            )
        }
    }

    override suspend fun getTransactions(
        userWallet: UserWallet,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): Flow<List<SavedSwapTransactionListModel>?> {
        return withContext(dispatchers.io) {
            val txStatuses = appPreferencesStore.getObjectMapSync<ExchangeStatusModel>(
                key = PreferencesKeys.SWAP_TRANSACTIONS_STATUSES_KEY,
            )
            appPreferencesStore.getObjectList<SavedSwapTransactionListModelInner>(
                key = PreferencesKeys.SWAP_TRANSACTIONS_KEY,
            ).map { savedTransactions ->
                val currencyTxs = savedTransactions
                    ?.filter {
                        it.userWalletId == userWallet.walletId.stringValue &&
                            (
                                it.toCryptoCurrencyId == cryptoCurrencyId.value ||
                                    it.fromCryptoCurrencyId == cryptoCurrencyId.value
                                )
                    }

                currencyTxs?.mapNotNull {
                    converter.convertBack(
                        value = it,
                        scanResponse = userWallet.scanResponse,
                        txStatuses = txStatuses,
                    )
                }
            }.flowOn(dispatchers.io)
        }
    }

    override suspend fun removeTransaction(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        txId: String,
    ) {
        clearTransactionsStatuses(txId = txId)
        appPreferencesStore.editData { mutablePreferences ->
            val savedList: List<SavedSwapTransactionListModelInner>? = mutablePreferences.getObjectList(
                key = PreferencesKeys.SWAP_TRANSACTIONS_KEY,
            )
            val tokenTransactions = savedList
                ?.firstOrNull {
                    it.checkId(
                        checkUserWalletId = userWalletId,
                        fromCurrencyId = fromCryptoCurrency.id,
                        toCurrencyId = toCryptoCurrency.id,
                    )
                }
                ?.transactions
                ?.filterNot { it.txId == txId }

            val editedList =
                if (tokenTransactions.isNullOrEmpty()) {
                    savedList?.filterNot {
                        it.checkId(
                            checkUserWalletId = userWalletId,
                            fromCurrencyId = fromCryptoCurrency.id,
                            toCurrencyId = toCryptoCurrency.id,
                        )
                    }
                } else {
                    savedList.updateList(
                        userWalletId = userWalletId,
                        fromCryptoCurrency = fromCryptoCurrency,
                        toCryptoCurrency = toCryptoCurrency,
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

    override suspend fun storeTransactionState(
        txId: String,
        status: ExchangeStatusModel,
        refundTokenCurrency: CryptoCurrency?,
    ) {
        appPreferencesStore.editData { mutablePreferences ->
            val savedMap = mutablePreferences.getObjectMap<ExchangeStatusModel>(
                key = PreferencesKeys.SWAP_TRANSACTIONS_STATUSES_KEY,
            )

            val updatesMap = savedMap.toMutableMap()
            updatesMap[txId] = status.copy(
                refundTokensResponse = refundTokenCurrency?.let {
                    userTokensResponseFactory.createResponseToken(refundTokenCurrency)
                },
            )

            mutablePreferences.setObjectMap(
                key = PreferencesKeys.SWAP_TRANSACTIONS_STATUSES_KEY,
                value = updatesMap,
            )
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

    private fun SavedSwapTransactionListModelInner.checkId(
        checkUserWalletId: UserWalletId,
        fromCurrencyId: CryptoCurrency.ID,
        toCurrencyId: CryptoCurrency.ID,
    ): Boolean {
        return userWalletId == checkUserWalletId.stringValue &&
            toCryptoCurrencyId == toCurrencyId.value &&
            fromCryptoCurrencyId == fromCurrencyId.value
    }

    private fun List<SavedSwapTransactionListModelInner>.updateList(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        transactions: List<SavedSwapTransactionModel>,
    ): List<SavedSwapTransactionListModelInner> {
        return addOrReplace(
            item = converter.default(
                userWalletId = userWalletId,
                fromCryptoCurrency = fromCryptoCurrency,
                toCryptoCurrency = toCryptoCurrency,
                tokenTransactions = transactions,
            ),
            predicate = {
                it.checkId(
                    checkUserWalletId = userWalletId,
                    fromCurrencyId = fromCryptoCurrency.id,
                    toCurrencyId = toCryptoCurrency.id,
                )
            },
        )
    }

    private suspend fun clearTransactionsStatuses(txId: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val savedList = mutablePreferences.getObjectMap<ExchangeStatusModel>(
                key = PreferencesKeys.SWAP_TRANSACTIONS_STATUSES_KEY,
            )
            val editedList = savedList.filterNot { it.key == txId }

            if (editedList.isEmpty()) {
                mutablePreferences.remove(key = PreferencesKeys.SWAP_TRANSACTIONS_STATUSES_KEY)
            } else {
                mutablePreferences.setObjectMap(
                    key = PreferencesKeys.SWAP_TRANSACTIONS_STATUSES_KEY,
                    value = editedList,
                )
            }
        }
    }
}