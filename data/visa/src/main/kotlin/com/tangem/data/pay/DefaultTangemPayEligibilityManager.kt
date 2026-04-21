package com.tangem.data.pay

import com.tangem.common.card.FirmwareVersion
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.TangemPayEligibilityType
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.TangemPayEntryPoint
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.hot.sdk.model.HotWalletId
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
        return getUserWalletsData(entryPoint = entryPoint).mapNotNull {
            if (!it.isPaeraCustomer || !shouldExcludePaeraCustomers) it.userWallet else null
        }
    }

    override suspend fun getPossibleWalletsIds(shouldExcludePaeraCustomers: Boolean): List<UserWalletId> {
        return getPossibleWalletsForTangemPay(entryPoint = null).addPaeraCustomersData().mapNotNull {
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
        return getUserWalletsData(entryPoint = entryPoint).any { it.isPaeraCustomer }
    }

    private suspend fun getUserWalletsData(entryPoint: TangemPayEntryPoint?): List<UserWalletData> {
        cachedEligibleWallets[entryPoint]?.let { return it }

        return loadMutex.withLock {
            cachedEligibleWallets[entryPoint]?.let { return it }
            eligibleWalletsDeferred?.let { return it.await() }

            coroutineScope {
                val deferred = async {
                    getPossibleWalletsForTangemPay(entryPoint = entryPoint)
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

    private suspend fun getPossibleWalletsForTangemPay(entryPoint: TangemPayEntryPoint?): List<UserWallet> {
        val wallets = userWalletsListRepository.userWallets.value ?: return emptyList()

        val candidates = wallets.filter { wallet ->
            wallet.isMultiCurrency && !wallet.isLocked && wallet.isCompatible() &&
                !onboardingRepository.isTangemPayDeactivated(wallet.walletId)
        }

        if (candidates.isEmpty()) return emptyList()

        if (!checkTangemPayEligibility(entryPoint = entryPoint)) {
            return emptyList()
        }

        return candidates
    }

    private fun UserWallet.isCompatible(): Boolean = when (this) {
        is UserWallet.Cold -> scanResponse.card.firmwareVersion >= FirmwareVersion.HDWalletAvailable
        is UserWallet.Hot -> hotWalletId.authType != HotWalletId.AuthType.NoPassword
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

    private suspend fun checkTangemPayEligibility(entryPoint: TangemPayEntryPoint?): Boolean {
        val eligibility = onboardingRepository.getCustomerEligibility().ifEmpty {
            onboardingRepository.checkCustomerEligibility()
        }
        return if (entryPoint == null) {
            eligibility.isNotEmpty()
        } else {
            eligibility.any { it == entryPoint.toEligibilityType() }
        }
    }

    private fun TangemPayEntryPoint.toEligibilityType(): TangemPayEligibilityType = when (this) {
        TangemPayEntryPoint.BANNER -> TangemPayEligibilityType.BANNER
        TangemPayEntryPoint.DETAILS -> TangemPayEligibilityType.DETAILS
    }

    private data class UserWalletData(
        val userWallet: UserWallet,
        val isPaeraCustomer: Boolean,
    )
}