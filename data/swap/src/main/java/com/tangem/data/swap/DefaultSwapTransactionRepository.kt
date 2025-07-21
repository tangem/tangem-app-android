package com.tangem.data.swap

import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.data.swap.converter.transaction.SavedSwapStatusConverter
import com.tangem.data.swap.converter.transaction.SavedSwapTransactionConverter
import com.tangem.data.swap.converter.transaction.SavedSwapTransactionListConverter
import com.tangem.data.swap.models.LastSwappedCryptoCurrencyDTO
import com.tangem.data.swap.models.SwapStatusDTO
import com.tangem.data.swap.models.SwapTransactionDTO
import com.tangem.data.swap.models.SwapTransactionListDTO
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectList
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.SwapTransactionRepository
import com.tangem.domain.swap.models.SwapStatusModel
import com.tangem.domain.swap.models.SwapTransactionListModel
import com.tangem.domain.swap.models.SwapTransactionModel
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DefaultSwapTransactionRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : SwapTransactionRepository {

    private val listConverter by lazy(LazyThreadSafetyMode.NONE) {
        SavedSwapTransactionListConverter(responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory)
    }
    private val converter by lazy(LazyThreadSafetyMode.NONE) {
        SavedSwapTransactionConverter(responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory)
    }
    private val savedStatusConverter by lazy(LazyThreadSafetyMode.NONE) {
        SavedSwapStatusConverter()
    }
    private val userTokensResponseFactory = UserTokensResponseFactory()

    override suspend fun storeTransaction(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        transaction: SwapTransactionModel,
    ) {
        transaction.status?.let {
            storeTransactionState(
                txId = transaction.txId,
                status = it,
                refundTokenCurrency = null,
            )
        }
        appPreferencesStore.editData { mutablePreferences ->
            val savedTransactions: List<SwapTransactionListDTO>? = mutablePreferences.getObjectList(
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
                    item = converter.convert(transaction),
                    predicate = { it.txId == transaction.txId },
                ) ?: listOf(converter.convert(transaction))

            mutablePreferences.setObject(
                key = PreferencesKeys.SWAP_TRANSACTIONS_KEY,
                value = savedTransactions?.updateList(
                    userWalletId = userWalletId,
                    fromCryptoCurrency = fromCryptoCurrency,
                    toCryptoCurrency = toCryptoCurrency,
                    transactions = tokenTransactions,
                ) ?: listOf(
                    listConverter.default(
                        userWalletId = userWalletId,
                        fromCryptoCurrency = fromCryptoCurrency,
                        toCryptoCurrency = toCryptoCurrency,
                        tokenTransactions = listOf(converter.convert(transaction)),
                    ),
                ),
            )
        }
    }

    override suspend fun getTransactions(
        userWallet: UserWallet,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): Flow<List<SwapTransactionListModel>?> {
        return withContext(dispatchers.io) {
            val txStatuses = appPreferencesStore.getObjectMapSync<SwapStatusDTO>(
                key = PreferencesKeys.SWAP_TRANSACTIONS_STATUSES_KEY,
            )
            appPreferencesStore.getObjectList<SwapTransactionListDTO>(
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
                    listConverter.convertBack(
                        value = it,
                        userWallet = userWallet,
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
            val savedList: List<SwapTransactionListDTO>? = mutablePreferences.getObjectList(
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
        status: SwapStatusModel,
        refundTokenCurrency: CryptoCurrency?,
    ) {
        appPreferencesStore.editData { mutablePreferences ->
            val savedMap = mutablePreferences.getObjectMap<SwapStatusDTO>(
                key = PreferencesKeys.SWAP_TRANSACTIONS_STATUSES_KEY,
            )

            val updatesMap = savedMap.toMutableMap()
            updatesMap[txId] = savedStatusConverter.convertBack(
                status.copy(
                    refundTokensResponse = refundTokenCurrency?.let {
                        userTokensResponseFactory.createResponseToken(refundTokenCurrency)
                    },
                ),
            )

            mutablePreferences.setObjectMap(
                key = PreferencesKeys.SWAP_TRANSACTIONS_STATUSES_KEY,
                value = updatesMap,
            )
        }
    }

    override suspend fun getLastSwappedCryptoCurrencyId(userWalletId: UserWalletId): String? {
        val lastSwappedCurrencies = appPreferencesStore.getObjectListSync<LastSwappedCryptoCurrencyDTO>(
            key = PreferencesKeys.LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY,
        )

        return lastSwappedCurrencies.find { userWalletId.stringValue == it.userWalletId }?.cryptoCurrencyId
    }

    override suspend fun storeLastSwappedCryptoCurrencyId(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
    ) {
        appPreferencesStore.editData { mutablePreferences ->
            val lastSwappedCryptoCurrencies: List<LastSwappedCryptoCurrencyDTO>? = mutablePreferences.getObjectList(
                key = PreferencesKeys.LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY,
            )

            val newList = if (lastSwappedCryptoCurrencies != null) {
                lastSwappedCryptoCurrencies.filter {
                    it.userWalletId != userWalletId.stringValue
                } + LastSwappedCryptoCurrencyDTO(userWalletId.stringValue, cryptoCurrencyId.value)
            } else {
                listOf(LastSwappedCryptoCurrencyDTO(userWalletId.stringValue, cryptoCurrencyId.value))
            }

            mutablePreferences.setObjectList(
                key = PreferencesKeys.LAST_SWAPPED_CRYPTOCURRENCY_ID_KEY,
                value = newList,
            )
        }
    }

    private fun SwapTransactionListDTO.checkId(
        checkUserWalletId: UserWalletId,
        fromCurrencyId: CryptoCurrency.ID,
        toCurrencyId: CryptoCurrency.ID,
    ): Boolean {
        return userWalletId == checkUserWalletId.stringValue &&
            toCryptoCurrencyId == toCurrencyId.value &&
            fromCryptoCurrencyId == fromCurrencyId.value
    }

    private fun List<SwapTransactionListDTO>.updateList(
        userWalletId: UserWalletId,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        transactions: List<SwapTransactionDTO>,
    ): List<SwapTransactionListDTO> {
        return addOrReplace(
            item = listConverter.default(
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
            val savedList = mutablePreferences.getObjectMap<SwapStatusModel>(
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