package com.tangem.features.yield.supply.impl.active.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOrigin
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOutcome
import com.tangem.domain.yield.supply.models.YieldSupplyTransactionOutcome
import com.tangem.features.yield.supply.impl.incentives.claim.result.entity.YieldSupplyClaimResultUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyActiveModelClaimOutcomeTest : YieldSupplyActiveModelTestBase() {

    @BeforeEach
    fun setUp() {
        coEvery { getYieldBoostStatusUseCase(any(), any()) } returns YieldBoostStatus.NotStarted.right()
        coEvery { yieldSupplyGetTokenStatusUseCase(any()) } returns IllegalStateException("no market").left()
    }

    @Test
    fun `GIVEN a submitted claim WHEN it is confirmed THEN the result turns into a confirmed one`() = runTest {
        // Arrange
        val model = createModel()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()
        model.onClaimSubmitted(submittedResult())

        // Act
        claimOutcomes.emit(outcome(YieldSupplyTransactionOutcome.Confirmed))
        runCurrent()

        // Assert
        assertThat(model.claimResultFlow.value?.status)
            .isInstanceOf(YieldSupplyClaimResultUM.Status.Confirmed::class.java)
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.ClaimResult)

        model.onDestroy()
    }

    @Test
    fun `GIVEN a submitted claim WHEN it reverts THEN the result sheet is closed`() = runTest {
        // Arrange
        val model = createModel()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()
        model.onClaimSubmitted(submittedResult())

        // Act
        claimOutcomes.emit(outcome(YieldSupplyTransactionOutcome.Reverted))
        runCurrent()

        // Assert
        assertThat(model.claimResultFlow.value).isNull()
        assertThat(slot.configuration).isNull()

        model.onDestroy()
    }

    @Test
    fun `GIVEN a claim of another token WHEN it is confirmed THEN this result is left alone`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        model.onClaimSubmitted(submittedResult())

        // Act
        claimOutcomes.emit(
            outcome(YieldSupplyTransactionOutcome.Confirmed).copy(
                cryptoCurrencyId = CryptoCurrency.ID(
                    prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                    body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
                    suffix = CryptoCurrency.ID.Suffix.RawID("0xother"),
                ),
            ),
        )
        runCurrent()

        // Assert
        assertThat(model.claimResultFlow.value?.status).isEqualTo(YieldSupplyClaimResultUM.Status.Submitted)

        model.onDestroy()
    }

    @Test
    fun `GIVEN another sheet open WHEN a claim reverts THEN that sheet is left alone`() = runTest {
        // Arrange
        val model = createModel()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()
        model.onStopEarning()
        advanceUntilIdle()

        // Act
        claimOutcomes.emit(outcome(YieldSupplyTransactionOutcome.Reverted))
        runCurrent()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)

        model.onDestroy()
    }

    @Test
    fun `GIVEN a sheet opened over the result WHEN the claim reverts THEN that sheet is left alone`() = runTest {
        // Arrange
        val model = createModel()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()
        model.onClaimSubmitted(submittedResult())
        model.onApprove()

        // Act
        claimOutcomes.emit(outcome(YieldSupplyTransactionOutcome.Reverted))
        runCurrent()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Approve)
        assertThat(model.claimResultFlow.value).isNull()

        model.onDestroy()
    }

    private fun submittedResult() = YieldSupplyClaimResultUM(
        status = YieldSupplyClaimResultUM.Status.Submitted,
        total = stringReference("$45.50"),
        serviceFee = stringReference("$1.40"),
        networkFee = stringReference("$1.40"),
    )

    private fun outcome(outcome: YieldSupplyTransactionOutcome) = YieldSupplyClaimOutcome(
        userWalletId = userWalletId,
        cryptoCurrencyId = CRYPTO_CURRENCY_ID,
        chainId = CHAIN_ID,
        userAddress = USER_ADDRESS,
        outcome = outcome,
        origin = YieldSupplyClaimOrigin.ClaimSheet,
    )

    private fun TestScope.createModel() = createModel(dispatchers = createTestingCoroutineDispatcherProvider())

    private companion object {
        const val CHAIN_ID = 1
        const val USER_ADDRESS = "0xowner"
    }
}