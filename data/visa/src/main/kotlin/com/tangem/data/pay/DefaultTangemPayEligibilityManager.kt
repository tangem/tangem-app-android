package com.tangem.data.pay

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.pay.isTangemPayType
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.models.wallet.isTangemPayCompatible
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.TangemPayEntryPoint
import com.tangem.domain.pay.repository.OnboardingRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

internal class DefaultTangemPayEligibilityManager @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val coroutineScope: AppCoroutineScope,
    private val onboardingRepository: OnboardingRepository,
) : TangemPayEligibilityManager {

    private var cachedEligibleWallets: Map<TangemPayEntryPoint?, List<UserWalletData>> = emptyMap()
    private var eligibleWalletsDeferred: Deferred<List<UserWalletData>>? = null
    private val loadMutex = Mutex()

    init {
        resetDataWhenWalletsUpdate()
    }

    override suspend fun getEligibleWallets(
        shouldExcludePaeraCustomers: Boolean,
        entryPoint: TangemPayEntryPoint?,
    ): List<UserWallet> {
        return getEligibleWalletsData(entryPoint = entryPoint).mapNotNull {
            if (!it.isPaeraCustomer || !shouldExcludePaeraCustomers) it.userWallet else null
        }
    }

    override suspend fun getCompatibleWalletsIds(shouldExcludePaeraCustomers: Boolean): List<UserWalletId> {
        return getCompatibleWallets().addPaeraCustomersData().mapNotNull {
            if (!it.isPaeraCustomer || !shouldExcludePaeraCustomers) it.userWallet.walletId else null
        }
    }

    override suspend fun getTangemPayAvailability(entryPoint: TangemPayEntryPoint): Boolean {
        val eligibility = onboardingRepository.checkCustomerEligibility()
        val type = entryPoint.toEligibilityType()
        return eligibility.any { it == type }
            .also { isEligible -> if (!isEligible) reset() }
    }

    override suspend fun isPaeraCustomerForAnyWallet(entryPoint: TangemPayEntryPoint): Boolean {
        return getEligibleWalletsData(entryPoint = entryPoint).any { it.isPaeraCustomer }
    }

    private suspend fun getEligibleWalletsData(entryPoint: TangemPayEntryPoint?): List<UserWalletData> {
        cachedEligibleWallets[entryPoint]?.let { return it }

        return loadMutex.withLock {
            cachedEligibleWallets[entryPoint]?.let { return it }
            eligibleWalletsDeferred?.let { return it.await() }

            coroutineScope {
                val deferred = async {
                    getCompatibleWallets()
                        .takeIf { it.isNotEmpty() && hasEligibleChannel(entryPoint = entryPoint) }
                        .orEmpty()
                        .addPaeraCustomersData()
                        .also { cachedEligibleWallets += mapOf(entryPoint to it) }
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

    private suspend fun getCompatibleWallets(): List<UserWallet> {
        val wallets = userWalletsListRepository.userWallets.value ?: return emptyList()

        return wallets.filter { wallet ->
            wallet.isMultiCurrency && !wallet.isLocked && wallet.isTangemPayCompatible &&
                !onboardingRepository.isTangemPayDeactivated(wallet.walletId)
        }
    }

    private suspend fun List<UserWallet>.addPaeraCustomersData(): List<UserWalletData> {
        if (isEmpty()) return emptyList()

        return coroutineScope {
            map { wallet ->
                async {
                    val isCustomer = onboardingRepository
                        .hasTangemPayInWallet(wallet.walletId)
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
            userWalletsListRepository.userWallets.collectLatest { reset() }
        }
    }

    override fun reset() {
        cachedEligibleWallets = emptyMap()
        eligibleWalletsDeferred?.cancel()
        eligibleWalletsDeferred = null
    }

    private suspend fun hasEligibleChannel(entryPoint: TangemPayEntryPoint?): Boolean {
        val eligibility = onboardingRepository.getCustomerEligibility()
            ?: onboardingRepository.checkCustomerEligibility()
        return if (entryPoint == null) {
            eligibility.any { it.isTangemPayType }
        } else {
            eligibility.any { it == entryPoint.toEligibilityType() }
        }
    }

    private fun TangemPayEntryPoint.toEligibilityType(): TangemPayEligibilityType = when (this) {
        TangemPayEntryPoint.BANNER -> TangemPayEligibilityType.BANNER
        TangemPayEntryPoint.DETAILS -> TangemPayEligibilityType.DETAILS
        TangemPayEntryPoint.DEEPLINK -> TangemPayEligibilityType.DEEPLINK
    }

    private data class UserWalletData(
        val userWallet: UserWallet,
        val isPaeraCustomer: Boolean,
    )
}