package com.tangem.data.staking

import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.request.*
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.domain.staking.model.UnsubmittedTransactionMetadata
import com.tangem.domain.staking.repositories.StakingTransactionHashRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class DefaultStakingTransactionHashRepository(
    private val stakeKitApi: StakeKitApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : StakingTransactionHashRepository {

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
                    key = PreferencesKeys.UNSUBMITTED_TRANSACTIONS_KEY,
                    default = emptyList(),
                )

                preferences.setObjectList(
                    key = PreferencesKeys.UNSUBMITTED_TRANSACTIONS_KEY,
                    value = savedTransactions + unsubmittedTransactionMetadata,
                )
            }
        }
    }

    override suspend fun sendUnsubmittedHashes() {
        withContext(dispatchers.io) {
            val savedTransactions = appPreferencesStore.getObjectListSync<UnsubmittedTransactionMetadata>(
                key = PreferencesKeys.UNSUBMITTED_TRANSACTIONS_KEY,
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
                                key = PreferencesKeys.UNSUBMITTED_TRANSACTIONS_KEY,
                                default = emptyList(),
                            ).filterNot { it.transactionId == transaction.transactionId }

                        mutablePreferences.setObjectList(
                            key = PreferencesKeys.UNSUBMITTED_TRANSACTIONS_KEY,
                            value = updatedTransactions,
                        )
                    }
                } catch (e: Exception) {
                    val logMessage = buildString {
                        append("Error while submitting transaction with\n")
                        append("StakeKit id = ${transaction.transactionId} and\n")
                        append("transaction hash = ${transaction.transactionHash}")
                    }
                    Timber.e(logMessage)
                }
            }
        }
    }
}