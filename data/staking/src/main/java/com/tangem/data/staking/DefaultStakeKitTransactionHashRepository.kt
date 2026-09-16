package com.tangem.data.staking

import com.tangem.grow.datasource.stakekit.StakeKitApi
import com.tangem.grow.datasource.stakekit.models.request.SubmitTransactionHashRequestBody
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.domain.staking.model.UnsubmittedTransactionMetadata
import com.tangem.domain.staking.repositories.StakeKitTransactionHashRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext

internal class DefaultStakeKitTransactionHashRepository(
    private val stakeKitApi: StakeKitApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val growPreferencesKeys: GrowPreferencesKeys,
    private val dispatchers: CoroutineDispatcherProvider,
) : StakeKitTransactionHashRepository {

    override suspend fun submitHash(transactionId: String, transactionHash: String) {
        withContext(dispatchers.io) {
            stakeKitApi.submitTransactionHash(
                transactionId = transactionId,
                body = SubmitTransactionHashRequestBody(
                    hash = transactionHash,
                ),
            )
        }
    }

    override suspend fun storeUnsubmittedHash(unsubmittedTransactionMetadata: UnsubmittedTransactionMetadata) {
        withContext(dispatchers.io) {
            appPreferencesStore.editData { preferences ->
                val savedTransactions = preferences.getObjectListOrDefault<UnsubmittedTransactionMetadata>(
                    key = growPreferencesKeys.unsubmittedTransactionsKey,
                    default = emptyList(),
                )

                preferences.setObjectList(
                    key = growPreferencesKeys.unsubmittedTransactionsKey,
                    value = savedTransactions + unsubmittedTransactionMetadata,
                )
            }
        }
    }

    override suspend fun sendUnsubmittedHashes() {
        withContext(dispatchers.io) {
            val savedTransactions = appPreferencesStore.getObjectListSync<UnsubmittedTransactionMetadata>(
                key = growPreferencesKeys.unsubmittedTransactionsKey,
            )

            savedTransactions.forEach { transaction ->
                try {
                    stakeKitApi.submitTransactionHash(
                        transactionId = transaction.transactionId,
                        body = SubmitTransactionHashRequestBody(hash = transaction.transactionHash),
                    )

                    appPreferencesStore.editData { mutablePreferences ->
                        val updatedTransactions =
                            mutablePreferences.getObjectListOrDefault<UnsubmittedTransactionMetadata>(
                                key = growPreferencesKeys.unsubmittedTransactionsKey,
                                default = emptyList(),
                            ).filterNot { it.transactionId == transaction.transactionId }

                        mutablePreferences.setObjectList(
                            key = growPreferencesKeys.unsubmittedTransactionsKey,
                            value = updatedTransactions,
                        )
                    }
                } catch (e: Exception) {
                    val logMessage = buildString {
                        append("Error while submitting transaction with\n")
                        append("StakeKit id = ${transaction.transactionId} and\n")
                        append("transaction hash = ${transaction.transactionHash}")
                    }
                    TangemLogger.e(logMessage)
                }
            }
        }
    }
}