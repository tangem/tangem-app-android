package com.tangem.features.staking.impl.presentation.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.test.core.ProvideTestModels
import com.tangem.features.staking.impl.presentation.state.transformers.amount.*
import com.tangem.features.staking.impl.presentation.state.transformers.notifications.DismissStakingNotificationsStateTransformer
import com.tangem.utils.transformer.Transformer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelAmountTest : StakingModelTestBase() {

    @Test
    fun `GIVEN loaded model WHEN onAmountPasteTriggerDismiss THEN AmountPasteDismissStateTransformer applied`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onAmountPasteTriggerDismiss()

            // Assert
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is AmountPasteDismissStateTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN loaded model WHEN onMaxValueClick THEN analytics sent and AmountMaxValueStateTransformer applied`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onMaxValueClick()

            // Assert
            verify {
                analyticsEventHandler.send(
                    match {
                        it is StakingAnalyticsEvent.ButtonMax
                    },
                )
            }
            verify {
                stateController.update(
                    match<Transformer<StakingUiState>> { it is AmountMaxValueStateTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN loaded model WHEN onCurrencyChangeClick THEN analytics sent and AmountCurrencyChangeStateTransformer applied`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onCurrencyChangeClick(isFiat = true)

            // Assert
            verify {
                analyticsEventHandler.send(match { it is StakingAnalyticsEvent.AmountSelectCurrency })
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is AmountCurrencyChangeStateTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN loaded model WHEN onAmountReduceByClick THEN AmountReduceByStateTransformer and DismissNotification applied`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onAmountReduceByClick(
                reduceAmountBy = BigDecimal.ONE,
                reduceAmountByDiff = BigDecimal.TEN,
                notification = NotificationUM::class.java,
            )

            // Assert
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is AmountReduceByStateTransformer },
                )
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is DismissStakingNotificationsStateTransformer },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN loaded model WHEN onAmountReduceToClick THEN AmountReduceToStateTransformer and DismissNotification applied`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onAmountReduceToClick(
                reduceAmountTo = BigDecimal.ONE,
                notification = NotificationUM::class.java,
            )

            // Assert
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is AmountReduceToStateTransformer },
                )
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> {
                        it is DismissStakingNotificationsStateTransformer
                    },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN loaded model WHEN onAmountReduceByFeeClick THEN AmountReduceByStateTransformer and DismissNotification applied`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onAmountReduceByFeeClick(
                reduceAmount = BigDecimal.ONE,
                notification = NotificationUM::class.java,
            )

            // Assert
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is AmountReduceByStateTransformer },
                )
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> {
                        it is DismissStakingNotificationsStateTransformer
                    },
                )
            }

            model.onDestroy()
        }

    @Test
    fun `GIVEN loaded model WHEN onNotificationCancel THEN DismissStakingNotificationsStateTransformer applied`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.onNotificationCancel(NotificationUM::class.java)

            // Assert
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is DismissStakingNotificationsStateTransformer },
                )
            }

            model.onDestroy()
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN minimum transaction amount WHEN smaller amount entered THEN amount field error follows minimum`(
        model: MinimumAmountModel,
    ) = runTest {
        // Arrange
        val status = currencyStatus(amount = BigDecimal.TEN)
        val accountStatus = accountCurrencyStatusOf(status)
        every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns flowOf(accountStatus)
        coEvery { getMinimumTransactionAmountSyncUseCase(testUserWalletId, status) } returns
            Either.Right(model.minimum)
        val stakingModel = createModel(testScope = this)
        advanceUntilIdle()
        val transformers = mutableListOf<Transformer<StakingUiState>>()
        every { stateController.update(capture(transformers)) } just Runs

        // Act
        stakingModel.onAmountValueChange("1")

        // Assert
        val amountState = transformers.filterIsInstance<AmountChangeStateTransformer>().single()
            .transform(stakingUiState(amountState = amountData())).amountState as AmountState.Data
        assertThat(amountState.amountTextField.isError).isEqualTo(model.isError)

        stakingModel.onDestroy()
    }

    data class MinimumAmountModel(val minimum: BigDecimal?, val isError: Boolean)

    companion object {
        @JvmStatic
        fun provideTestModels() = listOf(
            MinimumAmountModel(minimum = BigDecimal("5"), isError = true),
            MinimumAmountModel(minimum = null, isError = false),
        )
    }
}