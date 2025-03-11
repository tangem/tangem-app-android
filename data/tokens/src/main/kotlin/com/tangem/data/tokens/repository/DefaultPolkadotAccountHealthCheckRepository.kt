package com.tangem.data.tokens.repository

import androidx.datastore.preferences.core.Preferences
import com.tangem.blockchain.blockchains.polkadot.AccountCheckProvider
import com.tangem.blockchain.blockchains.polkadot.network.accounthealthcheck.ExtrinsicListItemResponse
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys.POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX_KEY
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.datasource.local.preferences.utils.getObjectSetSync
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.PolkadotAccountHealthCheckRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

internal class DefaultPolkadotAccountHealthCheckRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolkadotAccountHealthCheckRepository {

    private val hasImmortalTransaction = MutableSharedFlow<Pair<String, Boolean>>()
    private val hasResetTransaction = MutableSharedFlow<Pair<String, Boolean>>()

    private val mutex = Mutex()
    private val mutexes = ConcurrentHashMap<String, Mutex>()

    override suspend fun runCheck(userWalletId: UserWalletId, network: Network) {
        // Run Polkadot account health check
        if (Blockchain.fromId(network.id.value) != Blockchain.Polkadot) return

        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, network)
        val address = requireNotNull(walletManager?.wallet?.address) {
            Timber.e("Address is null")
            return
        }
        val accountCheckProvider = requireNotNull(walletManager as? AccountCheckProvider) {
            Timber.e("Unable to cast wallet manager to AccountCheckProvider")
            return
        }

        // use a separate mutexForKey for each key to avoid multiple calls block() to the same key
        // also used mutex to safe create mutexForKey, otherwise it can lead to multiple calls for the same key
        val mutexForKey = mutex.withLock { mutexes.getOrPut(address) { Mutex() } }
        mutexForKey.withLock {
            withContext(dispatchers.io) {
                checkHasReset(accountCheckProvider, address)
                checkHasImmortal(accountCheckProvider, address)
            }
        }
    }

    override fun subscribeToHasImmortalResults() = hasImmortalTransaction.asSharedFlow()

    override fun subscribeToHasResetResults() = hasResetTransaction.asSharedFlow()

    private suspend fun checkHasReset(polkadotManager: AccountCheckProvider, address: String) {
        val checkedAddresses = appPreferencesStore.getObjectSetSync<String>(POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS_KEY)
        if (checkedAddresses.contains(address)) return
        runCatching {
            val accountInfo = requireNotNull(polkadotManager.getAccountInfo().account) {
                Timber.e("Account info is null")
            }
            val nonce = accountInfo.nonce
            val extrinsicCount = accountInfo.countExtrinsic

            // Account was reset
            if (nonce != null && extrinsicCount != null) {
                val hasReset = nonce < extrinsicCount
                updateChecked(address, POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS_KEY)
                hasResetTransaction.emit(address to hasReset)
            }
        }.onFailure {
            if ((it as? BlockchainSdkError.CustomError)?.customMessage == ACCOUNT_NOT_FOUND) {
                updateChecked(address, POLKADOT_HEALTH_CHECKED_RESET_ACCOUNTS_KEY)
            }
        }
    }

    private suspend fun checkHasImmortal(accountCheckerProvider: AccountCheckProvider, address: String) {
        val checkedAddresses =
            appPreferencesStore.getObjectSetSync<String>(POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS_KEY)
        if (checkedAddresses.contains(address)) return

        runCatching {
            do {
                // Getting batch of extrinsics to check
                val lastChecked = appPreferencesStore.getObjectMapSync<Long>(POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX_KEY)
                val lastExtrinsic = lastChecked[address]
                val extrinsicListResult = accountCheckerProvider.getExtrinsicList(afterExtrinsicId = lastExtrinsic)
                extrinsicListResult.extrinsic
                    ?.forEach { tx ->
                        // Checking extrinsic one by one
                        if (checkTx(tx, address, accountCheckerProvider)) return
                    }
            } while (!extrinsicListResult.extrinsic.isNullOrEmpty())

            // We checked all transactions up to current moment and did not found an `immortal` transaction
            hasImmortalTransaction.emit(address to false)
            updateChecked(address, POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS_KEY)
            clearLastCheckedTransaction(address)
        }
    }

    private suspend fun checkTx(
        tx: ExtrinsicListItemResponse,
        address: String,
        accountCheckerProvider: AccountCheckProvider,
    ): Boolean {
        val hash = tx.hash
        val id = tx.id
        if (hash != null && id != null) {
            val details = accountCheckerProvider.getExtrinsicDetail(hash)

            // We found an `immortal` transaction
            if (details.lifetime == null) {
                hasImmortalTransaction.emit(address to true)
                updateChecked(address, POLKADOT_HEALTH_CHECKED_IMMUTABLE_ACCOUNTS_KEY)
                clearLastCheckedTransaction(address)
                return true
            }

            // Saving last checked transaction
            updateLastCheckedTransaction(address, id)
        }
        return false
    }

    private suspend fun updateLastCheckedTransaction(address: String, txId: Long) {
        appPreferencesStore.editData {
            val savedList = it.getObjectMap<Long>(POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX_KEY)
            val updatedList = savedList.toMutableMap()
            updatedList[address] = txId
            it.setObjectMap(POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX_KEY, updatedList)
        }
    }

    private suspend fun clearLastCheckedTransaction(address: String) {
        appPreferencesStore.editData { mutablePreferences ->
            val savedList = mutablePreferences.getObjectMap<Long>(POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX_KEY)
            val updatedList = savedList.toMutableMap()
            updatedList.remove(address)
            mutablePreferences.setObjectMap(POLKADOT_HEALTH_CHECK_LAST_INDEXED_TX_KEY, updatedList)
        }
    }

    private suspend fun updateChecked(address: String, key: Preferences.Key<String>) {
        appPreferencesStore.editData { mutablePreferences ->
            val savedList = mutablePreferences.getObjectSet<String>(key)
            val updatedList = savedList?.toMutableSet() ?: mutableSetOf()
            updatedList.add(address)

            if (updatedList.isNotEmpty()) {
                mutablePreferences.setObjectSet(
                    key,
                    updatedList.toSet(),
                )
            } else {
                mutablePreferences.remove(key)
            }
        }
    }

    private companion object {
        const val ACCOUNT_NOT_FOUND = "Record Not Found"
    }
}