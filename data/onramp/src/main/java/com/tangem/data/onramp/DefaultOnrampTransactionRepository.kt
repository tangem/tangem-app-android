package com.tangem.data.onramp

import com.tangem.data.onramp.converters.TransactionConverter
import com.tangem.data.onramp.models.OnrampTerminalTransactionDTO
import com.tangem.data.onramp.models.OnrampTransactionDTO
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSet
import com.tangem.datasource.local.preferences.utils.getObjectSetSync
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

internal class DefaultOnrampTransactionRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val growPreferencesKeys: GrowPreferencesKeys,
    private val dispatchers: CoroutineDispatcherProvider,
) : OnrampTransactionRepository {

    private val transactionConverter = TransactionConverter()

    override suspend fun storeTransaction(transaction: OnrampTransaction) {
        withContext(dispatchers.io) {
            appPreferencesStore.editData { mutablePreferences ->
                val stored = mutablePreferences.getObjectSet<OnrampTransactionDTO>(
                    growPreferencesKeys.onrampTransactionsStatusesKey,
                )?.map(transactionConverter::convert) ?: mutableSetOf()

                val updated = stored.toMutableSet()
                    .addOrReplace(transaction) { it.txId == transaction.txId }
                    .map(transactionConverter::convertBack)
                    .toSet()

                mutablePreferences.setObjectSet<OnrampTransactionDTO>(
                    key = growPreferencesKeys.onrampTransactionsStatusesKey,
                    value = updated,
                )
            }
        }
    }

    override fun getAllTransactions(): Flow<List<OnrampTransaction>> {
        return appPreferencesStore
            .getObjectSet<OnrampTransactionDTO>(growPreferencesKeys.onrampTransactionsStatusesKey)
            .onStart { cleanupExpiredTerminalTransactions() }
            .map { transactions -> transactions.map(transactionConverter::convert) }
    }

    override suspend fun getTransactionById(txId: String): OnrampTransaction? = withContext(dispatchers.io) {
        val stored = appPreferencesStore.getObjectSetSync<OnrampTransactionDTO>(
            growPreferencesKeys.onrampTransactionsStatusesKey,
        ).map(transactionConverter::convert)

        stored.firstOrNull { it.txId == txId }
    }

    override fun getTransactions(
        userWalletId: UserWalletId,
        cryptoCurrencyId: CryptoCurrency.ID,
    ): Flow<List<OnrampTransaction>> = appPreferencesStore
        .getObjectSet<OnrampTransactionDTO>(growPreferencesKeys.onrampTransactionsStatusesKey)
        .onStart { cleanupExpiredTerminalTransactions() }
        .map { transactions ->
            transactions.filter {
                it.userWalletId == userWalletId && it.toCurrencyId == cryptoCurrencyId.value
            }.map(transactionConverter::convert)
        }

    override suspend fun updateTransactionStatus(
        txId: String,
        externalTxId: String,
        externalTxUrl: String,
        status: OnrampStatus.Status,
    ) = withContext(dispatchers.io) {
        val updatedTx = getTransactionById(txId)?.copy(
            externalTxUrl = externalTxUrl,
            externalTxId = externalTxId,
            status = status,
        ) ?: return@withContext
        storeTransaction(updatedTx)
    }

    override suspend fun removeTransaction(txId: String) {
        withContext(dispatchers.io) {
            appPreferencesStore.editData { mutablePreferences ->
                runCatching {
                    val stored = mutablePreferences.getObjectSet<OnrampTransactionDTO>(
                        growPreferencesKeys.onrampTransactionsStatusesKey,
                    )?.toMutableSet()

                    stored?.removeIf { it.txId == txId }

                    mutablePreferences.setObjectSet<OnrampTransactionDTO>(
                        key = growPreferencesKeys.onrampTransactionsStatusesKey,
                        value = stored.orEmpty(),
                    )
                }
            }
        }
    }

    override suspend fun isHandledTransaction(txId: String): Boolean = withContext(dispatchers.io) {
        val terminated = appPreferencesStore.getObjectSetSync<OnrampTerminalTransactionDTO>(
            growPreferencesKeys.onrampHandledTransactionsKey,
        )

        terminated.any { it.txId == txId }
    }

    override suspend fun storeHandledTransaction(txId: String) {
        withContext(dispatchers.io) {
            appPreferencesStore.editData { mutablePreferences ->
                runCatching {
                    val archived = mutablePreferences.getObjectSet<OnrampTerminalTransactionDTO>(
                        growPreferencesKeys.onrampHandledTransactionsKey,
                    ).orEmpty().toMutableSet()

                    val record = OnrampTerminalTransactionDTO(
                        txId = txId,
                        terminatedAt = System.currentTimeMillis(),
                    )

                    archived.add(record)

                    mutablePreferences.setObjectSet(
                        key = growPreferencesKeys.onrampHandledTransactionsKey,
                        value = archived,
                    )
                }
            }
        }
    }

    private suspend fun cleanupExpiredTerminalTransactions() {
        withContext(dispatchers.io) {
            appPreferencesStore.editData { mutablePreferences ->
                runCatching {
                    val archived = mutablePreferences.getObjectSet<OnrampTerminalTransactionDTO>(
                        growPreferencesKeys.onrampHandledTransactionsKey,
                    )?.toMutableSet() ?: return@editData

                    val oneWeekAgo = System.currentTimeMillis() -
                        TimeUnit.DAYS.toMillis(HANDLED_TRANSACTIONS_RETENTION_DAYS)
                    val cleaned = archived
                        .filterTo(mutableSetOf()) { it.terminatedAt > oneWeekAgo }

                    if (cleaned.size != archived.size) {
                        mutablePreferences.setObjectSet(
                            key = growPreferencesKeys.onrampHandledTransactionsKey,
                            value = cleaned,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val HANDLED_TRANSACTIONS_RETENTION_DAYS = 7L
    }
}