package com.tangem.features.yield.supply.impl.active.model

import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOutcome
import com.tangem.domain.yield.supply.promo.usecase.GetYieldBoostStatusUseCase
import com.tangem.domain.yield.supply.usecase.GetYieldRewardsSummaryUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetCurrentFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetDustMinAmountUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetMaxFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetProtocolBalanceUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.features.yield.supply.api.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.YieldBoostStoryPreloader
import com.tangem.features.yield.supply.impl.incentives.claim.model.YieldSupplyClaimPreparer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.BeforeEach
import java.math.BigDecimal

internal abstract class YieldSupplyActiveModelTestBase {

    protected val yieldSupplyGetProtocolBalanceUseCase: YieldSupplyGetProtocolBalanceUseCase = mockk(relaxed = true)
    protected val yieldSupplyGetTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase = mockk(relaxed = true)
    protected val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    protected val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    protected val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk(relaxed = true)
    protected val router: Router = mockk(relaxed = true)
    protected val getYieldBoostStatusUseCase: GetYieldBoostStatusUseCase = mockk(relaxed = true)
    protected val getYieldRewardsSummaryUseCase: GetYieldRewardsSummaryUseCase = mockk(relaxed = true)
    protected val claimTracker: YieldSupplyClaimTracker = mockk()
    protected val claimOutcomes = MutableSharedFlow<YieldSupplyClaimOutcome>(extraBufferCapacity = 1)

    protected val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")
    protected val userWallet: UserWallet.Hot = mockk(relaxed = true)
    protected val network: Network = mockk(relaxed = true) {
        every { rawId } returns NETWORK_ID
        every { name } returns "Ethereum"
    }
    protected val token: CryptoCurrency.Token = mockk(relaxed = true) {
        every { id } returns CRYPTO_CURRENCY_ID
        every { contractAddress } returns CONTRACT_ADDRESS
        every { network } returns this@YieldSupplyActiveModelTestBase.network
        every { symbol } returns "USDT"
        every { decimals } returns 6
    }

    @BeforeEach
    fun setUpActiveModel() {
        every { getUserWalletUseCase.invoke(userWalletId) } returns userWallet.right()
        every { singleAccountStatusListSupplier.invoke(userWalletId) } returns emptyFlow()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        coEvery { yieldSupplyGetProtocolBalanceUseCase(any(), any()) } returns BigDecimal.ONE.right()
        every { claimTracker.outcomes } returns claimOutcomes
    }

    protected fun createModel(dispatchers: CoroutineDispatcherProvider): YieldSupplyActiveModel = YieldSupplyActiveModel(
        paramsContainer = MutableParamsContainer(
            YieldSupplyActiveComponent.Params(userWalletId = userWalletId, cryptoCurrency = token),
        ),
        dispatchers = dispatchers,
        analyticsHandler = mockk<AnalyticsEventHandler>(relaxed = true),
        yieldSupplyGetProtocolBalanceUseCase = yieldSupplyGetProtocolBalanceUseCase,
        yieldSupplyGetTokenStatusUseCase = yieldSupplyGetTokenStatusUseCase,
        yieldSupplyClaimPreparer = YieldSupplyClaimPreparer(
            getYieldRewardsSummaryUseCase = getYieldRewardsSummaryUseCase,
            getFreshYieldClaimCalldataUseCase = mockk(relaxed = true),
            validateYieldClaimCalldataUseCase = mockk(relaxed = true),
        ),
        yieldSupplyMinAmountUseCase = mockk<YieldSupplyMinAmountUseCase>(relaxed = true),
        yieldSupplyGetCurrentFeeUseCase = mockk<YieldSupplyGetCurrentFeeUseCase>(relaxed = true),
        yieldSupplyGetMaxFeeUseCase = mockk<YieldSupplyGetMaxFeeUseCase>(relaxed = true),
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        urlOpener = mockk<UrlOpener>(relaxed = true),
        router = router,
        yieldSupplyGetDustMinAmountUseCase = mockk<YieldSupplyGetDustMinAmountUseCase>(relaxed = true),
        getYieldBoostStatusUseCase = getYieldBoostStatusUseCase,
        boostStoryPreloader = mockk<YieldBoostStoryPreloader>(relaxed = true),
        yieldSupplyClaimTracker = claimTracker,
    )

    protected class FakeSlotHost(navigation: SlotNavigation<YieldSupplyActiveRoute>) {

        var configuration: YieldSupplyActiveRoute? = null
            private set

        init {
            navigation.subscribe { event ->
                val old = configuration
                configuration = event.transformer(old)
                event.onComplete(configuration, old)
            }
        }
    }

    protected companion object {
        const val CONTRACT_ADDRESS = "0xCONTRACT"
        const val NETWORK_ID = "ethereum"
        val CRYPTO_CURRENCY_ID = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(NETWORK_ID),
            suffix = CryptoCurrency.ID.Suffix.RawID("0xusdt"),
        )
    }
}