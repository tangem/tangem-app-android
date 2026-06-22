package com.tangem.features.yield.supply.impl.active.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.common.routing.AppRouter
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.domain.yield.supply.promo.usecase.GetYieldBoostStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetCurrentFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetDustMinAmountUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetMaxFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetProtocolBalanceUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyMinAmountUseCase
import com.tangem.features.yield.supply.api.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.impl.YieldBoostStoryPreloader
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
class YieldSupplyActiveModelBoostBlockTest {

    private val yieldSupplyGetProtocolBalanceUseCase: YieldSupplyGetProtocolBalanceUseCase = mockk(relaxed = true)
    private val yieldSupplyGetTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase = mockk(relaxed = true)
    private val yieldSupplyMinAmountUseCase: YieldSupplyMinAmountUseCase = mockk(relaxed = true)
    private val yieldSupplyGetCurrentFeeUseCase: YieldSupplyGetCurrentFeeUseCase = mockk(relaxed = true)
    private val yieldSupplyGetMaxFeeUseCase: YieldSupplyGetMaxFeeUseCase = mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val yieldSupplyGetDustMinAmountUseCase: YieldSupplyGetDustMinAmountUseCase = mockk(relaxed = true)
    private val getYieldBoostStatusUseCase: GetYieldBoostStatusUseCase = mockk(relaxed = true)
    private val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles = mockk(relaxed = true)
    private val boostStoryPreloader: YieldBoostStoryPreloader = mockk(relaxed = true)
    private val analyticsHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")
    private val userWallet: UserWallet.Hot = mockk(relaxed = true)

    private val network: Network = mockk(relaxed = true) {
        every { rawId } returns NETWORK_ID
        every { name } returns "Ethereum"
    }
    private val token: CryptoCurrency.Token = mockk(relaxed = true) {
        every { contractAddress } returns CONTRACT_ADDRESS
        every { network } returns this@YieldSupplyActiveModelBoostBlockTest.network
        every { symbol } returns "USDT"
        every { decimals } returns 6
    }

    private val enrolledStatus = YieldBoostStatus.Enrolled(
        tokenName = "USDT",
        networkId = NETWORK_ID,
        moduleAddress = "0xModule",
        userAddress = "0xUser",
        contractAddress = CONTRACT_ADDRESS,
        qualificationEndDate = Clock.System.now() + 100.days,
    )

    @BeforeEach
    fun setUp() {
        every { yieldSupplyFeatureToggles.isYieldPromoEnabled } returns true
        every { getUserWalletUseCase.invoke(userWalletId) } returns userWallet.right()
        every { singleAccountStatusListSupplier.invoke(userWalletId) } returns emptyFlow()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        coEvery { yieldSupplyGetProtocolBalanceUseCase(any(), any()) } returns BigDecimal.ONE.right()
    }

    private fun createModel(): YieldSupplyActiveModel = YieldSupplyActiveModel(
        paramsContainer = MutableParamsContainer(
            YieldSupplyActiveComponent.Params(userWalletId = userWalletId, cryptoCurrency = token),
        ),
        dispatchers = TestingCoroutineDispatcherProvider(),
        analyticsHandler = analyticsHandler,
        yieldSupplyGetProtocolBalanceUseCase = yieldSupplyGetProtocolBalanceUseCase,
        yieldSupplyGetTokenStatusUseCase = yieldSupplyGetTokenStatusUseCase,
        yieldSupplyMinAmountUseCase = yieldSupplyMinAmountUseCase,
        yieldSupplyGetCurrentFeeUseCase = yieldSupplyGetCurrentFeeUseCase,
        yieldSupplyGetMaxFeeUseCase = yieldSupplyGetMaxFeeUseCase,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
        urlOpener = urlOpener,
        appRouter = appRouter,
        yieldSupplyGetDustMinAmountUseCase = yieldSupplyGetDustMinAmountUseCase,
        getYieldBoostStatusUseCase = getYieldBoostStatusUseCase,
        yieldSupplyFeatureToggles = yieldSupplyFeatureToggles,
        boostStoryPreloader = boostStoryPreloader,
    )

    @Test
    fun `GIVEN cached status NotStarted WHEN model created THEN force refreshes and shows boost text`() = runTest {
        coEvery { getYieldBoostStatusUseCase(userWalletId, false) } returns YieldBoostStatus.NotStarted.right()
        coEvery { getYieldBoostStatusUseCase(userWalletId, true) } returns enrolledStatus.right()

        val model = createModel()

        assertThat(model.uiState.value.boostText).isNotNull()
        coVerify(exactly = 1) { getYieldBoostStatusUseCase(userWalletId, false) }
        coVerify(exactly = 1) { getYieldBoostStatusUseCase(userWalletId, true) }
    }

    @Test
    fun `GIVEN cached status already Enrolled WHEN model created THEN does not force refresh`() = runTest {
        coEvery { getYieldBoostStatusUseCase(userWalletId, false) } returns enrolledStatus.right()

        val model = createModel()

        assertThat(model.uiState.value.boostText).isNotNull()
        coVerify(exactly = 1) { getYieldBoostStatusUseCase(userWalletId, false) }
        coVerify(exactly = 0) { getYieldBoostStatusUseCase(userWalletId, true) }
    }

    @Test
    fun `GIVEN status NotStarted even after refresh WHEN model created THEN shows no boost text`() = runTest {
        coEvery { getYieldBoostStatusUseCase(userWalletId, false) } returns YieldBoostStatus.NotStarted.right()
        coEvery { getYieldBoostStatusUseCase(userWalletId, true) } returns YieldBoostStatus.NotStarted.right()

        val model = createModel()

        assertThat(model.uiState.value.boostText).isNull()
        coVerify(exactly = 1) { getYieldBoostStatusUseCase(userWalletId, false) }
        coVerify(exactly = 1) { getYieldBoostStatusUseCase(userWalletId, true) }
    }

    @Test
    fun `GIVEN promo toggle disabled WHEN model created THEN does not query boost status`() = runTest {
        every { yieldSupplyFeatureToggles.isYieldPromoEnabled } returns false

        val model = createModel()

        assertThat(model.uiState.value.boostText).isNull()
        coVerify(exactly = 0) { getYieldBoostStatusUseCase(any(), any()) }
    }

    private companion object {
        const val CONTRACT_ADDRESS = "0xCONTRACT"
        const val NETWORK_ID = "ethereum"
    }
}