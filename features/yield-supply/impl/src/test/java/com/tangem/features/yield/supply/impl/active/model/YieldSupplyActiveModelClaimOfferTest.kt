package com.tangem.features.yield.supply.impl.active.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/** Which sheet a tap on "Stop earning" opens while the module still holds claimable rewards. */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyActiveModelClaimOfferTest : YieldSupplyActiveModelTestBase() {

    @BeforeEach
    fun setUp() {
        coEvery { getYieldBoostStatusUseCase(any(), any()) } returns YieldBoostStatus.NotStarted.right()
        every { claimTracker.isClaimInFlight(any(), any()) } returns flowOf(false)
        coEvery { yieldSupplyGetTokenStatusUseCase(any()) } returns MockYieldSupply.market().right()
        coEvery { getYieldRewardsSummaryUseCase(any(), any()) } returns MockYieldSupply.claimableSummary().right()
    }

    @Test
    fun `GIVEN claimable rewards WHEN stop earning is tapped THEN the claim sheet is offered`() = runTest {
        // Arrange
        val model = createModelWithAddress()
        val slot = FakeSlotHost(model.slotNavigation)
        advanceUntilIdle()

        // Act
        model.onStopEarning()
        advanceUntilIdle()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Claim)
        assertThat(model.wasClaimOfferedBeforeWithdrawal).isTrue()

        model.onDestroy()
    }

    @Test
    fun `GIVEN the claim was already offered WHEN stop earning is tapped again THEN the withdrawal opens`() =
        runTest {
            // Arrange
            val model = createModelWithAddress()
            val slot = FakeSlotHost(model.slotNavigation)
            advanceUntilIdle()
            model.onStopEarning()
            advanceUntilIdle()

            // Act
            model.onStopEarning()
            advanceUntilIdle()

            // Assert
            assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)

            model.onDestroy()
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN nothing to offer WHEN stop earning is tapped THEN the withdrawal opens`(model: NoOfferModel) = runTest {
        // Arrange
        model.arrange(this@YieldSupplyActiveModelClaimOfferTest)
        val activeModel = createModelWithAddress()
        val slot = FakeSlotHost(activeModel.slotNavigation)
        advanceUntilIdle()

        // Act
        activeModel.onStopEarning()
        advanceUntilIdle()

        // Assert
        assertThat(slot.configuration).isEqualTo(YieldSupplyActiveRoute.Exit)
        assertThat(activeModel.wasClaimOfferedBeforeWithdrawal).isFalse()

        activeModel.onDestroy()
    }

    private fun provideTestModels() = listOf(
        NoOfferModel(reason = "the backend defers the claim") {
            coEvery { getYieldRewardsSummaryUseCase(any(), any()) } returns MockYieldSupply.deferredSummary().right()
        },
        NoOfferModel(reason = "a claim is already in flight") {
            every { claimTracker.isClaimInFlight(any(), any()) } returns flowOf(true)
        },
        NoOfferModel(reason = "the market is unknown") {
            coEvery { yieldSupplyGetTokenStatusUseCase(any()) } returns IllegalStateException("no market").left()
        },
    )

    internal data class NoOfferModel(
        val reason: String,
        val arrange: YieldSupplyActiveModelClaimOfferTest.() -> Unit,
    ) {
        override fun toString(): String = reason
    }

    /** The address the offer is keyed by arrives with the balance, so the status has to carry one. */
    private fun TestScope.createModelWithAddress(): YieldSupplyActiveModel =
        createModel(dispatchers = createTestingCoroutineDispatcherProvider()).also { model ->
            model.cryptoCurrencyStatusFlow.value = mockk(relaxed = true) {
                every { currency } returns token
                every { value.networkAddress?.defaultAddress?.value } returns USER_ADDRESS
            }
        }

    private companion object {
        const val USER_ADDRESS = "0xowner"
    }
}