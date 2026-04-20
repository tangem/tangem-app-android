package com.tangem.features.staking.impl.presentation.model

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.transformers.amount.*
import com.tangem.features.staking.impl.presentation.state.transformers.notifications.DismissStakingNotificationsStateTransformer
import com.tangem.utils.transformer.Transformer
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class StakingModelAmountTest : StakingModelTestBase() {

    @Test
    fun `WHEN onAmountPasteTriggerDismiss THEN AmountPasteDismissStateTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAmountPasteTriggerDismiss()

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is AmountPasteDismissStateTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onMaxValueClick THEN analytics sent and AmountMaxValueStateTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onMaxValueClick()

        verify {
            analyticsEventHandler.send(
                match {
                    it is StakingAnalyticsEvent.ButtonMax
                }
            )
        }
        verify {
            stateController.update(
                match<Transformer<StakingUiState>> { it is AmountMaxValueStateTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onCurrencyChangeClick THEN analytics sent and AmountCurrencyChangeStateTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onCurrencyChangeClick(isFiat = true)

        verify {
            analyticsEventHandler.send(match { it is StakingAnalyticsEvent.AmountSelectCurrency })
        }
        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is AmountCurrencyChangeStateTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onAmountReduceByClick THEN AmountReduceByStateTransformer and DismissNotification applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAmountReduceByClick(
            reduceAmountBy = BigDecimal.ONE,
            reduceAmountByDiff = BigDecimal.TEN,
            notification = NotificationUM::class.java,
        )

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is AmountReduceByStateTransformer }
            )
        }
        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is DismissStakingNotificationsStateTransformer }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onAmountReduceToClick THEN AmountReduceToStateTransformer and DismissNotification applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAmountReduceToClick(
            reduceAmountTo = BigDecimal.ONE,
            notification = NotificationUM::class.java,
        )

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is AmountReduceToStateTransformer }
            )
        }
        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> {
                    it is DismissStakingNotificationsStateTransformer
                }
            )
        }

        model.onDestroy()
    }

    @Test
    fun `WHEN onAmountReduceByFeeClick THEN AmountReduceByStateTransformer and DismissNotification applied`() =
        runTest {
            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onAmountReduceByFeeClick(
                reduceAmount = BigDecimal.ONE,
                notification = NotificationUM::class.java,
            )

            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> { it is AmountReduceByStateTransformer }
                )
            }
            verify {
                stateController.update(
                    transformer = match<Transformer<StakingUiState>> {
                        it is DismissStakingNotificationsStateTransformer
                    }
                )
            }

            model.onDestroy()
        }

    @Test
    fun `WHEN onNotificationCancel THEN DismissStakingNotificationsStateTransformer applied`() = runTest {
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onNotificationCancel(NotificationUM::class.java)

        verify {
            stateController.update(
                transformer = match<Transformer<StakingUiState>> { it is DismissStakingNotificationsStateTransformer }
            )
        }

        model.onDestroy()
    }
}