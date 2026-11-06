package com.tangem.features.yield.supply.impl.subcomponents.notifications.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.subcomponents.YieldSupplyActionModelTestBase
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsComponent
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsUpdateListener
import com.tangem.features.yield.supply.impl.subcomponents.notifications.entity.YieldSupplyNotificationData
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyNotificationsModelTest : YieldSupplyActionModelTestBase() {

    private val router: Router = mockk(relaxed = true)
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase = mockk()
    private val updateListener: YieldSupplyNotificationsUpdateListener = mockk(relaxed = true)
    private val updates = MutableSharedFlow<YieldSupplyNotificationData>()

    private val usdc: CryptoCurrency.Token = token().copy(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("usd-coin"),
        ),
        name = "USD Coin",
        symbol = "USDC",
        contractAddress = "0xUSDC",
    )

    @BeforeEach
    fun setUp() {
        clearMocks(router, getBalanceNotEnoughForFeeWarningUseCase, updateListener)
        every { updateListener.updateTriggerFlow } returns updates
        coEvery { getBalanceNotEnoughForFeeWarningUseCase(any(), any(), any(), any()) } returns null.right()
    }

    @Test
    fun `GIVEN picked fee token is short WHEN not enough funds THEN cannot cover banner and analytics sent`() =
        runTest {
            // Arrange
            val model = createModel(feeStatus = statusOf(usdc))

            // Act
            updates.emit(
                YieldSupplyNotificationData(feeValue = null, feeError = GetFeeError.GaslessError.NotEnoughFunds),
            )
            advanceUntilIdle()

            // Assert
            val notification = model.uiState.value.single() as NotificationUM.Error.TokenExceedsBalance
            assertThat(notification.copy(onClick = null)).isEqualTo(
                NotificationUM.Error.TokenExceedsBalance(
                    networkIconId = usdc.networkIconResId,
                    currencyName = "TEST_TOKEN",
                    feeName = "USD Coin",
                    feeSymbol = "USDC",
                    networkName = "Ethereum",
                ),
            )
            verify(exactly = 1) {
                analytics.send(
                    YieldSupplyAnalytics.NoticeNotEnoughFee(
                        token = "TTK",
                        blockchain = "Ethereum",
                        balance = AnalyticsParam.TokenBalanceState.Full,
                    ),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN picked fee token is empty WHEN not enough funds THEN a single cannot cover banner`() = runTest {
        // Arrange
        coEvery { getBalanceNotEnoughForFeeWarningUseCase(any(), any(), any(), any()) } returns
            CryptoCurrencyWarning.BalanceNotEnoughForFee(tokenCurrency = token, coinCurrency = usdc).right()
        val model = createModel(feeStatus = statusOf(usdc))

        // Act
        updates.emit(YieldSupplyNotificationData(feeValue = null, feeError = GetFeeError.GaslessError.NotEnoughFunds))
        advanceUntilIdle()

        // Assert
        val notification = model.uiState.value.single() as NotificationUM.Error.TokenExceedsBalance
        assertThat(notification.copy(onClick = null)).isEqualTo(
            NotificationUM.Error.TokenExceedsBalance(
                networkIconId = usdc.networkIconResId,
                currencyName = "TEST_TOKEN",
                feeName = "USD Coin",
                feeSymbol = "USDC",
                networkName = "USD Coin",
            ),
        )
        verify(exactly = 1) { analytics.send(ofType<YieldSupplyAnalytics.NoticeNotEnoughFee>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN fee unreachable WHEN unknown error THEN no cannot cover banner nor analytics`() = runTest {
        // Arrange
        val model = createModel(feeStatus = statusOf(usdc))

        // Act
        updates.emit(YieldSupplyNotificationData(feeValue = null, feeError = GetFeeError.UnknownError))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.none { it is NotificationUM.Error.TokenExceedsBalance }).isTrue()
        verify(exactly = 0) { analytics.send(ofType<YieldSupplyAnalytics.NoticeNotEnoughFee>()) }
        model.onDestroy()
    }

    private fun TestScope.createModel(feeStatus: CryptoCurrencyStatus): YieldSupplyNotificationsModel {
        val model = YieldSupplyNotificationsModel(
            paramsContainer = MutableParamsContainer(
                YieldSupplyNotificationsComponent.Params(
                    userWalletId = userWalletId,
                    cryptoCurrencyStatusFlow = cryptoCurrencyStatusFlow,
                    feeCryptoCurrencyStatusFlow = MutableStateFlow(feeStatus),
                    callback = mockk(relaxed = true),
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            analyticsEventHandler = analytics,
            router = router,
            getBalanceNotEnoughForFeeWarningUseCase = getBalanceNotEnoughForFeeWarningUseCase,
            yieldSupplyNotificationsUpdateListener = updateListener,
        )
        advanceUntilIdle()
        return model
    }
}