package com.tangem.data.pay

import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

internal class DefaultTangemPayEligibilityManager @Inject constructor(
    dispatchers: CoroutineDispatcherProvider,
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val onboardingRepository: OnboardingRepository,
) : TangemPayEligibilityManager {

    private var cachedEligibleWallets: List<UserWalletData>? = null
    private var eligibleWalletsDeferred: Deferred<List<UserWalletData>>? = null
    private val loadMutex = Mutex()
    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default)

    init {
        resetDataWhenWalletsUpdate()
    }

    override suspend fun getEligibleWallets(shouldExcludePaeraCustomers: Boolean): List<UserWallet> {
        return getUserWalletsData().mapNotNull {
            if (!it.isPaeraCustomer || !shouldExcludePaeraCustomers) it.userWallet else null
        }
    }

    override suspend fun getPossibleWalletsIds(shouldExcludePaeraCustomers: Boolean): List<UserWalletId> {
        return getPossibleWalletsForTangemPay().addPaeraCustomersData().mapNotNull {
            if (!it.isPaeraCustomer || !shouldExcludePaeraCustomers) it.userWallet.walletId else null
        }
    }

    override suspend fun getTangemPayAvailability(): Boolean {
        return onboardingRepository.checkCustomerEligibility()
            .also { isEligible -> if (!isEligible) reset() }
    }

    override suspend fun isPaeraCustomerForAnyWallet(): Boolean {
        return getUserWalletsData().any { it.isPaeraCustomer }
    }

    private suspend fun getUserWalletsData(): List<UserWalletData> {
        cachedEligibleWallets?.let { return it }

        return loadMutex.withLock {
            cachedEligibleWallets?.let { return it }
            eligibleWalletsDeferred?.let { return it.await() }

            coroutineScope {
                val deferred = async {
                    if (!checkTangemPayEligibility()) {
                        emptyList()
                    } else {
                        getPossibleWalletsForTangemPay()
                            .addPaeraCustomersData()
                            .also { cachedEligibleWallets = it }
                    }
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

    private fun getPossibleWalletsForTangemPay(): List<UserWallet> {
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
        is UserWallet.Hot -> hotWalletId.authType != HotWalletId.AuthType.NoPassword
    }

    private suspend fun List<UserWallet>.addPaeraCustomersData(): List<UserWalletData> {
        if (isEmpty()) return emptyList()

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
                .map { (userWallet, isPaeraCustomer) ->
                    UserWalletData(userWallet, isPaeraCustomer)
                }
        }
    }

    private fun resetDataWhenWalletsUpdate() {
        coroutineScope.launch {
            if (hotWalletFeatureToggles.isHotWalletEnabled) {
                userWalletsListRepository.userWallets.collectLatest { reset() }
            } else {
                userWalletsListManager.userWallets.collectLatest { reset() }
            }
        }
    }

    override fun reset() {
        cachedEligibleWallets = null
        eligibleWalletsDeferred?.cancel()
        eligibleWalletsDeferred = null
    }

    private suspend fun checkTangemPayEligibility(): Boolean {
        return onboardingRepository.getCustomerEligibility() || onboardingRepository.checkCustomerEligibility()
    }

    private data class UserWalletData(
        val userWallet: UserWallet,
        val isPaeraCustomer: Boolean,
    )
}