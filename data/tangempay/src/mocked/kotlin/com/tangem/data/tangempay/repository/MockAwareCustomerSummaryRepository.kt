package com.tangem.data.tangempay.repository

import arrow.core.Either
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.repository.CustomerSummaryRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.spend.datasource.config.TangemPay
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import javax.inject.Singleton

// MOCK env skips the enrollment that derives the money account key; the summary is fetched before the balance.
@Singleton
internal class MockAwareCustomerSummaryRepository @Inject constructor(
    private val real: DefaultCustomerSummaryRepository,
    private val apiConfigsManager: ApiConfigsManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val moneyAccountCurrencyFactory: MoneyAccountCurrencyFactory,
    private val derivationsRepository: DerivationsRepository,
) : CustomerSummaryRepository {

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(TangemPay.Bff.ID)
            .environment == ApiEnvironment.MOCK

    override suspend fun fetch(userWalletId: UserWalletId): Either<TangemPayApiError, CustomerSummary> {
        if (isMockMode) deriveMoneyAccountKeyIfMissing(userWalletId)
        return real.fetch(userWalletId)
    }

    override fun getFromCache(userWalletId: UserWalletId): CustomerSummary? = real.getFromCache(userWalletId)

    private suspend fun deriveMoneyAccountKeyIfMissing(userWalletId: UserWalletId) {
        val userWallet = userWalletsListRepository.userWallets.value?.firstOrNull { it.walletId == userWalletId }
        when (userWallet) {
            is UserWallet.Hot -> {
                val isAccessCodeDerivationAllowed = System.getProperty(UITEST_DERIVE_WITH_ACCESS_CODE_KEY) == "1"
                val hasAccessCode = userWallet.hotWalletId.authType != HotWalletId.AuthType.NoPassword
                if (hasAccessCode && !isAccessCodeDerivationAllowed) return
            }
            is UserWallet.Cold -> if (System.getProperty(UITEST_DERIVE_FOR_COLD_WALLETS_KEY) != "1") return
            null -> return
        }

        runSuspendCatching {
            val currency = moneyAccountCurrencyFactory.create(userWalletId)
            val missing = derivationsRepository.findMissingDerivations(userWalletId, listOf(currency))
            if (missing.isNotEmpty()) {
                derivationsRepository.derivePublicKeysByNetworks(userWalletId, listOf(currency.network))
            }
        }.onFailure { TangemLogger.e("Mock money account key derivation failed for $userWalletId", it) }
    }

    companion object {
        const val UITEST_DERIVE_WITH_ACCESS_CODE_KEY = "uitest.moneyaccount.derive_with_access_code"
        const val UITEST_DERIVE_FOR_COLD_WALLETS_KEY = "uitest.moneyaccount.derive_for_cold_wallets"
    }
}