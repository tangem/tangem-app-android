package com.tangem.data.pay

import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

internal class DefaultTangemPayEligibilityManager @Inject constructor(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val onboardingRepository: OnboardingRepository,
) : TangemPayEligibilityManager {

    private var cachedEligibleWallets: List<UserWallet>? = null
    private var eligibleWalletsDeferred: Deferred<List<UserWallet>>? = null
    private val loadMutex = Mutex()

    override suspend fun getEligibleWallets(): List<UserWallet> {
        cachedEligibleWallets?.let { return it }

        return loadMutex.withLock {
            cachedEligibleWallets?.let { return it }
            eligibleWalletsDeferred?.let { return it.await() }

            coroutineScope {
                val deferred = async {
                    getPossibleWalletsForTangemPay()
                        .excludePaeraCustomers()
                        .also { cachedEligibleWallets = it }
                }
                eligibleWalletsDeferred = deferred
                try {
                    deferred.await()
                } finally {
                    eligibleWalletsDeferred = null
                }
            }
        }
    }

    private suspend fun getPossibleWalletsForTangemPay(): List<UserWallet> {
        if (!onboardingRepository.checkCustomerEligibility()) {
            return emptyList()
        }

        val wallets = if (hotWalletFeatureToggles.isHotWalletEnabled) {
            userWalletsListRepository.userWallets.value
        } else {
            userWalletsListManager.userWalletsSync
        } ?: return emptyList()

        return wallets.filter { wallet ->
            wallet.isMultiCurrency && !wallet.isLocked && wallet.isCompatible()
        }
    }

    private fun UserWallet.isCompatible(): Boolean = when (this) {
        is UserWallet.Cold ->
            scanResponse.card.firmwareVersion >= FirmwareVersion.HDWalletAvailable
        is UserWallet.Hot -> true
    }

    private suspend fun List<UserWallet>.excludePaeraCustomers(): List<UserWallet> {
        if (isEmpty()) return this

        return coroutineScope {
            map { wallet ->
                async {
                    val isCustomer = onboardingRepository
                        .checkCustomerWallet(wallet.walletId)
                        .getOrNull() == true
                    wallet to isCustomer
                }
            }
                .awaitAll()
                .mapNotNull { (wallet, isCustomer) ->
                    wallet.takeUnless { isCustomer }
                }
        }
    }
}