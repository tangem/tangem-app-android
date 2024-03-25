package com.tangem.data.tokens.repository

import com.tangem.blockchain.blockchains.polkadot.AccountCheckProvider
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS
import com.tangem.datasource.local.preferences.PreferencesKeys.POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS
import com.tangem.datasource.local.preferences.PreferencesKeys.POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.datasource.local.preferences.utils.getObjectMap
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.PolkadotAccountHealthCheckRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class DefaultPolkadotAccountHealthCheckRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val appPreferencesStore: AppPreferencesStore,
) : PolkadotAccountHealthCheckRepository {

    private val hasImmortalTransaction = MutableSharedFlow<Boolean>()
    private val hasResetTransaction = MutableSharedFlow<Boolean>()

    private val mutex = Mutex()

    override suspend fun runCheck(userWalletId: UserWalletId, network: Network) {
        // Run Polkadot account health check
        if (Blockchain.fromId(network.id.value) != Blockchain.Polkadot) return

        mutex.withLock {
            val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
            val accountCheckProvider = requireNotNull(walletManager as AccountCheckProvider) {
                Timber.e("Unable to cast wallet manager to AccountCheckProvider")
                return
            }
            val address = walletManager.wallet.address

            checkHasReset(accountCheckProvider, address)
            checkHasImmortal(accountCheckProvider, address)
        }
    }

    override fun subscribeToHasImmortalResults() = hasImmortalTransaction.asSharedFlow()

    override fun subscribeToHasResetResults() = hasResetTransaction.asSharedFlow()

    private suspend fun checkHasReset(polkadotManager: AccountCheckProvider, address: String) {
        val checkedAddresses = appPreferencesStore.getObjectListSync<String>(POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS)
        if (checkedAddresses.contains(address)) return
        runCatching {
            val accountInfo = requireNotNull(polkadotManager.getAccountInfo().account) {
                Timber.e("Account info is null")
            }
            val nonce = accountInfo.nonce
            val extrinsicCount = accountInfo.countExtrinsic

            // Account was reset
            if (nonce != null && extrinsicCount != null) {
                hasResetTransaction.emit(nonce < extrinsicCount)
                updateCheckedResetAddressesList(address)
            }
        }.onFailure {
            if ((it as? BlockchainSdkError.CustomError)?.customMessage == ACCOUNT_NOT_FOUND) {
                updateCheckedResetAddressesList(address)
            }
        }
    }

    private suspend fun checkHasImmortal(polkadotManager: AccountCheckProvider, address: String) {
        val checkedAddresses = appPreferencesStore.getObjectListSync<String>(POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS)
        if (checkedAddresses.contains(address)) return

        runCatching {
            do {
                // Getting batch of extrinsics to check
                val lastChecked = appPreferencesStore.getObjectMap<Long>(POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX)
                val lastExtrinsic = lastChecked[address]
                val extrinsicListResult = polkadotManager.getExtrinsicList(afterExtrinsicId = lastExtrinsic)
                // Checking extrinsic one by one
                extrinsicListResult.extrinsic?.forEach { tx ->
                    val hash = tx.hash
                    val id = tx.id
                    if (hash != null && id != null) {
                        val details = polkadotManager.getExtrinsicDetail(hash)

                        // We found an `immortal` transaction
                        if (details.lifetime == null) {
                            hasImmortalTransaction.emit(true)
                            updateCheckedImmutableAddressesList(address)
                            clearLastCheckedTransaction(address)
                            return
                        }

                        // Saving last checked transaction
                        updateLastCheckedTransaction(address, id)
                    }
                }
            } while (!extrinsicListResult.extrinsic.isNullOrEmpty())

            // We checked all transactions up to current moment and did not found an `immortal` transaction
            hasImmortalTransaction.emit(false)
            updateCheckedImmutableAddressesList(address)
            clearLastCheckedTransaction(address)
        }
    }

    private suspend fun updateLastCheckedTransaction(address: String, txId: Long) {
        appPreferencesStore.editData {
            val savedList = it.getObjectMap<Long>(POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX)
            val updatedList = savedList.toMutableMap()
            updatedList[address] = txId
            it.setObjectMap(POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX, updatedList)
        }
    }

    private suspend fun clearLastCheckedTransaction(address: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val savedList = mutablePreferences.getObjectMap<Long>(POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX)
            val updatedList = savedList.toMutableMap()
            updatedList.remove(address)
            mutablePreferences.setObjectMap(POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX, updatedList)
        }
    }

    private suspend fun updateCheckedImmutableAddressesList(address: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val savedList = mutablePreferences.getObjectList<String>(POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS)
            val updatedList = savedList?.toMutableList()
            updatedList?.addOrReplace(address) { it == address }

            if (updatedList != null) {
                mutablePreferences.setObjectList(
                    POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS,
                    updatedList.toList(),
                )
            } else {
                mutablePreferences.remove(POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS)
            }
        }
    }

    private suspend fun updateCheckedResetAddressesList(address: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val savedList = mutablePreferences.getObjectList<String>(POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS)
            val updatedList = savedList?.toMutableList()
            updatedList?.addOrReplace(address) { it == address }

            if (updatedList != null) {
                mutablePreferences.setObjectList(
                    POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS,
                    updatedList.toList(),
                )
            } else {
                mutablePreferences.remove(POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS)
            }
        }
    }

    private companion object {
        const val ACCOUNT_NOT_FOUND = "Record Not Found"
    }
}