package com.tangem.data.onramp

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSet
import com.tangem.datasource.local.preferences.utils.getObjectSetSync
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DefaultOnrampTransactionRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : OnrampTransactionRepository {

    override suspend fun storeTransaction(transaction: OnrampTransaction) {
        withContext(dispatchers.io) {
            appPreferencesStore.editData { mutablePreferences ->
                val stored = mutablePreferences.getObjectSet<OnrampTransaction>(
                    PreferencesKeys.ONRAMP_TRANSACTIONS_STATUSES_KEY,
                )
                val updated = stored?.toMutableSet()
                    ?.addOrReplace(transaction) { it.txId == transaction.txId }
                    ?: mutableSetOf(transaction)

                mutablePreferences.setObjectSet(
                    key = PreferencesKeys.ONRAMP_TRANSACTIONS_STATUSES_KEY,
                    value = updated,
                )
            }
        }
    }

    override suspend fun getTransactionById(txId: String): OnrampTransaction? = withContext(dispatchers.io) {
        val stored = appPreferencesStore.getObjectSetSync<OnrampTransaction>(
            PreferencesKeys.ONRAMP_TRANSACTIONS_STATUSES_KEY,
        )

        stored.firstOrNull { it.txId == txId }
    }

    override fun getTransactions(
        userWalletId: UserWalletId,
        toCryptoCurrency: CryptoCurrency.ID,
    ): Flow<List<OnrampTransaction>> = appPreferencesStore
        .getObjectSet<OnrampTransaction>(PreferencesKeys.ONRAMP_TRANSACTIONS_STATUSES_KEY)
        .map { transactions ->
            transactions.filter {
                it.userWalletId == userWalletId && it.toCurrency.id == toCryptoCurrency
            }
        }

    override suspend fun removeTransaction(txId: String) {
        withContext(dispatchers.io) {
            appPreferencesStore.editData { mutablePreferences ->
                runCatching {
                    val stored = mutablePreferences.getObjectSet<OnrampTransaction>(
                        PreferencesKeys.ONRAMP_TRANSACTIONS_STATUSES_KEY,
                    )?.toMutableSet()

                    stored?.removeIf { it.txId == txId }

                    mutablePreferences.setObjectSet(
                        key = PreferencesKeys.ONRAMP_TRANSACTIONS_STATUSES_KEY,
                        value = stored ?: emptySet(),
                    )
                }
            }
        }
    }
}
