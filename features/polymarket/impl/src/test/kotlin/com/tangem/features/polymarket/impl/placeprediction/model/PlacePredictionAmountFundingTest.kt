package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.left
import arrow.core.right
import com.arkivanov.decompose.router.slot.dismiss
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.StatusSource
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.features.polymarket.impl.common.FakeSlotHost
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketFundingSheetConfig
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import io.mockk.coEvery
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlacePredictionAmountFundingTest : PlacePredictionModelTestBase() {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddFunds {

        @Test
        fun `GIVEN the requisites are ready WHEN add funds is tapped THEN the sheet opens`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            val slot = FakeSlotHost(model.fundingSheetNavigation)
            advanceUntilIdle()

            // Act
            model.onAddFundsClick()

            // Assert
            assertThat(slot.configuration).isNotNull()
        }

        @Test
        fun `GIVEN the requisites failed WHEN add funds is tapped THEN the failure is reported instead of the sheet`() =
            runTest {
                // Arrange
                coEvery { refreshRequisites(USER_WALLET_ID) } returns PolymarketBridgeError.Unavailable.left()
                val model = createAmountModel(testScope = this)
                val slot = FakeSlotHost(model.fundingSheetNavigation)
                advanceUntilIdle()

                // Act
                model.onAddFundsClick()

                // Assert
                assertThat(slot.configuration).isNull()
                verify { messageSender.send(any<DialogMessage>()) }
            }

        @Test
        fun `GIVEN the requisites are still loading WHEN add funds is tapped THEN nothing is opened or reported`() =
            runTest {
                // Arrange
                coEvery { refreshRequisites(USER_WALLET_ID) } coAnswers { awaitCancellation() }
                val model = createAmountModel(testScope = this)
                val slot = FakeSlotHost(model.fundingSheetNavigation)
                advanceUntilIdle()

                // Act
                model.onAddFundsClick()

                // Assert
                assertThat(slot.configuration).isNull()
                verify(exactly = 0) { messageSender.send(any<DialogMessage>()) }
            }

        @Test
        fun `GIVEN the sheet is open WHEN swap is chosen THEN the top-up swap opens with the deposit token locked`() =
            runTest {
                // Arrange
                val model = createAmountModel(testScope = this)
                val slot = FakeSlotHost(model.fundingSheetNavigation)
                advanceUntilIdle()
                model.onAddFundsClick()
                val route = slot<AppRoute.Swap>()
                every { router.push(capture(route), any()) } returns Unit

                // Act
                model.onSwapClick()

                // Assert
                assertThat(route.captured.fromCurrencyPosition).isEqualTo(AppRoute.Swap.CurrencyPosition.TO)
                assertThat(route.captured.accountFlow).isEqualTo(AppRoute.Swap.AccountFlow.PredictionTopUp)
                assertThat(route.captured.screenSource).isEqualTo(AnalyticsParam.ScreensSources.Predictions.value)
                assertThat(slot.configuration).isNull()
            }

        @Test
        fun `GIVEN the sheet is open WHEN receive is chosen THEN the bridge address is shown`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            val receiveSlot = FakeSlotHost(model.fundingSheetNavigation)
            advanceUntilIdle()
            model.onAddFundsClick()

            // Act
            model.onReceiveClick()

            // Assert
            val config = (receiveSlot.configuration as PolymarketFundingSheetConfig.Receive).config
            assertThat(config.receiveAddress.single().value).isEqualTo(REQUISITES_ADDRESS)
            assertThat(config.cryptoCurrency).isEqualTo(createCollateralCurrency())
        }

        @Test
        fun `GIVEN receive replaced the sheet WHEN the add funds sheet reports its exit THEN receive stays open`() =
            runTest {
                // Arrange
                val model = createAmountModel(testScope = this)
                val slot = FakeSlotHost(model.fundingSheetNavigation)
                advanceUntilIdle()
                model.onAddFundsClick()
                model.onReceiveClick()

                // Act
                model.onDismissAddFunds()

                // Assert
                assertThat(slot.configuration).isInstanceOf(PolymarketFundingSheetConfig.Receive::class.java)
            }

        @Test
        fun `GIVEN a deposit address WHEN the sheet is opened and closed THEN no top-up is ever assumed to be sent`() =
            runTest {
                // Arrange
                val model = createAmountModel(testScope = this)
                val receiveSlot = FakeSlotHost(model.fundingSheetNavigation)
                advanceUntilIdle()
                model.onAddFundsClick()

                // Act
                model.onReceiveClick()
                model.fundingSheetNavigation.dismiss()
                advanceUntilIdle()

                // Assert
                assertThat(receiveSlot.configuration).isNull()
                verify(exactly = 0) { fundingWindow.open(any()) }
            }

        @Test
        fun `GIVEN the deposit address is not confirmed WHEN add funds is tapped THEN no address is offered`() =
            runTest {
                // Arrange
                statusFlow.value = activeStatus(depositAddress = null)
                val model = createAmountModel(testScope = this)
                val slot = FakeSlotHost(model.fundingSheetNavigation)
                advanceUntilIdle()

                // Act
                model.onAddFundsClick()

                // Assert
                assertThat(slot.configuration).isNull()
                verify { messageSender.send(any<SnackbarMessage>()) }
            }

        @Test
        fun `GIVEN the region is restricted WHEN the screen opens THEN add funds is not offered`() = runTest {
            // Arrange
            coEvery { checkGeoblockUseCase() } returns true.right()

            // Act
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat((model.uiState.value.order as OrderSideUM.Buy).isAddFundsEnabled).isFalse()
        }

        @Test
        fun `GIVEN the region is still being checked WHEN the requisites are ready THEN add funds is not offered`() =
            runTest {
                // Arrange
                coEvery { checkGeoblockUseCase() } coAnswers { awaitCancellation() }

                // Act
                val model = createAmountModel(testScope = this)
                advanceUntilIdle()

                // Assert
                assertThat((model.uiState.value.order as OrderSideUM.Buy).isAddFundsEnabled).isFalse()
            }

        @Test
        fun `GIVEN the region check failed WHEN the requisites are ready THEN add funds is offered`() = runTest {
            // Arrange
            coEvery { checkGeoblockUseCase() } returns PolymarketOnboardingError.Network.left()

            // Act
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat((model.uiState.value.order as OrderSideUM.Buy).isAddFundsEnabled).isTrue()
        }

        @Test
        fun `GIVEN the requisites are ready WHEN the screen opens THEN add funds is offered`() = runTest {
            // Act
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat((model.uiState.value.order as OrderSideUM.Buy).isAddFundsEnabled).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Funding {

        @Test
        fun `GIVEN a top-up is on its way WHEN the screen is open THEN it says the funds are awaited`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Act
            isFundingOpen.value = true
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.notifications).contains(PredictionNotificationUM.AwaitingFunds)
        }

        @Test
        fun `GIVEN funds are awaited WHEN the top-up settles THEN the screen stops saying so`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            isFundingOpen.value = true
            advanceUntilIdle()

            // Act
            isFundingOpen.value = false
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.notifications).doesNotContain(PredictionNotificationUM.AwaitingFunds)
        }

        @Test
        fun `GIVEN the balance moves WHEN the account status reports it THEN the screen follows`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Act
            statusFlow.value = activeStatus(balance = BigDecimal("142"))
            advanceUntilIdle()

            // Assert
            assertThat((model.uiState.value.order as OrderSideUM.Buy).balance).isEqualTo(BigDecimal("142"))
        }

        @ParameterizedTest
        @EnumSource(value = StatusSource::class, names = ["CACHE", "ONLY_CACHE"])
        fun `GIVEN a balance not refreshed WHEN the account status reports it THEN the screen keeps it unknown`(
            source: StatusSource,
        ) = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()

            // Act
            statusFlow.value = activeStatus(balance = BigDecimal("142"), source = source)
            advanceUntilIdle()

            // Assert
            assertThat((model.uiState.value.order as OrderSideUM.Buy).balance).isNull()
        }

        @Test
        fun `GIVEN the screen is hidden WHEN it was polling THEN the shared poll is released`() = runTest {
            // Arrange
            val model = createAmountModel(testScope = this)
            advanceUntilIdle()
            model.onResume()
            runCurrent()

            // Act
            model.onPause()

            // Assert
            verify(exactly = 1) { balancePoller.subscribe(userWalletId = USER_WALLET_ID) }
        }
    }
}