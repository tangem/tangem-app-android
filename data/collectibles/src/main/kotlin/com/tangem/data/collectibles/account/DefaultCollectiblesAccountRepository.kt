package com.tangem.data.collectibles.account

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.data.collectibles.di.CollectiblesRelinkTimeSource
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.data.common.account.toWalletAccountDTO
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.datasource.api.tangemTech.models.account.isCollectibles
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Total by design: every operation degrades to "nothing recorded" instead of throwing. The record only ever saves
 * a chain read, so losing it costs one — while a throw here would surface as a crash on opening the feature, or as
 * a failed report for an onboarding that actually succeeded.
 */
internal class DefaultCollectiblesAccountRepository @Inject constructor(
    private val collectiblesAccountStore: CollectiblesAccountStore,
    @CollectiblesRelinkTimeSource private val timeSource: TimeSource,
    private val walletAccountsFetcher: WalletAccountsFetcher,
    private val walletAccountsSaver: WalletAccountsSaver,
) : CollectiblesAccountRepository {

    /** Runtime only: a relink in flight matters for the next two minutes, not for the next launch. */
    private val relinksSent = ConcurrentHashMap<UserWalletId, TimeMark>()

    override fun getStatus(userWalletId: UserWalletId): Flow<CollectiblesAccountStatusValue?> =
        collectiblesAccountStore.get(userWalletId)

    override suspend fun getStatusSync(userWalletId: UserWalletId): CollectiblesAccountStatusValue? =
        runSuspendCatching { collectiblesAccountStore.getSyncOrNull(userWalletId) }
            .onFailure { TangemLogger.e("Failed to read the collectibles account of $userWalletId", it) }
            .getOrNull()

    override suspend fun registerAccount(userWalletId: UserWalletId): Either<CollectiblesAccountError, Unit> =
        runSuspendCatching { pushRecord(userWalletId) }
            .onFailure { TangemLogger.e("Failed to register the collectibles account of $userWalletId", it) }
            .fold(
                onSuccess = { Unit.right() },
                onFailure = { CollectiblesAccountError.RegistrationFailed(cause = it).left() },
            )

    override suspend fun saveAccount(userWalletId: UserWalletId, status: CollectiblesAccountStatusValue) {
        runSuspendCatching { collectiblesAccountStore.store(userWalletId = userWalletId, value = status) }
            .onFailure { TangemLogger.e("Failed to record the collectibles account of $userWalletId", it) }
    }

    override fun markRelinkSent(userWalletId: UserWalletId) {
        relinksSent[userWalletId] = timeSource.markNow()
    }

    override fun isRelinkPending(userWalletId: UserWalletId): Boolean {
        val sentAt = relinksSent[userWalletId] ?: return false
        if (sentAt.elapsedNow() < RELINK_LANDING_TIMEOUT) return true

        relinksSent.remove(userWalletId, sentAt)
        return false
    }

    override fun clearRelinkPending(userWalletId: UserWalletId) {
        relinksSent.remove(userWalletId)
    }

    override suspend fun clearAccount(userWalletId: UserWalletId) {
        clearRelinkPending(userWalletId)
        runSuspendCatching { collectiblesAccountStore.clear(userWalletId) }
            .onFailure { TangemLogger.e("Failed to clear the collectibles account of $userWalletId", it) }
    }

    override suspend fun unregisterAccount(userWalletId: UserWalletId) {
        runSuspendCatching {
            val response = walletAccountsFetcher.fetch(userWalletId = userWalletId)
            if (response.accounts.none(WalletAccountDTO::isCollectibles)) return@runSuspendCatching

            val syncedResponse = walletAccountsSaver.push(
                userWalletId = userWalletId,
                accounts = response.accounts.filterNot(WalletAccountDTO::isCollectibles),
            ) ?: error("Failed to unregister the collectibles account of $userWalletId")

            walletAccountsSaver.store(userWalletId = userWalletId, response = syncedResponse)
        }
            .onFailure { TangemLogger.e("Failed to unregister the collectibles account of $userWalletId", it) }
    }

    private suspend fun pushRecord(userWalletId: UserWalletId) {
        val response = walletAccountsFetcher.fetch(userWalletId = userWalletId)
        if (response.accounts.any(WalletAccountDTO::isCollectibles)) return

        val syncedResponse = walletAccountsSaver.push(
            userWalletId = userWalletId,
            accounts = response.accounts + Account.Collectibles(userWalletId).toWalletAccountDTO(),
        ) ?: error("Failed to register the collectibles account of $userWalletId")

        walletAccountsSaver.store(userWalletId = userWalletId, response = syncedResponse)
        TangemLogger.i("Collectibles account of $userWalletId registered")
    }

    private companion object {
        // A Solana transaction expires ~150 blocks (about 90 s) after its blockhash; the rest covers the vault
        // read lagging behind the chain
        val RELINK_LANDING_TIMEOUT = 120.seconds
    }
}