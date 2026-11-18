package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.hotwallet.CheckHotWalletBalanceThresholdUseCase
import com.tangem.domain.hotwallet.IsHotWalletBalanceThresholdReachedUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetPermanentUpgradeBannerTransformer
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PermanentUpgradeBannerSubscriberTest {

    private val userWallet: UserWallet.Hot = mockk { every { walletId } returns USER_WALLET_ID }
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val isThresholdReachedUseCase: IsHotWalletBalanceThresholdReachedUseCase = mockk()
    private val checkThresholdUseCase: CheckHotWalletBalanceThresholdUseCase = mockk()
    private val stateController: WalletStateController = mockk(relaxUnitFun = true)
    private val clickIntents: WalletClickIntents = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)

    private val isThresholdReached = MutableStateFlow(false)
    private val selectedWalletId = MutableStateFlow(USER_WALLET_ID)
    private val accountStatusList: AccountStatusList = mockk {
        every { totalFiatBalance } returns TOTAL_FIAT_BALANCE
        every { flattenCurrencies() } returns emptyList()
    }

    private val screenLifecycleProvider = ScreenLifecycleProvider()

    @BeforeEach
    fun setUp() {
        clearMocks(
            singleAccountStatusListSupplier,
            isThresholdReachedUseCase,
            checkThresholdUseCase,
            stateController,
            clickIntents,
            analyticsEventHandler,
        )
        isThresholdReached.value = false
        selectedWalletId.value = USER_WALLET_ID
        screenLifecycleProvider.onResume()

        every { isThresholdReachedUseCase(USER_WALLET_ID) } returns isThresholdReached
        every {
            singleAccountStatusListSupplier(SingleAccountStatusListProducer.Params(USER_WALLET_ID))
        } returns flowOf(accountStatusList)
        coEvery { checkThresholdUseCase(USER_WALLET_ID, TOTAL_FIAT_BALANCE, emptyList()) } returns false.right()
        every { stateController.selectedWalletIdFlow } returns selectedWalletId
    }

    @Test
    fun `GIVEN threshold not reached WHEN balance loaded THEN the balance is checked and no banner shown`() = runTest {
        // Act
        collectSubscriber()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { checkThresholdUseCase(USER_WALLET_ID, TOTAL_FIAT_BALANCE, emptyList()) }
        assertThat(lastBannerTransformer().permanentUpgradeBannerUM).isNull()
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN threshold reached WHEN subscribed THEN the banner is shown and the balance is not checked`() = runTest {
        // Arrange
        isThresholdReached.value = true

        // Act
        collectSubscriber()
        advanceUntilIdle()

        // Assert
        assertThat(lastBannerTransformer().permanentUpgradeBannerUM).isNotNull()
        coVerify(exactly = 0) { checkThresholdUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN banner shown WHEN clicked THEN the upgrade opens from the permanent banner`() = runTest {
        // Arrange
        isThresholdReached.value = true
        collectSubscriber()
        advanceUntilIdle()

        // Act
        lastBannerTransformer().permanentUpgradeBannerUM?.onClick?.invoke()

        // Assert
        verify(exactly = 1) { clickIntents.onPermanentUpgradeBannerClick(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN threshold reached WHEN wallet page shown twice THEN the notice is sent each time`() = runTest {
        // Arrange
        collectSubscriber()
        advanceUntilIdle()

        // Act
        isThresholdReached.value = true
        advanceUntilIdle()
        selectedWalletId.value = OTHER_WALLET_ID
        advanceUntilIdle()
        selectedWalletId.value = USER_WALLET_ID
        advanceUntilIdle()

        // Assert
        verify(exactly = 2) { analyticsEventHandler.send(MainScreen.NoticePermanentUpgradeBanner(USER_WALLET_ID)) }
    }

    @Test
    fun `GIVEN banner on another wallet page WHEN subscribed THEN the notice is not sent`() = runTest {
        // Arrange
        isThresholdReached.value = true
        selectedWalletId.value = OTHER_WALLET_ID

        // Act
        collectSubscriber()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN screen in background WHEN banner appears THEN the notice waits for the screen`() = runTest {
        // Arrange
        val sentEvents = mutableListOf<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(sentEvents)) } just Runs
        isThresholdReached.value = true
        screenLifecycleProvider.onPause()
        collectSubscriber()
        advanceUntilIdle()
        val sentInBackground = sentEvents.toList()

        // Act
        screenLifecycleProvider.onResume()
        advanceUntilIdle()

        // Assert
        assertThat(sentInBackground).isEmpty()
        assertThat(sentEvents).containsExactly(MainScreen.NoticePermanentUpgradeBanner(USER_WALLET_ID))
    }

    private fun lastBannerTransformer(): SetPermanentUpgradeBannerTransformer {
        val transformer = slot<SetPermanentUpgradeBannerTransformer>()
        verify(atLeast = 1) { stateController.update(capture(transformer)) }
        return transformer.captured
    }

    private fun TestScope.collectSubscriber() {
        PermanentUpgradeBannerSubscriber(
            userWallet = userWallet,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            isHotWalletBalanceThresholdReachedUseCase = isThresholdReachedUseCase,
            checkHotWalletBalanceThresholdUseCase = checkThresholdUseCase,
            stateController = stateController,
            clickIntents = clickIntents,
            screenLifecycleProvider = screenLifecycleProvider,
            analyticsEventHandler = analyticsEventHandler,
        ).subscribe(
            coroutineScope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
            dispatchers = UnconfinedTestDispatcher(testScheduler),
        )
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("022")
        val TOTAL_FIAT_BALANCE = TotalFiatBalance.Loaded(amount = BigDecimal("100"), source = StatusSource.ACTUAL)
    }
}